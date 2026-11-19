package com.cocos.llallire.functional;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Clears every cache before each functional test. The database changes of a test are rolled back, but cached
 * values would survive and leak into the next test.
 */
class ClearCachesExtension implements BeforeEachCallback {

    /**
     * Clears all cache regions of the application context.
     *
     * @param context JUnit context
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        CacheManager cacheManager = SpringExtension.getApplicationContext(context).getBean(CacheManager.class);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }
}
