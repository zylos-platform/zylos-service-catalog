package app.zylos.catalog.adapter.out.persistence.mongo;

import java.util.List;

/**
 * Embedded persistence representation of a {@code ProductVariant}.
 */
public record VariantDocument(
        String id,
        String sku,
        long priceMinorUnits,
        String priceCurrency,
        List<AttributeDocument> attributes,
        String status) {}
