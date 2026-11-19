package com.cocos.llallire.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Tests of {@link Returns}.
 */
class ReturnsTest {

    @Test
    void calculatesPositiveAndNegativeChangesRoundedToTwoDecimals() {
        assertThat(Returns.percentChange(new BigDecimal("100"), new BigDecimal("110"))).isEqualByComparingTo("10.00");
        assertThat(Returns.percentChange(new BigDecimal("3"), new BigDecimal("2"))).isEqualByComparingTo("-33.33");
    }

    @Test
    void returnsZeroWhenInitialValueIsZeroOrAnyValueIsMissing() {
        assertThat(Returns.percentChange(BigDecimal.ZERO, BigDecimal.TEN)).isEqualByComparingTo("0");
        assertThat(Returns.percentChange(null, BigDecimal.TEN)).isEqualByComparingTo("0");
        assertThat(Returns.percentChange(BigDecimal.TEN, null)).isEqualByComparingTo("0");
    }
}
