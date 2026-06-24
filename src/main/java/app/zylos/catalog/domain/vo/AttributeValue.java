package app.zylos.catalog.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;

import app.zylos.catalog.domain.exception.CatalogDomainException;

/**
 * Typed value of a product attribute. The closed set of variants (text, number, boolean) keeps the
 * variable attribute model expressive while remaining exhaustively handleable at compile time via
 * pattern matching.
 */
public sealed interface AttributeValue
        permits AttributeValue.TextValue, AttributeValue.NumberValue, AttributeValue.BooleanValue {

    static AttributeValue of(String value) {
        return new TextValue(value);
    }

    static AttributeValue of(BigDecimal value) {
        return new NumberValue(value);
    }

    static AttributeValue of(boolean value) {
        return new BooleanValue(value);
    }

    record TextValue(String value) implements AttributeValue {
        public TextValue {
            Objects.requireNonNull(value, "value must not be null");
            value = value.trim();

            if (value.isEmpty()) {
                throw new CatalogDomainException("Text attribute value must not be blank");
            }
        }
    }

    record NumberValue(BigDecimal value) implements AttributeValue {
        public NumberValue {
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    record BooleanValue(boolean value) implements AttributeValue {}
}
