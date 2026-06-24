package app.zylos.catalog.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.vo.CategoryId;
import app.zylos.catalog.domain.vo.Money;
import app.zylos.catalog.domain.vo.ProductAttributes;
import app.zylos.catalog.domain.vo.Sku;

class CreateProductCommandTest {

    private static final Currency USD = Currency.getInstance("USD");

    private static NewVariant variant() {
        return new NewVariant(Sku.of("SKU-1"), Money.ofMinor(100, USD), ProductAttributes.empty());
    }

    @Test
    void rejectsNullRequiredFields() {
        assertThatThrownBy(() -> new CreateProductCommand(
                        "key", null, null, CategoryId.newId(), ProductAttributes.empty(), List.of(variant())))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void variantsListIsDefensivelyCopiedAndImmutable() {
        List<NewVariant> mutable = new ArrayList<>(List.of(variant()));
        CreateProductCommand command =
                new CreateProductCommand("key", "Name", null, CategoryId.newId(), ProductAttributes.empty(), mutable);
        mutable.clear(); // must not affect the command
        assertThat(command.variants()).hasSize(1);
        assertThatThrownBy(() -> command.variants().add(variant())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullVariantElement() {
        List<NewVariant> withNull = new ArrayList<>();
        withNull.add(null);
        assertThatThrownBy(() -> new CreateProductCommand(
                        "key", "Name", null, CategoryId.newId(), ProductAttributes.empty(), withNull))
                .isInstanceOf(NullPointerException.class);
    }
}
