package app.zylos.catalog.domain.vo;

import java.util.*;

import app.zylos.catalog.domain.exception.CatalogDomainException;

/**
 * Immutable, ordered collection of named product attributes backing the variable attribute model.
 *
 * <p>Names are normalized (trimmed, lower-cased), must be unique and non-blank, and the collection
 * is capped to guard against unbounded documents. Mutating helpers return new instances, preserving
 * value semantics.
 */
public record ProductAttributes(Map<String, AttributeValue> values) {

    private static final int MAX_ATTRIBUTES = 100;

    public ProductAttributes {
        Objects.requireNonNull(values, "values must not be null");
        Map<String, AttributeValue> normalised = new LinkedHashMap<>();

        values.forEach((name, value) -> {
            String key = normaliseName(name);
            Objects.requireNonNull(value, () -> "attribute value for '%s' must not be null".formatted(key));

            if (normalised.put(key, value) != null) {
                throw new CatalogDomainException("Duplicate attribute name: " + key);
            }
        });

        if (normalised.size() > MAX_ATTRIBUTES) {
            throw new CatalogDomainException("A product may have at most %d attributes".formatted(MAX_ATTRIBUTES));
        }
        values = Collections.unmodifiableMap(normalised);
    }

    public static ProductAttributes empty() {
        return new ProductAttributes(Map.of());
    }

    private static String normaliseName(String name) {
        Objects.requireNonNull(name, "attribute name must not be null");
        String key = name.trim().toLowerCase(Locale.ROOT);

        if (key.isEmpty()) {
            throw new CatalogDomainException("Attribute name must not be blank");
        }
        return key;
    }

    public ProductAttributes with(String name, AttributeValue value) {
        Map<String, AttributeValue> copy = new LinkedHashMap<>(values);
        copy.put(normaliseName(name), Objects.requireNonNull(value, "value must not be null"));
        return new ProductAttributes(copy);
    }

    public ProductAttributes without(String name) {
        Map<String, AttributeValue> copy = new LinkedHashMap<>(values);
        copy.remove(normaliseName(name));
        return new ProductAttributes(copy);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }
}
