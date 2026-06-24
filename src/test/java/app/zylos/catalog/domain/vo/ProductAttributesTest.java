package app.zylos.catalog.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.exception.CatalogDomainException;

class ProductAttributesTest {

    private static final int MAX = 100;

    @Test
    void emptyHasNoAttributes() {
        assertThat(ProductAttributes.empty().isEmpty()).isTrue();
        assertThat(ProductAttributes.empty().size()).isZero();
    }

    @Test
    void normalisesNamesAndExposesImmutableMap() {
        ProductAttributes attributes = ProductAttributes.empty().with("  Color ", AttributeValue.of("red"));
        assertThat(attributes.values()).containsOnlyKeys("color");
        assertThatThrownBy(() -> attributes.values().put("x", AttributeValue.of("y")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void withReturnsNewInstanceLeavingOriginalUnchanged() {
        ProductAttributes original = ProductAttributes.empty().with("color", AttributeValue.of("red"));
        ProductAttributes updated = original.with("size", AttributeValue.of("L"));
        assertThat(original.size()).isEqualTo(1);
        assertThat(updated.size()).isEqualTo(2);
    }

    @Test
    void withoutRemovesAttributeByNormalisedName() {
        ProductAttributes attributes = ProductAttributes.empty()
                .with("color", AttributeValue.of("red"))
                .without("Color");
        assertThat(attributes.isEmpty()).isTrue();
    }

    @Test
    void rejectsNamesCollidingAfterNormalisation() {
        Map<String, AttributeValue> input = new LinkedHashMap<>();
        input.put("Color", AttributeValue.of("red"));
        input.put("color", AttributeValue.of("blue"));
        assertThatThrownBy(() -> new ProductAttributes(input)).isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> ProductAttributes.empty().with("   ", AttributeValue.of("x")))
                .isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void enforcesMaximumAttributeCount() {
        Map<String, AttributeValue> input = new LinkedHashMap<>();
        for (int i = 0; i <= MAX; i++) {
            input.put("attr" + i, AttributeValue.of("v" + i));
        }

        assertThatThrownBy(() -> new ProductAttributes(input)).isInstanceOf(CatalogDomainException.class);
    }
}
