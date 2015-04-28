package io.github.kirillf.mapview.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class DiscCache implements Cache<String, Bitmap> {
    private static final String TAG = DiscCache.class.getName();

    private static final int COMPRESS_RATE = 60;

    private Context context;

    public DiscCache(Context context) {
        this.context = context;
    }

    @Override
    public void put(String key, Bitmap value) {
        String filename = String.valueOf(key.hashCode());
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fileOutputStream.write(bitmapToBytes(value));
            fileOutputStream.close();
            Log.i(TAG, "Put to disc cache");
        } catch (IOException e) {
            Log.e(TAG, "Unable to write to disc cache");
        }
    }

    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_RATE, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public Bitmap get(String key) {
        String filename = String.valueOf(key.hashCode());
        File file = new File(context.getFilesDir(), filename);
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                int size = fileInputStream.available();
                byte[] bytes = new byte[size];
                fileInputStream.read(bytes);
                Log.i(TAG, "Get from disc cache");
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } catch (IOException e) {
                Log.w(TAG, "Unable to read from disc cache");
            }
        }
        return null;
    }
}
