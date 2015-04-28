package io.github.kirillf.mapview.mapview;

public class MapViewConfig {
    private int mapWidth;
    private int mapHeight;

    public MapViewConfig(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }
}
