package com.example.sbp.security;

import com.example.sbp.exception.AccessDeniedException;
import com.example.sbp.repository.BillRepository;
import com.example.sbp.repository.SbpTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final BillRepository billRepository;
    private final SbpTransactionRepository transactionRepository;

    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    public Map<String, Object> getAuthVariables() {
        Map<String, Object> variables = new HashMap<>();
        CustomUserDetails user = getCurrentUser();
        variables.put("userName", user.getUsername());
        variables.put("role", user.getRole());
        variables.put("privileges", user.getPrivileges());
        variables.put("accountId", user.getAccountId());
        variables.put("phoneNumber", user.getPhoneNumber());
        return variables;
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
                isBillOwnedByCurrentUser(billId, user.getUsername(), user.getAccountId())
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для получения информации о счета");
    }

    public void checkPrivilegeReplenishBill(Long billId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.BILL_REPLENISH) &&
                isBillOwnedByCurrentUser(billId, user.getUsername(), user.getAccountId())
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для пополнения счета");
    }

    public void checkPrivilegeCreatePayment(Long senderBillId) {
        CustomUserDetails user = getCurrentUser();

        if (user != null &&
                user.hasPrivilege(Privilege.PAYMENT_CREATE) && isBillOwnedByCurrentUser(senderBillId, user.getUsername(), user.getAccountId())
        ) {
            return;
        }

        throw new AccessDeniedException("Не достаточно прав для создания перевода");
    }

    public boolean isBillOwnedByCurrentUser(Long billId, String userName, Long accountId) {
        if (userName == null || accountId == null) return false;

        return billRepository.findById(billId)
                .map(bill -> bill.getAccountId().equals(accountId))
                .orElse(false);
    }

    public boolean isTransactionRelatedToCurrentUser(String transactionId, String userName, Long accountId) {
        if (userName == null || accountId == null) return false;

        return transactionRepository.findByTransactionId(transactionId)
                .map(tx -> {
                    boolean isSender = isBillOwnedByCurrentUser(tx.getSenderBillId(), userName, accountId);
                    boolean isReceiver = isBillOwnedByCurrentUser(tx.getReceiverBillId(), userName, accountId);
                    return isSender || isReceiver;
                })
                .orElse(false);
    }
}
