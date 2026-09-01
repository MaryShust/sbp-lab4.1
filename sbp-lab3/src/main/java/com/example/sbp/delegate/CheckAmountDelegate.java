package com.example.sbp.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class CheckAmountDelegate implements JavaDelegate {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.005");
    private static final BigDecimal MIN_COMMISSION = new BigDecimal("10");
    private static final BigDecimal MAX_COMMISSION = new BigDecimal("1000");

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("TEST Calculating commission");

        // Get amount as String and parse to BigDecimal
        BigDecimal amount = (BigDecimal) execution.getVariable("amount");
//        BigDecimal amount = new BigDecimal(amountStr);
        Long senderAccountId = (Long) execution.getVariable("accountId");
        Long receiverAccountId = (Long) execution.getVariable("receiverAccountId");
        BigDecimal senderBalance = (BigDecimal) execution.getVariable("senderBalance");
//        BigDecimal senderBalance = new BigDecimal(balanceStr);


        BigDecimal commission = BigDecimal.ZERO;
        if (!senderAccountId.equals(receiverAccountId)) {
            commission = calculateCommission(amount);
        }

        // Проверить достаточность средств
        BigDecimal totalAmount = amount.add(commission);
        if (senderBalance.compareTo(totalAmount) < 0) {
            execution.setVariable("isAmountCorrect", false);
            execution.setVariable("bpmnError", "INSUFFICIENT");
            execution.setVariable("bpmnErrorMessage", "Недостаточно средств на счете отправителя");
            return;
        }

        execution.setVariable("isAmountCorrect", true);
        execution.setVariable("commission", commission);
    }

    private BigDecimal calculateCommission(BigDecimal amount) {
        BigDecimal commission = amount.multiply(COMMISSION_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        // Проверка минимальной и максимальной комиссии
        if (commission.compareTo(MIN_COMMISSION) < 0) {
            return MIN_COMMISSION;
        }
        if (commission.compareTo(MAX_COMMISSION) > 0) {
            return MAX_COMMISSION;
        }
        return commission;
    }
}