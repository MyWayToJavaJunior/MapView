package io.github.kirillf.mapview.ImageLoader;

import android.graphics.Bitmap;

public interface ImageLoadingListener {
    void onLoadingComplete(String url, Bitmap bitmap);
    void onError(String url, Throwable t);
}
