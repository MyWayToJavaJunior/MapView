package io.github.kirillf.mapview.tiles;

import java.net.URL;

import io.github.kirillf.mapview.exception.InvalidUrlException;

public interface TileUrlProvider {
    URL getTileUrl(int x, int y, int zoom) throws InvalidUrlException;
}
