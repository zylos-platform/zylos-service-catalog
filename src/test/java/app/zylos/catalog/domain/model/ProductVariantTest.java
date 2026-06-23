package app.zylos.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.Sku;
import app.zylos.catalog.domain.vo.VariantId;

class ProductVariantTest {

    @Test
    void exposesAccessors() {
        VariantId id = VariantId.newId();
        ProductVariant variant = ProductVariant.create(id, Sku.of("SKU-1"), ProductAttributes.empty());
        assertThat(variant.id()).isEqualTo(id);
        assertThat(variant.sku()).isEqualTo(Sku.of("SKU-1"));
        assertThat(variant.attributes()).isEqualTo(ProductAttributes.empty());
    }

    @Test
    void equalityIsByIdentityNotState() {
        VariantId id = VariantId.newId();
        ProductVariant a = ProductVariant.create(id, Sku.of("SKU-1"), ProductAttributes.empty());
        ProductVariant b = ProductVariant.create(id, Sku.of("SKU-2"), ProductAttributes.empty());
        ProductVariant c = ProductVariant.create(VariantId.newId(), Sku.of("SKU-1"), ProductAttributes.empty());
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void rejectsNulls() {
        assertThatThrownBy(() -> ProductVariant.create(null, Sku.of("SKU-1"), ProductAttributes.empty()))
                .isInstanceOf(NullPointerException.class);
    }
}
