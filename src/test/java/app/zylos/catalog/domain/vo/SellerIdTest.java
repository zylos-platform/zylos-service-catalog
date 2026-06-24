package app.zylos.catalog.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.exception.CatalogDomainException;

class SellerIdTest {

    @Test
    void newIdIsVersion7() {
        assertThat(SellerId.newId().value().version()).isEqualTo(7);
    }

    @Test
    void ofStringRejectsInvalid() {
        assertThatThrownBy(() -> SellerId.of("nope")).isInstanceOf(CatalogDomainException.class);
    }
}
