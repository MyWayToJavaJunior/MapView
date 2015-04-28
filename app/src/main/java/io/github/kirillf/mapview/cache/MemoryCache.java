package io.github.kirillf.mapview.cache;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MemoryCache implements Cache<String, Bitmap> {
    private static final String TAG = MemoryCache.class.getName();
    private int capacity;
    private ConcurrentLinkedQueue<String> linkedQueue;
    private ConcurrentHashMap<String, Bitmap> map;

    public MemoryCache(int capacity) {
        this.capacity = capacity;
        this.linkedQueue = new ConcurrentLinkedQueue<>();
        this.map = new ConcurrentHashMap<>(capacity);
    }

    @Override
    public void put(String key, Bitmap value) {
        Log.i(TAG, "Put to memory cache");
        if (map.containsKey(key)) {
            linkedQueue.remove(key);
        }
        while (linkedQueue.size() >= capacity) {
            String k = linkedQueue.poll();
            if (map.containsKey(k)) {
                map.remove(k);
            }
        }
        linkedQueue.add(key);
        map.put(key, value);
    }

    @Override
    public Bitmap get(String key) {
        Log.i(TAG, "Get from memory cache");
        if (map.containsKey(key)) {
            linkedQueue.remove(key);
            linkedQueue.add(key);
        }
        return map.get(key);
    }
}
