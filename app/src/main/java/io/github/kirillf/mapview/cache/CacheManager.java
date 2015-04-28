package io.github.kirillf.mapview.cache;

import android.graphics.Bitmap;

public class CacheManager implements Cache<String, Bitmap> {
    private final Cache<String, Bitmap> memoryCache;
    private final Object discCacheLock = new Object();
    private Cache<String, Bitmap> discCache;

    public CacheManager(Cache<String, Bitmap> memoryCache, Cache<String, Bitmap> discCache) {
        this.memoryCache = memoryCache;
        this.discCache = discCache;
    }

    public CacheManager(Cache<String, Bitmap> memoryCache) {
        this(memoryCache, null);
    }

    @Override
    public void put(String key, Bitmap value) {
        if (memoryCache.get(key) == null) {
            memoryCache.put(key, value);
        }

        synchronized (discCacheLock) {
            if (discCache != null && discCache.get(key) == null) {
                discCache.put(key, value);
            }
        }
    }

    @Override
    public Bitmap get(String key) {
        Bitmap bitmap = memoryCache.get(key);
        if (bitmap == null) {
            synchronized (discCacheLock) {
                if (discCache != null) {
                    return discCache.get(key);
                }
            }
        }
        return bitmap;
    }
}
