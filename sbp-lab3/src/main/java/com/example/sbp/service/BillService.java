package com.example.sbp.service;

import com.example.sbp.dto.BillCreateRequestDTO;
import com.example.sbp.dto.BillResponseDTO;
import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.entity.BillEntity;
import com.example.sbp.exception.*;
import com.example.sbp.repository.BankAccountRepository;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.security.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final BankAccountRepository accountRepository;
    private final SecurityService securityService;

    @Transactional
    public BillResponseDTO createBill(BillCreateRequestDTO billDTO) {
        securityService.checkPrivilegeCreateBill(billDTO.getAccountId());

        BankAccountEntity account = accountRepository.findById(billDTO.getAccountId())
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден по id: " + billDTO.getAccountId()));

        BillEntity billEntity = BillEntity.builder()
                .accountId(billDTO.getAccountId())
                .balance(BigDecimal.ZERO)
                .isActive(true) // активный так как явно уже не первый
                .build();

        billEntity = billRepository.save(billEntity);

        // Обновление аккаунта
        account.getAllBillIds().add(billEntity.getId());
        accountRepository.save(account);

        return mapToResponseDTO(billEntity);
    }

    public BillResponseDTO getBillById(Long id) {
        securityService.checkPrivilegeReadBill(id);

        BillEntity billEntity = billRepository.findById(id)
                .orElseThrow(() -> new BillNotFoundException("Счет не найден по id: " + id));
        return mapToResponseDTO(billEntity);
    }

    @Transactional
    public BillResponseDTO replenishBill(Long accountId, Long billId, BigDecimal amount) {
        securityService.checkPrivilegeReplenishBill(billId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма пополнения должна быть положительной");
        }

        BillEntity billEntity = billRepository.findById(billId)
                .orElseThrow(() -> new BillNotFoundException("Счет не найден по id: " + billId));

        BankAccountEntity account = accountRepository.findById(billEntity.getAccountId())
                .orElseThrow(() -> new BankAccountNotFoundException(
                        "Аккаунт не найден для счета id: " + billId + ". Счет не принадлежит пользователю"));

        if (!account.getId().equals(accountId)) {
            throw new BillNotBelongAccountExeption("Счет не принадлежит аккаунту");
        }

        if (!account.getIsActive()) {
            throw new BillInactiveException("Аккаунт отправителя не активен");
        }

        if (!billEntity.getIsActive()) {
            billEntity.setIsActive(true);
        }

        // Пополнение счета
        BigDecimal newBalance = billEntity.getBalance().add(amount);
        billEntity.setBalance(newBalance);

        billRepository.save(billEntity);

        return mapToResponseDTO(billEntity);
    }

    public BillResponseDTO getDefaultBillByAccountId(Long accountId) {
        securityService.checkPrivilegeReadAccount(accountId);

        BankAccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден по id: " + accountId));

        BillEntity billEntity = billRepository.findById(account.getDefaultBillId())
                .orElseThrow(() -> new BillNotFoundException("Счет не найден по id: " + account.getDefaultBillId()));

        securityService.checkPrivilegeReadBill(billEntity.getId());

        return mapToResponseDTO(billEntity);
    }

    private BillResponseDTO mapToResponseDTO(BillEntity billEntity) {
        BillResponseDTO dto = new BillResponseDTO();
        dto.setId(billEntity.getId());
        dto.setAccountId(billEntity.getAccountId());
        dto.setBalance(billEntity.getBalance());
        dto.setIsActive(billEntity.getIsActive());
        dto.setCreatedAt(billEntity.getCreatedAt());
        dto.setUpdatedAt(billEntity.getUpdatedAt());
        return dto;
    }
}