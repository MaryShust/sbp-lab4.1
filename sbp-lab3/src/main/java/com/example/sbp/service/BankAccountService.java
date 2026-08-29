package com.example.sbp.service;

import com.example.sbp.dto.BankAccountRequestDTO;
import com.example.sbp.dto.BankAccountResponseDTO;
import com.example.sbp.entity.BankAccountEntity;
import com.example.sbp.entity.BillEntity;
import com.example.sbp.exception.*;
import com.example.sbp.repository.BankAccountRepository;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.security.SecurityService;
import com.example.sbp.security.XmlUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final BankAccountRepository accountRepository;
    private final BillRepository billRepository;
    private final XmlUserDetailsService userDetailsService;
    private final SecurityService securityService;

    @Transactional
    public BankAccountResponseDTO createAccount(BankAccountRequestDTO bankAccountRequestDTO) {
        log.debug("Создать новый аккаунт с телефоном: {}", bankAccountRequestDTO.getPhoneNumber());

        validateOwnerName(bankAccountRequestDTO.getOwnerName());
        validateBankBic(bankAccountRequestDTO.getBankBic());
        validatePhoneNumber(bankAccountRequestDTO.getPhoneNumber());

        // Проверка уникальности на телефон
        if (accountRepository.existsByPhoneNumber(bankAccountRequestDTO.getPhoneNumber())) {
            throw new BankAccountAlreadyExistsException("Номер телефона уже существует");
        }

        BankAccountEntity account = BankAccountEntity.builder()
                .phoneNumber(bankAccountRequestDTO.getPhoneNumber())
                .ownerName(bankAccountRequestDTO.getOwnerName())
                .bankBic(bankAccountRequestDTO.getBankBic())
                .isActive(true)
                .allBillIds(new ArrayList<>())
                .build();

        account = accountRepository.save(account);
        log.debug("Аккаунт сохранен с ID: {}", account.getId());


        BillEntity defaultBillEntity = BillEntity.builder()
                .accountId(account.getId())
                .balance(BigDecimal.ZERO)
                .isActive(false)  // дефолтный счет требуется в дальнейшем активировать
                .build();

        defaultBillEntity = billRepository.save(defaultBillEntity);
        log.debug("Дефолтный счет создан с ID: {} (inactive)", defaultBillEntity.getId());

        // Обновление всего и вся
        account.setDefaultBillId(defaultBillEntity.getId());
        account.getAllBillIds().add(defaultBillEntity.getId());
        account = accountRepository.save(account);

        userDetailsService.updateUserAccountIdByPhone(account.getPhoneNumber(), account.getId());
        log.debug("Связали аккаунт {} с пользователем с номером телефона {}", account.getId(), account.getPhoneNumber());

        return mapToResponseDTO(account);
    }

    public BankAccountResponseDTO getAccountById(Long id) {
        securityService.checkPrivilegeReadAccount(id);

        BankAccountEntity account = accountRepository.findById(id)
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с id: " + id));
        return mapToResponseDTO(account);
    }

    public BankAccountResponseDTO getAccountByPhone(String phoneNumber) {
        BankAccountEntity account = accountRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с телефоном: " + phoneNumber));

        securityService.checkPrivilegeReadAccount(account.getId());

        return mapToResponseDTO(account);
    }

    @Transactional
    public void activateDefaultBill(Long accountId, BigDecimal startBalance) {
        securityService.checkPrivilegeActivateAccount(accountId);

        BankAccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("Аккаунт не найден с id: " + accountId));

        BillEntity defaultBillEntity = billRepository.findById(account.getDefaultBillId())
                .orElseThrow(() -> new BillNotFoundException("Дефолтный счет не найден"));

        // Баланс должен быть положительным
        if (defaultBillEntity.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            defaultBillEntity.setIsActive(true);
            defaultBillEntity.setBalance(startBalance);
            billRepository.save(defaultBillEntity);
            log.debug("Дефолтный счет {} активирован для аккаунта {}", defaultBillEntity.getId(), accountId);
        }
    }

    private BankAccountResponseDTO mapToResponseDTO(BankAccountEntity account) {
        BankAccountResponseDTO dto = new BankAccountResponseDTO();
        dto.setId(account.getId());
        dto.setPhoneNumber(account.getPhoneNumber());
        dto.setOwnerName(account.getOwnerName());
        dto.setBankBic(account.getBankBic());
        dto.setIsActive(account.getIsActive());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        dto.setDefaultBillId(account.getDefaultBillId());
        dto.setAllBillIds(account.getAllBillIds());
        return dto;
    }

    private void validateOwnerName(String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            throw new OwnerNameFormatException("Имя не может быть пустым");
        }

        String trimmed = ownerName.trim();
        if (trimmed.length() > 100) {
            throw new OwnerNameFormatException(
                    String.format("Имя владельца не должно превышать 100 символов, текущая длина: %d",
                            trimmed.length())
            );
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new PhoneNumberFormatException("Телефон не может быть пустым");
        }

        String trimmed = phoneNumber.trim();
        if (trimmed.length() > 11) {
            throw new PhoneNumberFormatException(
                    String.format("Номер телефона не должен превышать 11 символов, текущая длина: %d",
                            trimmed.length())
            );
        }

        // Проверка формата (международный формат)
        if (!trimmed.matches("^7[0-9]{10}$")) {
            throw new PhoneNumberFormatException("Неверный формат телефона");
        }
    }

    private void validateBankBic(String bankBic) {
        if (bankBic == null || bankBic.isBlank()) {
            throw new BankBicFormatException("BIC не может быть пустым");
        }

        String trimmed = bankBic.trim();

        // Проверка длины (BIC должен быть 8 или 11 символов)
        int length = trimmed.length();
        if (length < 8 || length > 11) {
            throw new BankBicFormatException(
                    String.format("Код BIC банка должен состоять из 8 или 11 символов, текущая длина: %d", length)
            );
        }

        // Проверка на запрещенные символы в BIC
        if (trimmed.contains(" ")) {
            throw new BankBicFormatException("BIC не может содержать пробелы");
        }
    }
}