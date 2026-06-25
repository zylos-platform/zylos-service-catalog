package app.zylos.catalog.adapter.out.persistence.mongo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import app.zylos.catalog.domain.vo.AttributeValue;
import app.zylos.catalog.domain.vo.ProductAttributes;

final class AttributeDocumentMapper {

    private AttributeDocumentMapper() {}

    static List<AttributeDocument> toDocuments(ProductAttributes attributes) {
        List<AttributeDocument> documents = new ArrayList<>();
        attributes.values().forEach((name, value) -> documents.add(toDocument(name, value)));
        return documents;
    }

    static ProductAttributes toDomain(List<AttributeDocument> entries) {
        Map<String, AttributeValue> values = new LinkedHashMap<>();
        for (AttributeDocument entry : entries) {
            values.put(entry.name(), toValue(entry));
        }
        return new ProductAttributes(values);
    }

    private static AttributeDocument toDocument(String name, AttributeValue value) {
        return switch (value) {
            case AttributeValue.TextValue t -> new AttributeDocument(name, "TEXT", t.value(), null, null);
            case AttributeValue.NumberValue n -> new AttributeDocument(name, "NUMBER", null, n.value(), null);
            case AttributeValue.BooleanValue b -> new AttributeDocument(name, "BOOLEAN", null, null, b.value());
        };
    }

    private static AttributeValue toValue(AttributeDocument entry) {
        return switch (entry.type()) {
            case "TEXT" -> AttributeValue.of(Objects.requireNonNull(entry.text(), "text"));
            case "NUMBER" -> AttributeValue.of(Objects.requireNonNull(entry.number(), "number"));
            case "BOOLEAN" -> AttributeValue.of(Objects.requireNonNull(entry.bool(), "bool"));
            default -> throw new IllegalStateException("Unknown attribute type: " + entry.type());
        };
    }
}
