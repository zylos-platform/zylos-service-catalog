package app.zylos.catalog.domain.vo;

import java.util.Objects;
import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;

import app.zylos.catalog.domain.exception.CatalogDomainException;

/**
 * Identity of a {@code Product} aggregate.
 *
 * <p>New identities are time-ordered UUIDv7 (RFC 9562).
 */
public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static ProductId newId() {
        return new ProductId(UuidCreator.getTimeOrderedEpoch());
    }

    public static ProductId of(UUID value) {
        return new ProductId(value);
    }

    public static ProductId of(String value) {
        Objects.requireNonNull(value, "value must not be null");

        try {
            return new ProductId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new CatalogDomainException("Invalid ProductId: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
