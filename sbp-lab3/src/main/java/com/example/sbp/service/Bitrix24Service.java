package com.example.sbp.service;

import com.example.sbp.entity.Bitrix24SyncEntity;
import com.example.sbp.jca.Bitrix24Connection;
import com.example.sbp.jca.bitrix24.Bitrix24ConnectionFactoryImpl;
import com.example.sbp.jca.bitrix24.Bitrix24ManagedConnectionFactory;
import com.example.sbp.repository.Bitrix24SyncRepository;
import jakarta.annotation.PostConstruct;
import jakarta.resource.ResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.Serializable;

@Service
@RequiredArgsConstructor
@Slf4j
public class Bitrix24Service implements Serializable {

    @Value("${bitrix24.webhook.url}")
    private String webhookUrl;

    private final Bitrix24SyncRepository bitrix24SyncRepository;
    private transient Bitrix24ConnectionFactoryImpl connectionFactory;

    @PostConstruct
    public void init() {
        try {
            Bitrix24ManagedConnectionFactory mcf = new Bitrix24ManagedConnectionFactory();
            mcf.setWebhookUrl(webhookUrl);
            connectionFactory = (Bitrix24ConnectionFactoryImpl) mcf.createConnectionFactory();
            log.info("Bitrix24 JCA connection factory initialized");
        } catch (ResourceException e) {
            log.error("Failed to initialize Bitrix24 connection factory", e);
        }
    }

    public Long createSuspiciousActivityDeal(
            Long suspicionId,
            String userName,
            String phone,
            String accountId,
            String bankBic,
            String duplicateCount,
            String riskLevel,
            String description,
            String senderAccountId,
            String senderBankBic,
            String totalAmount
    ) {
        if (bitrix24SyncRepository.findBySuspicionId(suspicionId).isPresent()) {
            log.info("Bitrix24 deal already exists for suspicion {}", suspicionId);
            return null;
        }

        if (connectionFactory == null) {
            log.warn("Bitrix24 connection factory not initialized");
            saveSyncRecord(suspicionId, null, Bitrix24SyncEntity.SyncStatus.FAILED, "Connection factory not initialized");
            return null;
        }

        try (Bitrix24Connection conn = connectionFactory.getConnection()) {
            String senderInfo = (senderAccountId != null && senderBankBic != null)
                    ? String.format(" | Отпр. ID: %s, БИК: %s", senderAccountId, senderBankBic)
                    : "";
            String title = String.format("Подозрительная активность: %s (ID аккаунта: %s, БИК: %s)%s", 
                    userName, accountId, bankBic, senderInfo);
            
            Long dealId = conn.createDeal(
                    title,
                    userName,
                    phone,
                    totalAmount,
                    riskLevel,
                    String.format("БИК получателя: %s\nБИК отправителя: %s\nID аккаунта получателя: %s\nID аккаунта отправителя: %s\nДубликатов: %s\n%s", 
                            bankBic, senderBankBic != null ? senderBankBic : "N/A", accountId, 
                            senderAccountId != null ? senderAccountId : "N/A", duplicateCount, 
                            description != null ? description : "")
            );
            
            saveSyncRecord(suspicionId, dealId, Bitrix24SyncEntity.SyncStatus.SUCCESS, null);
            log.info("Created Bitrix24 deal {} for suspicious user {}", dealId, userName);
            return dealId;
        } catch (ResourceException e) {
            log.error("Failed to create Bitrix24 deal for user {}", userName, e);
            saveSyncRecord(suspicionId, null, Bitrix24SyncEntity.SyncStatus.FAILED, e.getMessage());
            return null;
        }
    }

    private void saveSyncRecord(Long suspicionId, Long dealId, Bitrix24SyncEntity.SyncStatus status, String error) {
        try {
            Bitrix24SyncEntity sync = Bitrix24SyncEntity.builder()
                    .suspicionId(suspicionId)
                    .bitrix24DealId(dealId)
                    .syncStatus(status.name())
                    .errorMessage(error)
                    .build();
            bitrix24SyncRepository.save(sync);
        } catch (Exception e) {
            log.error("Failed to save sync record for suspicion {}", suspicionId, e);
        }
    }
}
