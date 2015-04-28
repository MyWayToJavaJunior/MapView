package io.github.kirillf.mapview.tiles;

import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;

public class MapTile {

    private int row;
    private int column;

    private int width;
    private int height;

    private int left;
    private int top;

    private String url;
    private ImageView imageView;

    public MapTile(int row, int column, int width, int height) {
        this.row = row;
        this.column = column;
        this.width = width;
        this.height = height;
        this.top = row * height;
        this.left = column * width;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void destroy() {
        if (imageView != null) {
            imageView.setImageBitmap(null);
            ViewParent parent = imageView.getParent();
            if (parent != null && parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(imageView);
            }
            imageView = null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MapTile)) {
            return false;
        }
        MapTile that = (MapTile) obj;
        return this.row == that.getRow() && this.column == that.getColumn();
    }
}
