package io.github.kirillf.mapview;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import io.github.kirillf.mapview.ImageLoader.HttpImageLoader;
import io.github.kirillf.mapview.ImageLoader.ImageLoader;
import io.github.kirillf.mapview.cache.CacheManager;
import io.github.kirillf.mapview.cache.DiscCache;
import io.github.kirillf.mapview.cache.MemoryCache;
import io.github.kirillf.mapview.http.HttpService;
import io.github.kirillf.mapview.mapview.MapView;
import io.github.kirillf.mapview.mapview.MapViewConfig;
import io.github.kirillf.mapview.mapview.MapViewManager;
import io.github.kirillf.mapview.tiles.OpenCycleMapTileProvider;
import io.github.kirillf.mapview.tiles.TileFactory;


public class MapViewActivity extends ActionBarActivity {
    private static final int MAP_SIZE_X = 256 * 100;
    private static final int MAP_SIZE_Y = 256 * 100;
    private static final int MEMORY_CACHE_SIZE = 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MapFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map_view, menu);
        return true;
    }

    public static class MapFragment extends Fragment {
        private MapView mapView;

        public MapFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            MapViewConfig mapViewConfig = new MapViewConfig(MAP_SIZE_X, MAP_SIZE_Y);
            CacheManager cacheManager = new CacheManager(new MemoryCache(MEMORY_CACHE_SIZE), new DiscCache(getActivity()));
            TileFactory tileFactory = new TileFactory(new OpenCycleMapTileProvider(), 9881, 5220, 14);
            ImageLoader imageLoader = new HttpImageLoader(getActivity(), new HttpService(), R.drawable.loading);
            MapViewManager mapViewManager = new MapViewManager.Builder()
                    .setCacheManager(cacheManager)
                    .setImageLoader(imageLoader)
                    .setMapViewConfig(mapViewConfig)
                    .setTileFactory(tileFactory)
                    .build();
            mapView = new MapView(getActivity(), mapViewManager);
            View rootView = mapView;

            mapView.setShouldIntercept(true);
            return rootView;
        }

        @Override
        public void onPause() {
            super.onPause();
            mapView.clear();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mapView.destroy();
        }
    }
}
