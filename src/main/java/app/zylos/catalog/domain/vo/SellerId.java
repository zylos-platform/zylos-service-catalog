package app.zylos.catalog.domain.vo;

import java.util.Objects;
import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;

import app.zylos.catalog.domain.exception.CatalogDomainException;

public record SellerId(UUID value) {

    public SellerId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static SellerId newId() {
        return new SellerId(UuidCreator.getTimeOrderedEpoch());
    }

    public static SellerId of(UUID value) {
        return new SellerId(value);
    }

    public static SellerId of(String value) {
        Objects.requireNonNull(value, "value must not be null");

        try {
            return new SellerId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new CatalogDomainException("Invalid SellerId: " + value, e);
        }
    }
}
