package app.zylos.catalog.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.exception.CatalogDomainException;

class ProductIdTest {

    @Test
    void newIdGeneratesVersion7Uuid() {
        assertThat(ProductId.newId().value().version()).isEqualTo(7);
    }

    @Test
    void ofUuidRoundTrips() {
        UUID uuid = UUID.randomUUID();
        assertThat(ProductId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void ofStringParses() {
        UUID uuid = UUID.randomUUID();
        assertThat(ProductId.of(uuid.toString())).isEqualTo(ProductId.of(uuid));
    }

    @Test
    void ofStringRejectsInvalid() {
        assertThatThrownBy(() -> ProductId.of("not-a-uuid")).isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void rejectsNullUuid() {
        assertThatThrownBy(() -> ProductId.of((UUID) null)).isInstanceOf(NullPointerException.class);
    }
}
