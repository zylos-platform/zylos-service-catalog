package app.zylos.catalog.domain.vo;

import java.util.Objects;
import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;

import app.zylos.catalog.domain.exception.CatalogDomainException;

/**
 * Reference to a category a product is classified under. Category management is a separate
 * concern; the Catalog domain holds only the typed identifier.
 */
public record CategoryId(UUID value) {

    public CategoryId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static CategoryId newId() {
        return new CategoryId(UuidCreator.getTimeOrderedEpoch());
    }

    public static CategoryId of(UUID value) {
        return new CategoryId(value);
    }

    public static CategoryId of(String value) {
        Objects.requireNonNull(value, "value must not be null");

        try {
            return new CategoryId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new CatalogDomainException("Invalid CategoryId: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
