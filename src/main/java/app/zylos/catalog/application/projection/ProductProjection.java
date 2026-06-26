package app.zylos.catalog.application.projection;

import java.util.List;

import org.jspecify.annotations.Nullable;

public record ProductProjection(
        String productId,
        String sellerId,
        String name,
        @Nullable String description,
        String categoryId,
        String visibility,
        long version,
        List<AttributeProjection> attributes,
        List<VariantProjection> variants,
        long occurredAt,
        String eventId) {

    public record AttributeProjection(String name, String type, String value) {}

    public record VariantProjection(
            String variantId,
            String sku,
            String status,
            boolean purchasable,
            MoneyProjection listPrice,
            List<AttributeProjection> attributes) {}

    public record MoneyProjection(long minorUnits, String currency) {}
}
