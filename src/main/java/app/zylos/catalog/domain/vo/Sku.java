package app.zylos.catalog.domain.vo;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import app.zylos.catalog.domain.exception.CatalogDomainException;

/**
 * Stock Keeping Unit carried by an individual {@code ProductVariant}.
 *
 * <p>Values are trimmed and upper-cased, then constrained to a conservative, URL- and
 * barcode-friendly character set. SKU uniqueness per seller is a persistence/aggregate concern,
 * not enforced by this value object.
 */
public record Sku(String value) {

    private static final int MAX_LENGTH = 64;
    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9._-]{0,63}$");

    public Sku {
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);

        if (value.isEmpty()) {
            throw new CatalogDomainException("SKU must not be blank");
        }

        if (value.length() > MAX_LENGTH) {
            throw new CatalogDomainException("SKU must be at most %d characters: %s".formatted(MAX_LENGTH, value));
        }

        if (!PATTERN.matcher(value).matches()) {
            throw new CatalogDomainException("SKU has invalid format: " + value);
        }
    }

    public static Sku of(String value) {
        return new Sku(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
