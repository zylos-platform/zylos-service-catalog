package app.zylos.catalog.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.zylos.catalog.domain.exception.CatalogDomainException;
import app.zylos.catalog.domain.vo.AttributeValue.BooleanValue;
import app.zylos.catalog.domain.vo.AttributeValue.NumberValue;
import app.zylos.catalog.domain.vo.AttributeValue.TextValue;

class AttributeValueTest {

    @Test
    void textValueIsTrimmed() {
        assertThat(((TextValue) AttributeValue.of("  red  ")).value()).isEqualTo("red");
    }

    @Test
    void blankTextValueIsRejected() {
        assertThatThrownBy(() -> AttributeValue.of("   ")).isInstanceOf(CatalogDomainException.class);
    }

    @Test
    void numberAndBooleanFactories() {
        assertThat(AttributeValue.of(new BigDecimal("1.5"))).isInstanceOf(NumberValue.class);
        assertThat(AttributeValue.of(true)).isEqualTo(new BooleanValue(true));
    }

    @Test
    void sealedHierarchyIsExhaustivelySwitchable() {
        AttributeValue value = AttributeValue.of("blue");

        String description =
                switch (value) {
                    case TextValue t -> "text:" + t.value();
                    case NumberValue n -> "number:" + n.value();
                    case BooleanValue b -> "boolean:" + b.value();
                };
        assertThat(description).isEqualTo("text:blue");
    }
}
