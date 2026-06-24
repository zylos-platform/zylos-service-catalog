package app.zylos.catalog.adapter.out.persistence.mongo;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record AttributeEntryDocument(
        String name,
        String type,
        @Nullable String text,
        @Nullable BigDecimal number,
        @Nullable Boolean bool) {}
