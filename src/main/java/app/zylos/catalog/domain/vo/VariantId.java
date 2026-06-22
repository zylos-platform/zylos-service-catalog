package app.zylos.catalog.domain.vo;

import java.util.Objects;
import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;

import app.zylos.catalog.domain.exception.CatalogDomainException;

/**
 * Identity of a {@code ProductVariant} entity within the {@code Product} aggregate.
 */
public record VariantId(UUID value) {

    public VariantId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static VariantId newId() {
        return new VariantId(UuidCreator.getTimeOrderedEpoch());
    }

    public static VariantId of(UUID value) {
        return new VariantId(value);
    }

    public static VariantId of(String value) {
        Objects.requireNonNull(value, "value must not be null");

        try {
            return new VariantId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new CatalogDomainException("Invalid VariantId: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
