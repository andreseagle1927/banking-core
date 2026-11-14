package com.banking.transfer.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new TransferRuleViolation("Amount must be positive and have at most two decimal places");
        if (currency == null || !currency.matches("[A-Z]{3}"))
            throw new TransferRuleViolation("Currency must be an uppercase ISO code");
        try {
            if (Currency.getInstance(currency).getDefaultFractionDigits() != 2)
                throw new TransferRuleViolation("V1 supports currencies with two minor-unit digits only");
        } catch (IllegalArgumentException ex) {
            throw new TransferRuleViolation("Unsupported ISO currency: " + currency);
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
