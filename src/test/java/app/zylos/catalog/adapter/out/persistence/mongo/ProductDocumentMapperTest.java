package app.zylos.catalog.adapter.out.persistence.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.model.Product;
import app.zylos.catalog.domain.model.ProductStatus;
import app.zylos.catalog.domain.model.ProductVariant;
import app.zylos.catalog.domain.model.VariantStatus;
import app.zylos.catalog.domain.vo.*;

class ProductDocumentMapperTest {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void roundTripsAllFieldsThroughTheDocumentForm() {
        ProductAttributes productAttributes = ProductAttributes.empty()
                .with("brand", AttributeValue.of("Acme"))
                .with("warrantyYears", AttributeValue.of(new BigDecimal("2")))
                .with("wireless", AttributeValue.of(true));

        ProductVariant active = ProductVariant.reconstitute(
                VariantId.newId(),
                Sku.of("SKU-1"),
                Money.ofMinor(19999, USD),
                ProductAttributes.empty().with("color", AttributeValue.of("black")),
                VariantStatus.ACTIVE);
        ProductVariant inactive = ProductVariant.reconstitute(
                VariantId.newId(),
                Sku.of("SKU-2"),
                Money.ofMinor(17999, USD),
                ProductAttributes.empty(),
                VariantStatus.INACTIVE);

        Product original = Product.reconstitute(
                ProductId.newId(),
                SellerId.newId(),
                "Wireless Headphones",
                "Over-ear",
                CategoryId.newId(),
                productAttributes,
                ProductStatus.PUBLISHED,
                List.of(active, inactive),
                5L);

        Product restored = ProductDocumentMapper.toDomain(ProductDocumentMapper.toDocument(original));

        assertThat(restored.id()).isEqualTo(original.id());
        assertThat(restored.sellerId()).isEqualTo(original.sellerId());
        assertThat(restored.name()).isEqualTo(original.name());
        assertThat(restored.description()).isEqualTo(original.description());
        assertThat(restored.categoryId()).isEqualTo(original.categoryId());
        assertThat(restored.attributes()).isEqualTo(original.attributes());
        assertThat(restored.visibility()).isEqualTo(original.visibility());
        assertThat(restored.version()).isEqualTo(original.version());
        assertThat(restored.variants()).hasSize(2);

        ProductVariant restoredActive = restored.variants().getFirst();
        assertThat(restoredActive.id()).isEqualTo(active.id());
        assertThat(restoredActive.sku()).isEqualTo(active.sku());
        assertThat(restoredActive.listPrice()).isEqualTo(active.listPrice());
        assertThat(restoredActive.attributes()).isEqualTo(active.attributes());
        assertThat(restoredActive.status()).isEqualTo(VariantStatus.ACTIVE);

        assertThat(restored.variants().get(1).status()).isEqualTo(VariantStatus.INACTIVE);
    }

    @Test
    void roundTripsNullDescriptionAndEmptyAttributes() {
        Product original = Product.reconstitute(
                ProductId.newId(),
                SellerId.newId(),
                "Name",
                null,
                CategoryId.newId(),
                ProductAttributes.empty(),
                ProductStatus.DRAFT,
                List.of(ProductVariant.reconstitute(
                        VariantId.newId(),
                        Sku.of("SKU-1"),
                        Money.ofMinor(100, USD),
                        ProductAttributes.empty(),
                        VariantStatus.ACTIVE)),
                1L);

        Product restored = ProductDocumentMapper.toDomain(ProductDocumentMapper.toDocument(original));

        assertThat(restored.description()).isNull();
        assertThat(restored.attributes().isEmpty()).isTrue();
    }
}
