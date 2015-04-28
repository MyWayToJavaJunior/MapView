package io.github.kirillf.mapview.ImageLoader;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.kirillf.mapview.R;
import io.github.kirillf.mapview.cache.Cache;
import io.github.kirillf.mapview.exception.ImageLoadingException;
import io.github.kirillf.mapview.http.HttpRequest;
import io.github.kirillf.mapview.http.HttpResponse;
import io.github.kirillf.mapview.http.HttpService;
import io.github.kirillf.mapview.tiles.MapTile;

public class HttpImageLoader implements ImageLoader {
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static BitmapFactory.Options OPTIONS = new BitmapFactory.Options();
    private static ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);
    private HttpService httpService;
    private Cache<String, Bitmap> cache;
    private Bitmap loadingBitmap;
    private Context context;

    static {
        OPTIONS.inPreferredConfig = Bitmap.Config.RGB_565;
    }

    public HttpImageLoader(Context context, HttpService service, int loadingResId) {
        this.httpService = service;
        this.context = context;
        loadingBitmap = BitmapFactory.decodeResource(context.getResources(), loadingResId);
    }

    @Override
    public void loadImage(MapTile mapTile, ImageLoadingListener imageLoadingListener) {
        if (cancelPotentialWork(mapTile.getUrl(), mapTile.getImageView())) {
            ImageLoaderTask imageLoaderTask = new ImageLoaderTask(mapTile, imageLoadingListener);
            AsyncDrawable asyncDrawable = new AsyncDrawable(context.getResources(), loadingBitmap, imageLoaderTask);
            mapTile.getImageView().setImageDrawable(asyncDrawable);
            imageLoaderTask.executeOnExecutor(executorService);
        }
    }

    @Override
    public void setCache(Cache<String, Bitmap> cache) {
        this.cache = cache;
    }

    private Bitmap decodeBitmap(byte[] bytes) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, OPTIONS);
    }

    private class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<ImageLoaderTask> imageLoaderTaskWeakReference;

        public AsyncDrawable(Resources resources, Bitmap bitmap, ImageLoaderTask imageLoaderTask) {
            super(resources, bitmap);
            imageLoaderTaskWeakReference = new WeakReference<>(imageLoaderTask);
        }

        public ImageLoaderTask getImageLoadingTask() {
            return imageLoaderTaskWeakReference.get();
        }
    }

    public static boolean cancelPotentialWork(String url, ImageView imageView) {
        final ImageLoaderTask imageLoaderTask = getImageLoaderTask(imageView);

        if (imageLoaderTask != null) {
            String taskUrl = imageLoaderTask.mapTile.getUrl();
            if (taskUrl.equals(url)) {
                imageLoaderTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    private static ImageLoaderTask getImageLoaderTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getImageLoadingTask();
            }
        }
        return null;
    }

    private class ImageLoaderTask extends AsyncTask<Void, Void, Bitmap> {
        private ImageLoadingListener imageLoadingListener;
        private MapTile mapTile;
        private WeakReference<ImageView> imageViewWeakReference;

        public ImageLoaderTask(MapTile mapTile, ImageLoadingListener imageLoadingListener) {
            this.mapTile = mapTile;
            this.imageLoadingListener = imageLoadingListener;
            imageViewWeakReference = new WeakReference<>(mapTile.getImageView());
        }


        @Override
        protected Bitmap doInBackground(Void... params) {
            if (cache != null) {
                Bitmap bitmap = cache.get(mapTile.getUrl());
                if (bitmap != null) {
                    return bitmap;
                }
            }
            HttpRequest httpRequest = new HttpRequest(mapTile.getUrl(), HttpRequest.Method.GET);
            HttpResponse httpResponse = httpService.doRequest(httpRequest);
            if (httpResponse != null && httpResponse.getResponseCode() == 200) {
                byte[] content = httpResponse.getContent();
                if (content != null) {
                    Bitmap bitmap = decodeBitmap(content);
                    cache.put(mapTile.getUrl(), bitmap);
                    return bitmap;
                }
            }
            imageLoadingListener.onError(mapTile.getUrl(), new ImageLoadingException("Unable to process bitmap"));
            return null;
        }

        @Override
        public void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }
            if (imageViewWeakReference != null && bitmap != null) {
                ImageView imageView = imageViewWeakReference.get();
                ImageLoaderTask imageLoaderTask = getImageLoaderTask(imageView);
                if (this == imageLoaderTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                    imageLoadingListener.onLoadingComplete(mapTile.getUrl(), bitmap);
                }
            }
        }
    }
}
