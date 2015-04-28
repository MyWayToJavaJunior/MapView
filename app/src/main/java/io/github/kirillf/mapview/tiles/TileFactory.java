package io.github.kirillf.mapview.tiles;

import android.util.Log;

import java.net.URL;

import io.github.kirillf.mapview.exception.InvalidUrlException;

public class TileFactory {
    private static final String TAG = TileFactory.class.getName();

    private TileUrlProvider tileUrlProvider;
    private int startX;
    private int startY;
    private int zoom;

    public TileFactory(TileUrlProvider tileUrlProvider, int startX, int startY, int zoom) {
        this.tileUrlProvider = tileUrlProvider;
        this.startX = startX;
        this.startY = startY;
        this.zoom = zoom;
    }

    public MapTile getTile(int row, int column, int width, int height) {
        MapTile mapTile = new MapTile(row, column, width, height);
        URL url;
        try {
            url = tileUrlProvider.getTileUrl(startX + column, startY + row, zoom);
            mapTile.setUrl(url.toString());
        } catch (InvalidUrlException e) {
            Log.w(TAG, e);
        }
        return mapTile;
    }

}