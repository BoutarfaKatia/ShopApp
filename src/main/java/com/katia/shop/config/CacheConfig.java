package com.katia.shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache c, Object key) {
                log.warn("Cache get failed. cache={}, key={}", c.getName(), key);
            }
            @Override public void handleCachePutError(RuntimeException e, Cache c, Object key, Object v) {
                log.warn("Cache put failed. cache={}, key={}", c.getName(), key);
            }
            @Override public void handleCacheEvictError(RuntimeException e, Cache c, Object key) {
                log.warn("Cache evict failed. cache={}, key={}", c.getName(), key);
            }
            @Override public void handleCacheClearError(RuntimeException e, Cache c) {
                log.warn("Cache clear failed. cache={}", c.getName());
            }
        };
    }
}