package com.itx.similar_products.service;

import com.itx.similar_products.client.ProductClient;
import com.itx.similar_products.model.ProductDetail;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class SimilarProductsService {

    private final ProductClient productClient;

    public SimilarProductsService(ProductClient productClient) {
        this.productClient = productClient;
    }

    public Flux<ProductDetail> getSimilarProducts(String productId) {
        return productClient.getSimilarIds(productId)
                .flatMap(productClient::getProductDetail)
                .filter(detail -> true);
    }
}
