package app.zylos.catalog.adapter.out.persistence.mongo;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
public record ProductDocument(
        @Id String id,

        String sellerId,
        String name,

        @Nullable String description,

        String categoryId,
        List<AttributeDocument> attributes,
        String status,
        List<VariantDocument> variants,
        long version) {}
