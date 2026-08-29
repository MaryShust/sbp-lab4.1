package com.example.sbp.security;

import com.example.sbp.exception.AccessDeniedException;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.repository.SbpTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final BillRepository billRepository;
    private final SbpTransactionRepository transactionRepository;

    public CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    public void checkPrivilegeReadAccount(Long accountId) {
        CustomUserDetails user = getCurrentUser();
        log.info("SecurityService checkPrivilegeReadAccount: {}", user);

        if (user != null && user.hasPrivilege(Privilege.ACCOUNT_SUPER_READ)) {
            return;
        }

        if (user != null &&
                user.hasPrivilege(Privilege.ACCOUNT_READ) &&
                user.hasPrivilege(Privilege.ACCOUNT_READ_BY_PHONE) &&
                user.getAccountId() != null &&
                user.getAccountId().equals(accountId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для получения информации об аккаунте");
    }

    public void checkPrivilegeActivateAccount(Long accountId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.ACCOUNT_ACTIVATE) &&
                user.getAccountId() != null && user.getAccountId().equals(accountId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для активации аккаунта");
    }

    public void checkPrivilegeCreateBill(Long accountId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.BILL_CREATE) &&
                user.getAccountId() != null && user.getAccountId().equals(accountId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для создания счета");
    }

    public void checkPrivilegeReadBill(Long billId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null && user.hasPrivilege(Privilege.BILL_SUPER_READ)) {
            return;
        }

        if (user != null &&
                user.hasPrivilege(Privilege.BILL_READ) &&
                user.hasPrivilege(Privilege.BILL_READ_DEFAULT) &&
                isBillOwnedByCurrentUser(billId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для получения информации о счета");
    }

    public void checkPrivilegeReplenishBill(Long billId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.BILL_REPLENISH) &&
                isBillOwnedByCurrentUser(billId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для пополнения счета");
    }

    public void checkPrivilegeReadPaymentStatus(String transactionId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null && user.hasPrivilege(Privilege.PAYMENT_SUPER_READ_STATUS)) {
            return;
        }

        if (user != null &&
                user.hasPrivilege(Privilege.PAYMENT_READ_STATUS) &&
                isTransactionRelatedToCurrentUser(transactionId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для чтения статусов транзакции");
    }

    public void checkPrivilegeCreatePayment(Long senderBillId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.PAYMENT_CREATE) && isBillOwnedByCurrentUser(senderBillId)
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для создания перевода");
    }

    private boolean isBillOwnedByCurrentUser(Long billId) {
        CustomUserDetails user = getCurrentUser();
        if (user == null || user.getAccountId() == null) return false;

        return billRepository.findById(billId)
                .map(bill -> bill.getAccountId().equals(user.getAccountId()))
                .orElse(false);
    }

    private boolean isTransactionRelatedToCurrentUser(String transactionId) {
        CustomUserDetails user = getCurrentUser();
        if (user == null || user.getAccountId() == null) return false;

        return transactionRepository.findByTransactionId(transactionId)
                .map(tx -> {
                    boolean isSender = isBillOwnedByCurrentUser(tx.getSenderBillId());
                    boolean isReceiver = isBillOwnedByCurrentUser(tx.getReceiverBillId());
                    return isSender || isReceiver;
                })
                .orElse(false);
    }
}
