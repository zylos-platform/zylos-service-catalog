package app.zylos.catalog.adapter.out.persistence.mongo;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import app.zylos.catalog.domain.model.Product;
import app.zylos.catalog.domain.model.ProductStatus;
import app.zylos.catalog.domain.model.ProductVariant;
import app.zylos.catalog.domain.model.VariantStatus;
import app.zylos.catalog.domain.vo.*;

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
                AttributeDocumentMapper.toDocuments(product.attributes()),
                product.status().name(),
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
                AttributeDocumentMapper.toDomain(document.attributes()),
                ProductStatus.valueOf(document.status()),
                variants,
                document.version());
    }

    private static VariantDocument toVariantDocument(ProductVariant variant) {
        return new VariantDocument(
                variant.id().value().toString(),
                variant.sku().value(),
                variant.listPrice().minorUnits(),
                variant.listPrice().currency().getCurrencyCode(),
                AttributeDocumentMapper.toDocuments(variant.attributes()),
                variant.status().name());
    }

    private static ProductVariant toVariantDomain(VariantDocument document) {
        return ProductVariant.reconstitute(
                VariantId.of(document.id()),
                Sku.of(document.sku()),
                Money.ofMinor(document.priceMinorUnits(), Currency.getInstance(document.priceCurrency())),
                AttributeDocumentMapper.toDomain(document.attributes()),
                VariantStatus.valueOf(document.status()));
    }
}
