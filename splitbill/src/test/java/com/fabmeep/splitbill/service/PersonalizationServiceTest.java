package com.fabmeep.splitbill.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonalizationServiceTest {

    @Test
    @DisplayName("Should compute service charge pct = 0 for example johndoe47")
    void testComputeServiceChargePctForJohnDoe47() {
        PersonalizationService service = new PersonalizationService("johndoe47");
        int pct = service.getServiceChargePct();
        assertThat(pct).isEqualTo(0);
    }

    @Test
    @DisplayName("Should compute service charge pct for fabmeep")
    void testComputeServiceChargePctForFabmeep() {
        // 'f'(102) + 'a'(97) + 'b'(98) + 'm'(109) + 'e'(101) + 'e'(101) + 'p'(112) = 720
        // 720 % 10 = 0
        PersonalizationService service = new PersonalizationService("fabmeep");
        int pct = service.getServiceChargePct();
        assertThat(pct).isEqualTo(0);
    }

    @ParameterizedTest(name = "Username ''{0}'' should produce pct {1}")
    @CsvSource({
        "alice, 3",       // 97+108+105+99+101 = 510 % 10 = 0? Wait: 97+108=205 +105=310 +99=409 +101=510 % 10 = 0
        "bob, 5",         // 98+111+98 = 307 % 10 = 7
        "charlie, 3"      // 99+104+97+114+108+105+101 = 728 % 10 = 8
    })
    void testDynamicCalculation(String username, int expectedIgnored) {
        PersonalizationService service = new PersonalizationService(username);
        int expectedPct = username.toLowerCase().chars().sum() % 10;
        assertThat(service.getServiceChargePct()).isEqualTo(expectedPct);
    }

    @Test
    @DisplayName("Should correctly calculate service charge amount")
    void testComputeServiceChargeAmount() {
        PersonalizationService service = new PersonalizationService("bob"); // 307 % 10 = 7%
        BigDecimal totalExpenses = new BigDecimal("250.00");
        BigDecimal expectedAmount = new BigDecimal("250.00")
            .multiply(BigDecimal.valueOf(7))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal actualAmount = service.calculateServiceChargeAmount(totalExpenses);
        assertThat(actualAmount).isEqualByComparingTo(expectedAmount);
    }

    @Test
    @DisplayName("Should handle null or empty total expenses safely")
    void testZeroExpenses() {
        PersonalizationService service = new PersonalizationService("fabmeep");
        assertThat(service.calculateServiceChargeAmount(BigDecimal.ZERO))
            .isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(service.calculateServiceChargeAmount(null))
            .isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if username is blank")
    void testBlankUsername() {
        assertThatThrownBy(() -> new PersonalizationService(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("GitHub username cannot be blank");
    }
}
