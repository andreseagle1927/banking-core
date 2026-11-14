package com.banking.transfer.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test
    void normalizesAcceptedTwoDigitAmounts() {
        assertEquals(new BigDecimal("10.00"), new Money(new BigDecimal("10"), "USD").amount());
        assertEquals(new BigDecimal("10.10"), new Money(new BigDecimal("10.1"), "EUR").amount());
    }

    @Test
    void rejectsInvalidAmountsAndCurrencies() {
        assertThrows(TransferRuleViolation.class, () -> new Money(new BigDecimal("0.00"), "USD"));
        assertThrows(TransferRuleViolation.class, () -> new Money(new BigDecimal("1.001"), "USD"));
        assertThrows(TransferRuleViolation.class, () -> new Money(new BigDecimal("1.00"), "JPY"));
        assertThrows(TransferRuleViolation.class, () -> new Money(new BigDecimal("1.00"), "KWD"));
        assertThrows(TransferRuleViolation.class, () -> new Money(new BigDecimal("1.00"), "usd"));
    }
}
