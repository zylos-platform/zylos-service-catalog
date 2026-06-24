package app.zylos.catalog.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import app.zylos.catalog.domain.exception.CatalogDomainException;

class SkuTest {

    @Test
    void normalisesToTrimmedUpperCase() {
        assertThat(Sku.of("  abc-123 ").value()).isEqualTo("ABC-123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SKU-001", "A", "PROD.123_X", "0ABC"})
    void acceptsValidSkus(String raw) {
        assertThat(Sku.of(raw).value()).isEqualTo(raw.toUpperCase(Locale.ROOT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "-LEADING", ".dot", "has space", "has/slash", "héllo"})
    void rejectsInvalidSkus(String raw) {
        assertThatThrownBy(() -> Sku.of(raw)).isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void rejectsTooLongSku() {
        assertThatThrownBy(() -> Sku.of("A".repeat(65))).isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Sku.of(null)).isInstanceOf(NullPointerException.class);
    }
}
