package app.zylos.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.event.*;
import app.zylos.catalog.domain.exception.CatalogDomainException;
import app.zylos.catalog.domain.exception.IllegalStatusTransitionException;
import app.zylos.catalog.domain.vo.*;

class ProductTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final SellerId SELLER = SellerId.newId();

    private static VariantDraft draft(String sku, long minor) {
        return new VariantDraft(VariantId.newId(), Sku.of(sku), Money.ofMinor(minor, USD), ProductAttributes.empty());
    }

    private static Product newDraftProduct(VariantDraft... drafts) {
        List<VariantDraft> initial = drafts.length == 0 ? List.of(draft("SKU-1", 19999)) : List.of(drafts);
        return Product.create(
                ProductId.newId(),
                SELLER,
                "Wireless Headphones",
                "Over-ear, noise cancelling",
                CategoryId.newId(),
                ProductAttributes.empty(),
                initial);
    }

    @Test
    void createStartsInDraftAtVersionOneWithActiveVariant() {
        Product product = newDraftProduct();
        assertThat(product.visibility()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.version()).isEqualTo(1L);
        assertThat(product.variants())
                .singleElement()
                .satisfies(v -> assertThat(v.status()).isEqualTo(VariantStatus.ACTIVE));
        assertThat(product.pullDomainEvents()).singleElement().isInstanceOf(ProductCreated.class);
        assertThat(product.pullDomainEvents()).isEmpty();
    }

    @Test
    void ownershipIsCapturedAtCreationAndSurvivesCommandsAndRoundTrip() {
        Product product = newDraftProduct();
        assertThat(product.sellerId()).isEqualTo(SELLER);

        // mutating commands must never change ownership
        product.updateDetails("New Name", null, CategoryId.newId(), ProductAttributes.empty());
        product.addVariant(draft("SKU-2", 100));
        product.publish();
        assertThat(product.sellerId()).isEqualTo(SELLER);

        // ownership round-trips through reconstitution
        Product rehydrated = Product.reconstitute(
                product.id(),
                product.sellerId(),
                product.name(),
                product.description(),
                product.categoryId(),
                product.attributes(),
                product.visibility(),
                product.variants(),
                product.version());
        assertThat(rehydrated.sellerId()).isEqualTo(SELLER);
    }

    @Test
    void createRejectsNullSeller() {
        assertThatThrownBy(() -> Product.create(
                        ProductId.newId(),
                        null,
                        "X",
                        null,
                        CategoryId.newId(),
                        ProductAttributes.empty(),
                        List.of(draft("SKU-1", 1))))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createRejectsEmptyVariantList() {
        assertThatThrownBy(() -> Product.create(
                        ProductId.newId(), SELLER, "X", null, CategoryId.newId(), ProductAttributes.empty(), List.of()))
                .isInstanceOf(CatalogDomainException.class)
                .hasMessageContaining("at least one variant");
    }

    @Test
    void createRejectsDuplicateSkus() {
        assertThatThrownBy(() -> newDraftProduct(draft("SKU-1", 100), draft("sku-1", 200)))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> Product.create(
                        ProductId.newId(),
                        SELLER,
                        "  ",
                        null,
                        CategoryId.newId(),
                        ProductAttributes.empty(),
                        List.of(draft("SKU-1", 1))))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void updateDetailsBumpsVersionAndRecordsEvent() {
        Product product = newDraftProduct();
        product.pullDomainEvents();
        product.updateDetails("New Name", null, CategoryId.newId(), ProductAttributes.empty());
        assertThat(product.name()).isEqualTo("New Name");
        assertThat(product.version()).isEqualTo(2L);
        assertThat(product.pullDomainEvents()).singleElement().isInstanceOf(ProductUpdated.class);
    }

    @Test
    void addVariantEnforcesUniquenessAndRecordsEvent() {
        Product product = newDraftProduct();
        product.pullDomainEvents();
        product.addVariant(draft("SKU-2", 5000));
        assertThat(product.version()).isEqualTo(2L);
        assertThat(product.variants()).hasSize(2);
        assertThat(product.pullDomainEvents()).singleElement().isInstanceOf(VariantAdded.class);
        assertThatThrownBy(() -> product.addVariant(draft("sku-2", 6000))).isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void updateVariantChangesBusinessFieldsWithUniquenessExcludingSelf() {
        Product product = newDraftProduct(draft("SKU-1", 100), draft("SKU-2", 200));
        VariantId v1 = product.variants().get(0).id();
        product.pullDomainEvents();

        product.updateVariant(v1, Sku.of("SKU-1"), Money.ofMinor(150, USD), ProductAttributes.empty());
        assertThat(product.variants().get(0).listPrice()).isEqualTo(Money.ofMinor(150, USD));
        assertThat(product.pullDomainEvents()).singleElement().isInstanceOf(VariantUpdated.class);

        assertThatThrownBy(() ->
                        product.updateVariant(v1, Sku.of("SKU-2"), Money.ofMinor(150, USD), ProductAttributes.empty()))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void publishRequiresAPurchasableVariant() {
        Product product = newDraftProduct();
        VariantId v = product.variants().get(0).id();
        product.deactivateVariant(v);
        assertThatThrownBy(product::publish)
                .isInstanceOf(CatalogDomainException.class)
                .hasMessageContaining("purchasable");
    }

    @Test
    void publishUnpublishRepublishCycle() {
        Product product = newDraftProduct();
        product.publish();
        assertThat(product.visibility()).isEqualTo(ProductStatus.PUBLISHED);
        product.unpublish();
        assertThat(product.visibility()).isEqualTo(ProductStatus.UNPUBLISHED);
        product.publish();
        assertThat(product.visibility()).isEqualTo(ProductStatus.PUBLISHED);
    }

    @Test
    void draftIsAOneWayOnRamp() {
        Product product = newDraftProduct();
        product.publish();
        product.unpublish();
        assertThat(product.visibility()).isEqualTo(ProductStatus.UNPUBLISHED);
        assertThat(ProductStatus.UNPUBLISHED.canTransitionTo(ProductStatus.DRAFT))
                .isFalse();
    }

    @Test
    void deactivatingLastPurchasableVariantAutoDemotesPublishedProduct() {
        Product product = newDraftProduct();
        product.publish();
        VariantId v = product.variants().get(0).id();
        product.pullDomainEvents();

        product.deactivateVariant(v);

        assertThat(product.visibility()).isEqualTo(ProductStatus.UNPUBLISHED);
        List<DomainEvent> events = product.pullDomainEvents();
        assertThat(events).hasSize(2);
        assertThat(events).anyMatch(VariantDeactivated.class::isInstance);
        assertThat(events).anyMatch(ProductUnpublished.class::isInstance);
        assertThat(events).allMatch(e -> e.version() == product.version());
    }

    @Test
    void deactivatingOneOfTwoVariantsDoesNotDemote() {
        Product product = newDraftProduct(draft("SKU-1", 100), draft("SKU-2", 200));
        product.publish();
        VariantId v1 = product.variants().get(0).id();
        product.deactivateVariant(v1);
        assertThat(product.visibility()).isEqualTo(ProductStatus.PUBLISHED);
    }

    @Test
    void discontinueCascadesToVariantsAndIsTerminal() {
        Product product = newDraftProduct(draft("SKU-1", 100), draft("SKU-2", 200));
        product.publish();
        product.pullDomainEvents();

        product.discontinue();

        assertThat(product.visibility()).isEqualTo(ProductStatus.DISCONTINUED);
        assertThat(product.variants()).allMatch(v -> v.status() == VariantStatus.DISCONTINUED);
        assertThat(product.pullDomainEvents()).singleElement().isInstanceOf(ProductDiscontinued.class);

        assertThatThrownBy(product::unpublish).isInstanceOf(IllegalStatusTransitionException.class);
        assertThatThrownBy(() -> product.addVariant(draft("SKU-9", 1))).isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void cannotUpdateDiscontinuedVariant() {
        Product product = newDraftProduct();
        VariantId v = product.variants().get(0).id();
        product.discontinueVariant(v);
        assertThatThrownBy(() ->
                        product.updateVariant(v, Sku.of("SKU-X"), Money.ofMinor(1, USD), ProductAttributes.empty()))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void variantNotFoundIsRejected() {
        Product product = newDraftProduct();
        assertThatThrownBy(() -> product.activateVariant(VariantId.newId()))
                .isInstanceOf(CatalogDomainException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void versionIncreasesMonotonicallyAcrossCommands() {
        Product product = newDraftProduct(); // v1
        product.updateDetails("n2", null, CategoryId.newId(), ProductAttributes.empty()); // v2
        product.addVariant(draft("SKU-2", 100)); // v3
        product.publish(); // v4
        product.deactivateVariant(product.variants().get(1).id()); // v5 (still one active -> no demote)
        product.discontinue(); // v6
        assertThat(product.version()).isEqualTo(6L);
    }

    @Test
    void reconstituteRestoresStateWithoutRecordingEvents() {
        ProductVariant variant = ProductVariant.reconstitute(
                VariantId.newId(),
                Sku.of("SKU-1"),
                Money.ofMinor(500, USD),
                ProductAttributes.empty(),
                VariantStatus.ACTIVE);
        Product product = Product.reconstitute(
                ProductId.newId(),
                SELLER,
                "Name",
                "desc",
                CategoryId.newId(),
                ProductAttributes.empty(),
                ProductStatus.PUBLISHED,
                List.of(variant),
                7L);
        assertThat(product.version()).isEqualTo(7L);
        assertThat(product.visibility()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(product.pullDomainEvents()).isEmpty();
    }

    @Test
    void reconstituteRejectsInvalidVersionAndEmptyVariants() {
        assertThatThrownBy(() -> Product.reconstitute(
                        ProductId.newId(),
                        SELLER,
                        "N",
                        null,
                        CategoryId.newId(),
                        ProductAttributes.empty(),
                        ProductStatus.DRAFT,
                        List.of(),
                        0L))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void equalityIsByIdentity() {
        ProductId id = ProductId.newId();
        ProductVariant variant = ProductVariant.reconstitute(
                VariantId.newId(),
                Sku.of("SKU-1"),
                Money.ofMinor(1, USD),
                ProductAttributes.empty(),
                VariantStatus.ACTIVE);
        Product p1 = Product.reconstitute(
                id,
                SELLER,
                "A",
                null,
                CategoryId.newId(),
                ProductAttributes.empty(),
                ProductStatus.DRAFT,
                List.of(variant),
                1L);
        Product p2 = Product.reconstitute(
                id,
                SellerId.newId(),
                "B",
                null,
                CategoryId.newId(),
                ProductAttributes.empty(),
                ProductStatus.UNPUBLISHED,
                List.of(variant),
                9L);
        assertThat(p1).isEqualTo(p2).hasSameHashCodeAs(p2);
    }
}
