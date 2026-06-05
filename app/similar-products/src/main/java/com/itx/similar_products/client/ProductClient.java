package com.itx.similar_products.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.itx.similar_products.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);
    private static final Duration PRODUCT_DETAIL_TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;
    private final Cache<String, ProductDetail> productCache;
    private final Set<String> backgroundFetchInProgress = ConcurrentHashMap.newKeySet();

    public ProductClient(WebClient productWebClient) {
        this.webClient = productWebClient;
        this.productCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1000)
                .build();
    }

    public Flux<String> getSimilarIds(String productId) {
        log.info("Fetching similar ids for productId={}", productId);
        return webClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        _ -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Product not found: " + productId)))
                .bodyToMono(new ParameterizedTypeReference<List<Integer>>() {})
                .flatMapMany(Flux::fromIterable)
                .map(String::valueOf);
    }

    public Mono<ProductDetail> getProductDetail(String productId) {
        ProductDetail cached = productCache.getIfPresent(productId);
        if (cached != null) {
            log.info("Cache hit for productId={}", productId);
            return Mono.just(cached);
        }

        log.info("Cache miss for productId={}, fetching from API", productId);
        return fetchProductDetail(productId)
                .timeout(PRODUCT_DETAIL_TIMEOUT)
                .doOnNext(detail -> productCache.put(productId, detail))
                .doOnError(TimeoutException.class, _ -> {
                    if (backgroundFetchInProgress.add(productId)) {
                        log.warn("Timeout for productId={}, scheduling background fetch", productId);
                        fetchProductDetail(productId)
                                .doOnNext(detail -> {
                                    productCache.put(productId, detail);
                                    log.info("Background fetch completed for productId={}", productId);
                                })
                                .doFinally(_ -> backgroundFetchInProgress.remove(productId))
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                    } else {
                        log.info("Background fetch already in progress for productId={}, skipping", productId);
                    }
                })
                .onErrorResume(e -> !(e instanceof ResponseStatusException), _ -> Mono.empty());
    }

    private Mono<ProductDetail> fetchProductDetail(String productId) {
        return webClient.get()
                .uri("/product/{productId}", productId)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(ProductDetail.class);
                    }
                    log.warn("Non-2xx response for productId={}: status={}", productId, response.statusCode());
                    return response.releaseBody().then(Mono.empty());
                });
    }
}
