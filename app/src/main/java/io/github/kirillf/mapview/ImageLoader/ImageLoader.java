package io.github.kirillf.mapview.ImageLoader;

import android.content.res.Resources;
import android.graphics.Bitmap;

import io.github.kirillf.mapview.cache.Cache;
import io.github.kirillf.mapview.tiles.MapTile;

public interface ImageLoader {
    void loadImage(MapTile mapTile,  ImageLoadingListener imageLoadingListener);
    void setCache(Cache<String, Bitmap> cache);
}
