package com.itx.similar_products.controller;

import com.itx.similar_products.model.ProductDetail;
import com.itx.similar_products.service.SimilarProductsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ProductController {

    private final SimilarProductsService similarProductsService;

    public ProductController(SimilarProductsService similarProductsService) {
        this.similarProductsService = similarProductsService;
    }

    @GetMapping("/product/{productId}/similar")
    public Flux<ProductDetail> getSimilarProducts(@PathVariable String productId) {
        return similarProductsService.getSimilarProducts(productId);
    }
}
