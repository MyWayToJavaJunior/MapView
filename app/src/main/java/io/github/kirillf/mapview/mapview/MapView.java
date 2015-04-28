package io.github.kirillf.mapview.mapview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import io.github.kirillf.mapview.ImageLoader.ImageLoader;
import io.github.kirillf.mapview.cache.Cache;
import io.github.kirillf.mapview.layouts.ContentLayout;
import io.github.kirillf.mapview.tiles.TileFactory;
import io.github.kirillf.mapview.tiles.TileManager;
import io.github.kirillf.mapview.widgets.Scroller;

public class MapView extends ViewGroup {
    private static final String TAG = MapView.class.getSimpleName();

    private static final int MINIMUM_VELOCITY = 50;
    private static final int VELOCITY_UNITS = 1000;

    private Point touchDownPoint = new Point();
    private Point touchUpPoint = new Point();

    private Point scrollPosition = new Point();

    private Point actualPoint = new Point();

    private boolean isBeingFlung = false;

    private boolean shouldIntercept = false;

    private VelocityTracker velocity;

    private Scroller scroller;

    private int mapWidth;
    private int mapHeight;

    private ScrollActionHandler scrollActionHandler;
    private ContentLayout clip;

    private TileManager tileManager;

    public MapView(Context context, MapViewManager mapViewManager) {
        super(context);
        TileFactory tileFactory = mapViewManager.getTileFactory();
        ImageLoader imageLoader = mapViewManager.getImageLoader();
        Cache<String, Bitmap> cache = mapViewManager.getCacheManager();
        imageLoader.setCache(cache);
        tileManager = new TileManager(context, tileFactory, imageLoader);
        clip = new ContentLayout(context);
        addChild(tileManager);
        setWillNotDraw(false);


        scrollActionHandler = new ScrollActionHandler(this);

        scroller = new Scroller(context);

        super.addView(clip);

        mapWidth = mapViewManager.getMapViewConfig().getMapWidth();
        mapHeight = mapViewManager.getMapViewConfig().getMapHeight();

        setSize(mapWidth, mapHeight);
        updateClip();
        tileManager.configure(mapWidth, mapHeight);
    }

    public void requestRender() {
        tileManager.requestRender();
    }

    public boolean isFlinging() {
        return isBeingFlung;
    }

