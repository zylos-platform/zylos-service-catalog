package app.zylos.catalog.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.util.Currency;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.exception.CatalogDomainException;
import app.zylos.catalog.domain.exception.IllegalStatusTransitionException;
import app.zylos.catalog.domain.vo.Money;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.Sku;
import app.zylos.catalog.domain.vo.VariantId;

class ProductVariantTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void createStartsActiveAndExposesAccessors() {
        VariantId id = VariantId.newId();
        ProductVariant variant =
                ProductVariant.create(id, Sku.of("SKU-1"), Money.ofMinor(999, USD), ProductAttributes.empty());
        assertThat(variant.id()).isEqualTo(id);
        assertThat(variant.sku()).isEqualTo(Sku.of("SKU-1"));
        assertThat(variant.listPrice()).isEqualTo(Money.ofMinor(999, USD));
        assertThat(variant.status()).isEqualTo(VariantStatus.ACTIVE);
    }

    @Test
    void rejectsNegativePrice() {
        assertThatThrownBy(() -> ProductVariant.create(
                        VariantId.newId(), Sku.of("SKU-1"), Money.ofMinor(-1, USD), ProductAttributes.empty()))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void reconstituteRestoresGivenStatus() {
        ProductVariant variant = ProductVariant.reconstitute(
                VariantId.newId(),
                Sku.of("SKU-1"),
                Money.ofMinor(500, USD),
                ProductAttributes.empty(),
                VariantStatus.INACTIVE);
        assertThat(variant.status()).isEqualTo(VariantStatus.INACTIVE);
    }

    @Test
    void businessDetailsAreMutableViaRoot() {
        ProductVariant variant = ProductVariant.create(
                VariantId.newId(), Sku.of("SKU-1"), Money.ofMinor(100, USD), ProductAttributes.empty());
        variant.changeBusinessDetails(Sku.of("SKU-2"), Money.ofMinor(200, USD), ProductAttributes.empty());
        assertThat(variant.sku()).isEqualTo(Sku.of("SKU-2"));
        assertThat(variant.listPrice()).isEqualTo(Money.ofMinor(200, USD));
    }

    @Test
    void transitionToValidatesStateMachine() {
        ProductVariant variant = ProductVariant.create(
                VariantId.newId(), Sku.of("SKU-1"), Money.ofMinor(100, USD), ProductAttributes.empty());
        assertThatCode(() -> variant.transitionTo(VariantStatus.INACTIVE)).doesNotThrowAnyException();
        variant.transitionTo(VariantStatus.DISCONTINUED);
        assertThatThrownBy(() -> variant.transitionTo(VariantStatus.ACTIVE))
                .isInstanceOf(IllegalStatusTransitionException.class);
    }

    @Test
    void equalityIsByIdentityNotState() {
        VariantId id = VariantId.newId();
        ProductVariant a = ProductVariant.create(id, Sku.of("SKU-1"), Money.ofMinor(1, USD), ProductAttributes.empty());
        ProductVariant b = ProductVariant.create(id, Sku.of("SKU-2"), Money.ofMinor(2, USD), ProductAttributes.empty());
        ProductVariant c = ProductVariant.create(
                VariantId.newId(), Sku.of("SKU-1"), Money.ofMinor(1, USD), ProductAttributes.empty());
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}
