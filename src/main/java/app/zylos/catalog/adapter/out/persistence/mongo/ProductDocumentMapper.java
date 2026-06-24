package app.zylos.catalog.adapter.out.persistence.mongo;

import java.util.*;

import app.zylos.catalog.domain.model.Product;
import app.zylos.catalog.domain.model.ProductStatus;
import app.zylos.catalog.domain.model.ProductVariant;
import app.zylos.catalog.domain.model.VariantStatus;
import app.zylos.catalog.domain.vo.*;
import app.zylos.catalog.domain.vo.AttributeValue.BooleanValue;
import app.zylos.catalog.domain.vo.AttributeValue.NumberValue;
import app.zylos.catalog.domain.vo.AttributeValue.TextValue;

public final class ProductDocumentMapper {

    private ProductDocumentMapper() {}

    public static ProductDocument toDocument(Product product) {
        List<VariantDocument> variants = new ArrayList<>();
        for (ProductVariant variant : product.variants()) {
            variants.add(toVariantDocument(variant));
        }
        return new ProductDocument(
                product.id().value().toString(),
                product.sellerId().value().toString(),
                product.name(),
                product.description(),
                product.categoryId().value().toString(),
                toAttributeDocuments(product.attributes()),
                product.visibility().name(),
                variants,
                product.version());
    }

    public static Product toDomain(ProductDocument document) {
        List<ProductVariant> variants = new ArrayList<>();
        for (VariantDocument variant : document.variants()) {
            variants.add(toVariantDomain(variant));
        }
        return Product.reconstitute(
                ProductId.of(document.id()),
                SellerId.of(document.sellerId()),
                document.name(),
                document.description(),
                CategoryId.of(document.categoryId()),
                toAttributesDomain(document.attributes()),
                ProductStatus.valueOf(document.visibility()),
                variants,
                document.version());
    }

    private static VariantDocument toVariantDocument(ProductVariant variant) {
        return new VariantDocument(
                variant.id().value().toString(),
                variant.sku().value(),
                variant.listPrice().minorUnits(),
                variant.listPrice().currency().getCurrencyCode(),
                toAttributeDocuments(variant.attributes()),
                variant.status().name());
    }

    private static ProductVariant toVariantDomain(VariantDocument document) {
        return ProductVariant.reconstitute(
                VariantId.of(document.id()),
                Sku.of(document.sku()),
                Money.ofMinor(document.priceMinorUnits(), Currency.getInstance(document.priceCurrency())),
                toAttributesDomain(document.attributes()),
                VariantStatus.valueOf(document.status()));
    }

    private static List<AttributeEntryDocument> toAttributeDocuments(ProductAttributes attributes) {
        List<AttributeEntryDocument> docs = new ArrayList<>();
        attributes.values().forEach((name, value) -> docs.add(toAttributeDocument(name, value)));
        return docs;
    }

    private static AttributeEntryDocument toAttributeDocument(String name, AttributeValue value) {
        return switch (value) {
            case TextValue t -> new AttributeEntryDocument(name, "TEXT", t.value(), null, null);
            case NumberValue n -> new AttributeEntryDocument(name, "NUMBER", null, n.value(), null);
            case BooleanValue b -> new AttributeEntryDocument(name, "BOOLEAN", null, null, b.value());
        };
    }

    private static ProductAttributes toAttributesDomain(List<AttributeEntryDocument> docs) {
        Map<String, AttributeValue> values = new LinkedHashMap<>();
        for (AttributeEntryDocument doc : docs) {
            values.put(doc.name(), toAttributeValue(doc));
        }
        return new ProductAttributes(values);
    }

    private static AttributeValue toAttributeValue(AttributeEntryDocument doc) {
        return switch (doc.type()) {
            case "TEXT" -> AttributeValue.of(Objects.requireNonNull(doc.text(), "text"));
            case "NUMBER" -> AttributeValue.of(Objects.requireNonNull(doc.number(), "number"));
            case "BOOLEAN" -> AttributeValue.of(Objects.requireNonNull(doc.bool(), "bool"));
            default -> throw new IllegalStateException("Unknown attribute type: " + doc.type());
        };
    }
}
