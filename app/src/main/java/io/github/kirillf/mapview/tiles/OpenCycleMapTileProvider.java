package io.github.kirillf.mapview.tiles;

import java.net.MalformedURLException;
import java.net.URL;

import io.github.kirillf.mapview.exception.InvalidUrlException;


public class OpenCycleMapTileProvider implements TileUrlProvider {

    @Override
    public URL getTileUrl(int x, int y, int zoom) throws InvalidUrlException {
        String pattern = "http://b.tile.opencyclemap.org/cycle/%d/%d/%d.png";
        String url = String.format(pattern, zoom, x, y);
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new InvalidUrlException(e.toString());
        }
    }
}
