package io.github.kirillf.mapview.tiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.github.kirillf.mapview.ImageLoader.ImageLoader;
import io.github.kirillf.mapview.ImageLoader.ImageLoadingListener;
import io.github.kirillf.mapview.layouts.ContentLayout;

public class TileManager extends ContentLayout implements ImageLoadingListener {
    private static final String TAG = TileManager.class.getSimpleName();

    private static final int DEFAULT_TILE_SIZE = 256;

    private static final int RENDER_FLAG = 1;
    private static final int RENDER_DELAY = 100;

    private LinkedList<MapTile> scheduledToRender = new LinkedList<>();
    private LinkedList<MapTile> alreadyRendered = new LinkedList<>();

    private List<ContentLayout> tileGroups = new ArrayList<>();

    private Rect viewport = new Rect();
    private int mapWidth;
    private int mapHeight;

    private int topPadding;
    private int bottomPadding;
    private int leftPadding;
    private int rightPadding;

    private Rect computedViewport = new Rect();

    private ContentLayout currentTileGroup;

    private boolean renderIsCancelled;

    private TileRenderHandler handler;
    private TileFactory tileFactory;
    private ImageLoader imageLoader;

    public TileManager(Context context, TileFactory tileFactory, ImageLoader imageLoader) {
        super(context);
        handler = new TileRenderHandler(this);
        this.tileFactory = tileFactory;
        this.imageLoader = imageLoader;
    }

    public void configure(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        updateTileSet();
    }

    public void setPadding(int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        this.leftPadding = leftPadding;
        this.topPadding = topPadding;
        this.rightPadding = rightPadding;
        this.bottomPadding = bottomPadding;
    }

    public void requestRender() {
        renderIsCancelled = false;
        if (handler.hasMessages(RENDER_FLAG)) {
            handler.removeMessages(RENDER_FLAG);
        }
        handler.sendEmptyMessageDelayed(RENDER_FLAG, RENDER_DELAY);
    }

    public void cancelRender() {
        renderIsCancelled = true;
    }

    public void updateTileSet() {
        currentTileGroup = getCurrentTileGroup();
        updateViewClip(currentTileGroup);
        currentTileGroup.setVisibility(View.VISIBLE);
        currentTileGroup.bringToFront();
    }

    public void clear() {
        cancelRender();
        for (MapTile m : scheduledToRender) {
            m.destroy();
        }
        scheduledToRender.clear();
        for (MapTile m : alreadyRendered) {
            m.destroy();
        }
        alreadyRendered.clear();
        for (ContentLayout tileGroup : tileGroups) {
            int totalChildren = tileGroup.getChildCount();
            for (int i = 0; i < totalChildren; i++) {
                View child = tileGroup.getChildAt(i);
                if (child instanceof ImageView) {
                    ImageView imageView = (ImageView) child;
                    imageView.setImageBitmap(null);
                }
            }
            tileGroup.removeAllViews();
        }
    }

    private ContentLayout getCurrentTileGroup() {
        ContentLayout tileGroup = new ContentLayout(getContext());
        tileGroups.add(tileGroup);
        addView(tileGroup);
        return tileGroup;
    }

    void renderTiles() {
        if (renderIsCancelled) {
            return;
        }
        processTiles();
    }

    private void updateViewClip(View view) {
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        view.setLayoutParams(lp);
    }

    public void updateViewport(int left, int top, int right, int bottom) {
        viewport.set(left, top, right, bottom);
        updateComputedViewport();
    }

    private void updateComputedViewport() {
        computedViewport.set(viewport);
        computedViewport.top -= topPadding;
        computedViewport.left -= leftPadding;
        computedViewport.bottom += bottomPadding;
        computedViewport.right += rightPadding;
    }

    public LinkedList<MapTile> getIntersections() {
        double offsetWidth = DEFAULT_TILE_SIZE;
        double offsetHeight = DEFAULT_TILE_SIZE;
        LinkedList<MapTile> intersections = new LinkedList<>();
        viewport.set(computedViewport);
        viewport.top = Math.max(viewport.top, 0);
        viewport.left = Math.max(viewport.left, 0);
        viewport.right = Math.min(viewport.right, mapWidth);
        viewport.bottom = Math.min(viewport.bottom, mapHeight);
        int startRow = (int) Math.floor(viewport.top / offsetHeight);
        int endRow = (int) Math.ceil(viewport.bottom / offsetHeight);
        int startColumn = (int) Math.floor(viewport.left / offsetWidth);
        int endColumn = (int) Math.ceil(viewport.right / offsetWidth);
        for (int row = startRow; row < endRow; row++) {
            for (int column = startColumn; column < endColumn; column++) {
                MapTile m = tileFactory.getTile(row, column, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE);
                intersections.add(m);
            }
        }
        return intersections;
    }

    private void processTiles() {
        LinkedList<MapTile> intersections = getIntersections();
        if (scheduledToRender.equals(intersections)) {
            return;
        }
        scheduledToRender = intersections;
        for (MapTile mapTile : intersections) {
            if (!alreadyRendered.contains(mapTile)) {
                ImageView imageView = new ImageView(getContext());
                alreadyRendered.add(mapTile);
                imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                mapTile.setImageView(imageView);
                imageLoader.loadImage(mapTile, this);
                LayoutParams l = getLayoutFromTile(mapTile);
                currentTileGroup.addView(imageView, l);
            }
        }
    }

    private ContentLayout.LayoutParams getLayoutFromTile(MapTile m) {
        int w = m.getWidth();
        int h = m.getHeight();
        int x = m.getLeft();
        int y = m.getTop();
        return new ContentLayout.LayoutParams(w, h, x, y);
    }

    private void cleanup() {
        LinkedList<MapTile> condemned = new LinkedList<>(alreadyRendered);
        condemned.removeAll(scheduledToRender);
        for (MapTile m : condemned) {
            m.destroy();
            alreadyRendered.remove(m);
        }
        for (ContentLayout tileGroup : tileGroups) {
            if (currentTileGroup == tileGroup) {
                continue;
            }
            tileGroup.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoadingComplete(String url, Bitmap bitmap) {
        Log.i(TAG, "Image loading completed");
        cleanup();
        requestRender();
    }

    @Override
    public void onError(String url, Throwable t) {
        Log.e(TAG, "Image loading failed");
    }
}