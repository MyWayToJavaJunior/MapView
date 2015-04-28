package io.github.kirillf.mapview.mapview;

import io.github.kirillf.mapview.ImageLoader.ImageLoader;
import io.github.kirillf.mapview.cache.CacheManager;
import io.github.kirillf.mapview.tiles.TileFactory;

public class MapViewManager {
    private Builder builder;

    private MapViewManager(Builder builder) {
        this.builder = builder;
    }

    public MapViewConfig getMapViewConfig() {
        return builder.mapViewConfig;
    }

    public CacheManager getCacheManager() {
        return builder.cacheManager;
    }

    public TileFactory getTileFactory() {
        return builder.tileFactory;
    }

    public ImageLoader getImageLoader() {
        return builder.imageLoader;
    }

    public static class Builder {
        private MapViewConfig mapViewConfig;
        private CacheManager cacheManager;
        private TileFactory tileFactory;
        private ImageLoader imageLoader;

        public Builder setMapViewConfig(MapViewConfig mapViewConfig) {
            this.mapViewConfig = mapViewConfig;
            return this;
        }

        public Builder setCacheManager(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
            return this;
        }

        public Builder setTileFactory(TileFactory tileFactory) {
            this.tileFactory = tileFactory;
            return this;
        }

        public Builder setImageLoader(ImageLoader imageLoader) {
            this.imageLoader = imageLoader;
            return this;
        }

        public MapViewManager build() {
            return new MapViewManager(this);
        }
    }

}
