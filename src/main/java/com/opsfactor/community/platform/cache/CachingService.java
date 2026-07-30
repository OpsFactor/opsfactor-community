package com.opsfactor.community.platform.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Service utilitario para invalidacao explicita de caches Spring.
 */
@Service
public class CachingService {

    /**
     * CacheManager configurado pelo contexto Spring Community.
     */
    @Autowired
    private CacheManager cacheManager;

    public void evictSingleCacheValue(String cacheName, String cacheKey) {
        cacheManager.getCache(cacheName).evict(cacheKey);
    }

    public void evictAllCacheValues() {
        cacheManager.getCacheNames().stream()
                .forEach(x -> cacheManager.getCache(x).clear());
    }
    
}
