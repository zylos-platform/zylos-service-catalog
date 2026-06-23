package app.zylos.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.event.*;
import app.zylos.catalog.domain.exception.CatalogDomainException;
import app.zylos.catalog.domain.vo.*;

class ProductTest {

    private static final Currency USD = Currency.getInstance("USD");

    private static Product newDraft() {
        return Product.create(
                ProductId.newId(),
                "Wireless Headphones",
                "Over-ear, noise cancelling",
                CategoryId.newId(),
                Money.ofMinor(19999, USD),
                ProductAttributes.empty());
    }

    @Test
    void createStartsInDraftAtVersionOneAndRecordsEvent() {
        Product product = newDraft();
        assertThat(product.status()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.version()).isEqualTo(1L);

        List<DomainEvent> events = product.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOf(ProductCreated.class);
        assertThat(events.getFirst().version()).isEqualTo(1L);
        assertThat(product.pullDomainEvents()).isEmpty(); // cleared after pull
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Product.create(
                        ProductId.newId(),
                        "  ",
                        null,
                        CategoryId.newId(),
                        Money.ofMinor(1, USD),
                        ProductAttributes.empty()))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void createRejectsNegativeListPrice() {
        assertThatThrownBy(() -> Product.create(
                        ProductId.newId(),
                        "X",
                        null,
                        CategoryId.newId(),
                        Money.ofMinor(-1, USD),
                        ProductAttributes.empty()))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void updateDetailsBumpsVersionAndRecordsEvent() {
        Product product = newDraft();
        product.pullDomainEvents();

        product.updateDetails(
                "New Name", null, CategoryId.newId(), Money.ofMinor(9999, USD), ProductAttributes.empty());

        assertThat(product.name()).isEqualTo("New Name");
        assertThat(product.version()).isEqualTo(2L);
        assertThat(product.pullDomainEvents()).singleElement().isInstanceOf(ProductUpdated.class);
    }

    @Test
    void addVariantEnforcesSkuUniquenessAndRecordsEvent() {
        Product product = newDraft();
        product.pullDomainEvents();

        product.addVariant(VariantId.newId(), Sku.of("SKU-1"), ProductAttributes.empty());
        assertThat(product.version()).isEqualTo(2L);
        assertThat(product.variants()).hasSize(1);
        assertThat(product.pullDomainEvents()).singleElement().isInstanceOf(VariantAdded.class);

        // normalized SKU ("sku-1" -> "SKU-1") collides
        assertThatThrownBy(() -> product.addVariant(VariantId.newId(), Sku.of("sku-1"), ProductAttributes.empty()))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void publishRequiresAtLeastOneVariant() {
        Product product = newDraft();
        assertThatThrownBy(product::publish)
                .isInstanceOf(CatalogDomainException.class)
                .hasMessageContaining("no variants");
    }

    @Test
    void publishTransitionsAndRecordsEvent() {
        Product product = newDraft();
        product.addVariant(VariantId.newId(), Sku.of("SKU-1"), ProductAttributes.empty());
        product.pullDomainEvents();

        product.publish();

        assertThat(product.status()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(product.version()).isEqualTo(3L);
        assertThat(product.pullDomainEvents()).singleElement().isInstanceOf(ProductPublished.class);
    }

    @Test
    void republishingIsRejected() {
        Product product = newDraft();
        product.addVariant(VariantId.newId(), Sku.of("SKU-1"), ProductAttributes.empty());
        product.publish();
        assertThatThrownBy(product::publish).isInstanceOf(IllegalStatusTransitionException.class);
    }

    @Test
    void discontinueIsTerminalAndBlocksFurtherModification() {
        Product product = newDraft();
        product.addVariant(VariantId.newId(), Sku.of("SKU-1"), ProductAttributes.empty());
        product.publish();
        product.pullDomainEvents();

        product.discontinue();
        assertThat(product.status()).isEqualTo(ProductStatus.DISCONTINUED);
        assertThat(product.pullDomainEvents()).singleElement().isInstanceOf(ProductDiscontinued.class);

        assertThatThrownBy(product::discontinue).isInstanceOf(IllegalStatusTransitionException.class);
        assertThatThrownBy(() -> product.updateDetails(
                        "n", null, CategoryId.newId(), Money.ofMinor(1, USD), ProductAttributes.empty()))
                .isInstanceOf(CatalogDomainException.class);
        assertThatThrownBy(() -> product.addVariant(VariantId.newId(), Sku.of("SKU-2"), ProductAttributes.empty()))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void versionIncreasesMonotonicallyAcrossCommands() {
        Product product = newDraft(); // v1
        product.updateDetails("n2", null, CategoryId.newId(), Money.ofMinor(1, USD), ProductAttributes.empty()); // v2
        product.addVariant(VariantId.newId(), Sku.of("SKU-1"), ProductAttributes.empty()); // v3
        product.publish(); // v4
        product.discontinue(); // v5
        assertThat(product.version()).isEqualTo(5L);
    }

    @Test
    void reconstituteRestoresStateWithoutRecordingEvents() {
        ProductVariant variant = ProductVariant.create(VariantId.newId(), Sku.of("SKU-1"), ProductAttributes.empty());
        Product product = Product.reconstitute(
                ProductId.newId(),
                "Name",
                "desc",
                CategoryId.newId(),
                Money.ofMinor(500, USD),
                ProductAttributes.empty(),
                ProductStatus.PUBLISHED,
                List.of(variant),
                7L);

        assertThat(product.version()).isEqualTo(7L);
        assertThat(product.status()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(product.pullDomainEvents()).isEmpty();
    }

    @Test
    void reconstituteRejectsInvalidVersion() {
        assertThatThrownBy(() -> Product.reconstitute(
                        ProductId.newId(),
                        "Name",
                        null,
                        CategoryId.newId(),
                        Money.ofMinor(1, USD),
                        ProductAttributes.empty(),
                        ProductStatus.DRAFT,
                        List.of(),
                        0L))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void reconstituteRejectsDuplicateVariantSkus() {
        ProductVariant a = ProductVariant.create(VariantId.newId(), Sku.of("SKU-1"), ProductAttributes.empty());
        ProductVariant b = ProductVariant.create(VariantId.newId(), Sku.of("SKU-1"), ProductAttributes.empty());
        assertThatThrownBy(() -> Product.reconstitute(
                        ProductId.newId(),
                        "Name",
                        null,
                        CategoryId.newId(),
                        Money.ofMinor(1, USD),
                        ProductAttributes.empty(),
                        ProductStatus.DRAFT,
                        List.of(a, b),
                        1L))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void equalityIsByIdentity() {
        ProductId id = ProductId.newId();
        Product p1 = Product.reconstitute(
                id,
                "A",
                null,
                CategoryId.newId(),
                Money.ofMinor(1, USD),
                ProductAttributes.empty(),
                ProductStatus.DRAFT,
                List.of(),
                1L);
        Product p2 = Product.reconstitute(
                id,
                "B",
                null,
                CategoryId.newId(),
                Money.ofMinor(2, USD),
                ProductAttributes.empty(),
                ProductStatus.DRAFT,
                List.of(),
                9L);
        assertThat(p1).isEqualTo(p2).hasSameHashCodeAs(p2);
    }
}