    private void clearView(View view) {
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            imageView.setImageBitmap(null);
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = viewGroup.getChildAt(i);
                clearView(child);
            }
            viewGroup.removeAllViews();
        }
    }

    public void clear() {
        tileManager.clear();
        clearView(this);
    }

    public void destroy() {
        tileManager.clear();
        clearView(this);
    }

    public void setShouldIntercept(boolean intercept) {
        shouldIntercept = intercept;
    }

    public void setSize(int wide, int tall) {
        mapWidth = wide;
        mapHeight = tall;
        updateClip();
    }

    public void addChild(View child) {
        LayoutParams lp = new LayoutParams(mapWidth, mapHeight);
        clip.addView(child, lp);
    }

    public void removeChild(View child) {
        if (clip.indexOfChild(child) > -1) {
            clip.removeView(child);
        }
    }

    private void updateClip() {
        updateViewClip(clip);
        for (int i = 0; i < clip.getChildCount(); i++) {
            View child = clip.getChildAt(i);
            updateViewClip(child);
        }
        constrainScroll();
    }

    private void constrainPoint(Point point) {
        int x = point.x;
        int y = point.y;
        int mx = Math.max(0, Math.min(x, getLimitX()));
        int my = Math.max(0, Math.min(y, getLimitY()));
        if (x != mx || y != my) {
            point.set(mx, my);
        }
    }

    public void scrollToPoint(Point point) {
        constrainPoint(point);
        int ox = getScrollX();
        int oy = getScrollY();
        int nx = point.x;
        int ny = point.y;
        scrollTo(nx, ny);
        if (ox != nx || oy != ny) {
            updateViewport();
        }
    }

    private void constrainScroll() { // TODO:
        Point currentScroll = new Point(getScrollX(), getScrollY());
        Point limitScroll = new Point(currentScroll);
        constrainPoint(limitScroll);
        if (!currentScroll.equals(limitScroll)) {
            scrollToPoint(currentScroll);
        }
    }

    private void updateViewClip(View v) {
        LayoutParams lp = v.getLayoutParams();
        lp.width = mapWidth;
        lp.height = mapHeight;
        v.setLayoutParams(lp);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        clip.layout(0, 0, clip.getMeasuredWidth(), clip.getMeasuredHeight());
        if (changed) {
            updateViewport();
            requestRender();
        }
    }

    private void updateViewport() {
        int left = getScrollX();
        int top = getScrollY();
        int right = left + getWidth();
        int bottom = top + getHeight();
        tileManager.updateViewport(left, top, right, bottom);
    }

    private void processEvent(MotionEvent event) {

        touchUpPoint.set(touchDownPoint.x, touchDownPoint.y);

        for (int i = 0; i < event.getPointerCount(); i++) {
            int id = event.getPointerId(i);
            int x = (int) event.getX(i);
            int y = (int) event.getY(i);
            switch (id) {
                case 0:
                    touchDownPoint.set(x, y);
                    actualPoint.set(x, y);
                    break;
                case 1:
                    actualPoint.set(x, y);
                    break;
            }
        }
        // record scroll position and adjust finger point to account for scroll offset
        scrollPosition.set(getScrollX(), getScrollY());
        actualPoint.offset(scrollPosition.x, scrollPosition.y);

        // update velocity for flinging
        // TODO: this can probably be moved to the ACTION_MOVE switch
        if (velocity == null) {
            velocity = VelocityTracker.obtain();
        }
        velocity.addMovement(event);
    }

    private int getLimitX() {
        return mapWidth - getWidth();
    }

    private int getLimitY() {
        return mapHeight - getHeight();
    }

    private void performDrag() {
        Point delta = new Point();
        delta.set(touchUpPoint.x, touchUpPoint.y);
        delta.offset(-touchDownPoint.x, -touchDownPoint.y);
        scrollPosition.offset(delta.x, delta.y);
        scrollToPoint(scrollPosition);
    }

    private boolean performFling() {
        velocity.computeCurrentVelocity(VELOCITY_UNITS);
        double xv = velocity.getXVelocity();
        double yv = velocity.getYVelocity();
        double totalVelocity = Math.abs(xv) + Math.abs(yv);
        if (totalVelocity > MINIMUM_VELOCITY) {
            scroller.fling(getScrollX(), getScrollY(), (int) -xv, (int) -yv, 0, getLimitX(), 0, getLimitY());
            invalidate();
            return true;
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int w = clip.getMeasuredWidth();
        int h = clip.getMeasuredHeight();
        w = Math.max(w, getSuggestedMinimumWidth());
        h = Math.max(h, getSuggestedMinimumHeight());
        w = resolveSize(w, widthMeasureSpec);
        h = resolveSize(h, heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        processEvent(event);
        if (shouldIntercept) {
            final int action = event.getAction() & MotionEvent.ACTION_MASK;
            if (action == MotionEvent.ACTION_MOVE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        processEvent(event);
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                isBeingFlung = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                performDrag();
                break;
            case MotionEvent.ACTION_UP:
                if (performFling()) {
                    isBeingFlung = true;
                }
                if (velocity != null) {
                    velocity.recycle();
                    velocity = null;
                }
                if (!isFlinging()) {
                    requestRender();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (!isFlinging()) {
                    requestRender();
                }
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            Point destination = new Point(scroller.getCurrX(), scroller.getCurrY());
            scrollToPoint(destination);
            dispatchScrollActionNotification();
        }
    }

    private void dispatchScrollActionNotification() {
        if (scrollActionHandler.hasMessages(0)) {
            scrollActionHandler.removeMessages(0);
        }
        scrollActionHandler.sendEmptyMessageDelayed(0, 100);
    }

    private void handleScrollerAction() {
        Point point = new Point();
        point.x = getScrollX();
        point.y = getScrollY();
        requestRender();
        if (isBeingFlung) {
            isBeingFlung = false;
            requestRender();
        }
    }

    private static class ScrollActionHandler extends Handler {
        private final WeakReference<MapView> reference;

        public ScrollActionHandler(MapView mapView) {
            super();
            reference = new WeakReference<>(mapView);
        }

        @Override
        public void handleMessage(Message msg) {
            MapView mapView = reference.get();
            if (mapView != null) {
                mapView.handleScrollerAction();
            }
        }
    }

}