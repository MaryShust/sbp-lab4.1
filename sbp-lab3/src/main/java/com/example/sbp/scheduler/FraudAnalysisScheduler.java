package com.example.sbp.scheduler;

import com.example.sbp.entity.PreSuspicionEntity;
import com.example.sbp.entity.SuspicionEntity;
import com.example.sbp.kafka.dto.RiskLevel;
import com.example.sbp.repository.BankAccountRepository;
import com.example.sbp.repository.PreSuspicionRepository;
import com.example.sbp.repository.SuspicionRepository;
import com.example.sbp.service.Bitrix24Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisScheduler {

    private final PreSuspicionRepository preSuspicionRepository;
    private final SuspicionRepository suspicionRepository;
    private final BankAccountRepository accountRepository;
    private final Bitrix24Service bitrix24Service;

    @Scheduled(fixedRate = 120000)
    public void analyzeSuspiciousTransactions() {
        log.info("Начало анализа мошенничества (каждые 2 минуты)");


        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusMinutes(4);

        List<PreSuspicionEntity> events = preSuspicionRepository.findByEventTimeBetween(startTime, now);

        List<PreSuspicionEntity> criticalHighEvents = events.stream()
                .filter(e -> e.getRiskLevel() == RiskLevel.CRITICAL
                        || e.getRiskLevel() == RiskLevel.HIGH)
                .toList();

        Map<String, List<PreSuspicionEntity>> groupedByBankAndAccount = criticalHighEvents.stream()
                .collect(Collectors.groupingBy(e -> e.getReceiverBankBic() + ":" + e.getReceiverAccountId()));

        int duplicatesFound = 0;

        for (Map.Entry<String, List<PreSuspicionEntity>> entry : groupedByBankAndAccount.entrySet()) {
            List<PreSuspicionEntity> group = entry.getValue();

            if (group.size() >= 2) {
                saveSuspicionRecord(group, now);
                duplicatesFound++;
            }
        }

        log.info("Анализ мошенничества завершен: обнаружено {} дропперов", duplicatesFound);
    }

    private void saveSuspicionRecord(List<PreSuspicionEntity> group, LocalDateTime analysisDate) {
        PreSuspicionEntity first = group.get(0);
        Long accountId = first.getReceiverAccountId();
        String bankBic = first.getReceiverBankBic();
        int duplicateCount = group.size();
        LocalDateTime since = analysisDate.minusMinutes(4);
        RiskLevel maxRiskLevel = group.stream()
                .map(PreSuspicionEntity::getRiskLevel)
                .max(Comparator.comparingInt(r -> r.ordinal()))
                .orElse(RiskLevel.HIGH);

        accountRepository.findById(accountId).ifPresent(account -> {
            String userName = account.getOwnerName();

            Optional<SuspicionEntity> existing = suspicionRepository.findRecentByUserAndAccountAndBank(
                    userName, accountId, bankBic, since);

            if (existing.isPresent()) {
                SuspicionEntity entity = existing.get();
                if (duplicateCount > entity.getDuplicateCount()) {
                    suspicionRepository.updateDuplicateCount(entity.getId(), duplicateCount);
                    log.info("Обновлен счётчик для дроппера {}: {} -> {}", userName, entity.getDuplicateCount(), duplicateCount);
                }
            } else {
                String senderNames = group.stream()
                        .map(e -> accountRepository.findById(e.getSenderAccountId()).map(a -> a.getOwnerName()).orElse("Неизвестный"))
                        .distinct()
                        .collect(Collectors.joining(", "));

                String senderBillIds = group.stream()
                        .map(e -> e.getSenderBillId() != null ? e.getSenderBillId().toString() : "N/A")
                        .distinct()
                        .collect(Collectors.joining(", "));

                Long senderAccountId = first.getSenderAccountId();
                String senderBankBic = first.getSenderBankBic();

                String totalAmount = group.stream()
                        .filter(e -> e.getAmount() != null)
                        .map(e -> e.getAmount().toString())
                        .findFirst()
                        .orElse("N/A");

                SuspicionEntity suspicion = SuspicionEntity.builder()
                        .userName(userName)
                        .accountId(accountId)
                        .bankBic(bankBic)
                        .duplicateCount(duplicateCount)
                        .analysisDate(analysisDate)
                        .senderAccountId(senderAccountId)
                        .senderBankBic(senderBankBic)
                        .build();

                SuspicionEntity saved = suspicionRepository.save(suspicion);

                bitrix24Service.createSuspiciousActivityDeal(
                        saved.getId(),
                        userName,
                        account.getPhoneNumber(),
                        accountId.toString(),
                        bankBic,
                        String.valueOf(duplicateCount),
                        maxRiskLevel.name(),
                        String.format("Обнаружено %d подозрительных транзакций за последние 4 минуты.%nОтправители: %s (счета: %s)", duplicateCount, senderNames, senderBillIds),
                        senderAccountId != null ? senderAccountId.toString() : null,
                        senderBankBic,
                        totalAmount
                );
            }
        });
    }
}
