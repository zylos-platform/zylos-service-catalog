package app.zylos.catalog.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.model.ProductStatus;
import app.zylos.catalog.domain.model.VariantStatus;
import app.zylos.catalog.domain.vo.*;

class ProductViewTest {

    private static final Currency USD = Currency.getInstance("USD");

    private static ProductVariantView variantView() {
        return new ProductVariantView(
                VariantId.newId(),
                Sku.of("SKU-1"),
                Money.ofMinor(100, USD),
                ProductAttributes.empty(),
                VariantStatus.ACTIVE);
    }

    @Test
    void variantsAreDefensivelyCopiedAndImmutable() {
        List<ProductVariantView> mutable = new ArrayList<>(List.of(variantView()));
        ProductView view = new ProductView(
                ProductId.newId(),
                "Name",
                null,
                CategoryId.newId(),
                ProductAttributes.empty(),
                ProductStatus.PUBLISHED,
                3L,
                mutable);
        mutable.clear();
        assertThat(view.variants()).hasSize(1);
        assertThatThrownBy(() -> view.variants().add(variantView())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThatThrownBy(() -> new ProductView(
                        null,
                        "Name",
                        null,
                        CategoryId.newId(),
                        ProductAttributes.empty(),
                        ProductStatus.DRAFT,
                        1L,
                        List.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
