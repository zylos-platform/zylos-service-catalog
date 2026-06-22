package app.zylos.catalog.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.exception.CatalogDomainException;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void ofMinorExposesAccessorsAndRendering() {
        Money money = Money.ofMinor(1234, USD);
        assertThat(money.minorUnits()).isEqualTo(1234L);
        assertThat(money.currency()).isEqualTo(USD);
        assertThat(money.toAmount()).isEqualByComparingTo("12.34");
        assertThat(money).hasToString("12.34 USD");
    }

    @Test
    void ofMajorScalesByCurrencyFractionDigits() {
        assertThat(Money.of(new BigDecimal("12.34"), USD)).isEqualTo(Money.ofMinor(1234, USD));
    }

    @Test
    void ofMajorRejectsExcessPrecision() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("12.345"), USD)).isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void plusAndMinusOperateWithinSameCurrency() {
        assertThat(Money.ofMinor(100, USD).plus(Money.ofMinor(50, USD))).isEqualTo(Money.ofMinor(150, USD));
        assertThat(Money.ofMinor(100, USD).minus(Money.ofMinor(150, USD))).isEqualTo(Money.ofMinor(-50, USD));
    }

    @Test
    void crossCurrencyArithmeticIsRejected() {
        assertThatThrownBy(() -> Money.ofMinor(100, USD).plus(Money.ofMinor(50, EUR)))
                .isInstanceOf(CatalogDomainException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void multipliedBy() {
        assertThat(Money.ofMinor(199, USD).multipliedBy(3)).isEqualTo(Money.ofMinor(597, USD));
    }

    @Test
    void additionOverflowIsRejected() {
        assertThatThrownBy(() -> Money.ofMinor(Long.MAX_VALUE, USD).plus(Money.ofMinor(1, USD)))
                .isInstanceOf(CatalogDomainException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    void signPredicatesAndNegation() {
        assertThat(Money.zero(USD).isZero()).isTrue();
        assertThat(Money.ofMinor(-1, USD).isNegative()).isTrue();
        assertThat(Money.ofMinor(1, USD).isPositive()).isTrue();
        assertThat(Money.ofMinor(5, USD).negated()).isEqualTo(Money.ofMinor(-5, USD));
    }

    @Test
    void comparisonsRequireSameCurrency() {
        assertThat(Money.ofMinor(100, USD).isGreaterThan(Money.ofMinor(50, USD)))
                .isTrue();
        assertThat(Money.ofMinor(50, USD).isLessThan(Money.ofMinor(100, USD))).isTrue();
        assertThatThrownBy(() -> Money.ofMinor(100, USD).compareTo(Money.ofMinor(100, EUR)))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void nullCurrencyIsRejected() {
        assertThatThrownBy(() -> Money.ofMinor(1, null)).isInstanceOf(NullPointerException.class);
    }
}
