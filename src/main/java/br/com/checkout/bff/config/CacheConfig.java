package br.com.checkout.bff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * Cache (Redis) das consultas de produtos e meios de pagamento.
 * Se o Redis cair, o BFF registra o erro e consulta o serviço direto: cache nunca derruba o checkout.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String PRODUCTS = "products";
    public static final String PAYMENT_METHODS = "payment-methods";

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache {} indisponível na leitura: {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache {} indisponível na escrita: {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache {} indisponível ao remover: {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache {} indisponível ao limpar: {}", cache.getName(), e.getMessage());
            }
        };
    }
}
