package com.cestunmac.android.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class ImageDownloader {
    private static final int                           CACHE_SIZE           = 5;
    public static final int                            BITMAP_MAX_HEIGHT    = 480;
    public static final int                            BITMAP_MAX_WIDTH     = 320;
    public static final int                            THUMB_MAX_HEIGHT     = 200;

    public static final int                            FLAG_ROUNDED_CORNERS = 1;
    public static final int                            FLAG_GET_THUMBNAIL   = 1 << 1;
    public static final int                            FLAG_NO_THUMBNAIL    = 1 << 2;
    public static final int                            FLAG_LIMIT_WIDTH     = 1 << 3;

    private static final int                           MSG_LOAD_FROM_SD     = 0;
    private static final int                           MSG_DOWNLOAD         = 1;
    private static final int                           MSG_STOP             = 2;
    private static final String                        THUMB_FOLDER         = "/thumb";
    private static final String                        RESIZED_FOLDER       = "/resized";
    private static ImageDownloader                     instance;

    private LooperThread                               mDowloadLooper;
    private final ImageDownloaderCache<String, Bitmap> mImageLiveCache      = new ImageDownloaderCache<String, Bitmap>(CACHE_SIZE);
    private final Handler                              mUiHandler;

    private boolean                                    mStopped             = false;

    private ImageDownloader(Context ctx) {
        instance = this;
        mDowloadLooper = new LooperThread();
        mDowloadLooper.start();
        mUiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (mStopped) {
                    return;
                }
                ImageDownloadMessageData messageData = (ImageDownloadMessageData) msg.obj;
                ImageView v = messageData.viewRef.get();
                if (v != null && messageData.bitmap != null && messageData == getImageDownloadData(v)) {
                    v.setImageBitmap(messageData.bitmap);
                }
            };
        };

        // create cache dir if needed
        new File(ctx.getExternalCacheDir() + THUMB_FOLDER).mkdirs();
        new File(ctx.getExternalCacheDir() + RESIZED_FOLDER).mkdirs();
    }

    public static ImageDownloader getInstance(Context ctx) {
        if (instance == null) {
            instance = new ImageDownloader(ctx);
        }
        return instance;
    }

    public void download(String url, ImageView imageView, int flags) {
        if (mStopped || TextUtils.isEmpty(url)) {
            return;
        }

        // get image from cache
        Bitmap cachedBitmap = null;
        if ((flags & FLAG_GET_THUMBNAIL) != 0) {
            cachedBitmap = mImageLiveCache.get(url + THUMB_FOLDER);
        } else if ((flags & FLAG_LIMIT_WIDTH) != 0) {
            cachedBitmap = mImageLiveCache.get(url + RESIZED_FOLDER);
        } else {
            cachedBitmap = mImageLiveCache.get(url);
        }

        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
        } else {
            ImageDownloadMessageData messageData = new ImageDownloadMessageData(url, imageView);
            messageData.flags = flags;

            DownloadedDrawable downloadedDrawable = new DownloadedDrawable(messageData);
            imageView.setImageDrawable(downloadedDrawable);

            Message msg = new Message();
            msg.obj = messageData;

            // check if available from sd card
            if (isSDCacheReadable()) {
                File extCacheDir = imageView.getContext().getExternalCacheDir();
                File img = new File(extCacheDir, md5(url));
                if (img.exists()) {
                    msg.what = MSG_LOAD_FROM_SD;
                    mDowloadLooper.enqueueMessage(msg, true);
                    return;
                }
            }

            // load from web
            msg.what = MSG_DOWNLOAD;
            mDowloadLooper.enqueueMessage(msg, false);
        }
    }

    private boolean isSDCacheReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isSDCacheWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        } else {
            return false;
        }
    }

    private static ImageDownloadMessageData getImageDownloadData(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getImageDownloadData();
            }
        }
        return null;
    }

    public static void release() {
        instance.mStopped = true;
        instance.mDowloadLooper.stopLooper();
        for (Bitmap bmp : instance.mImageLiveCache.values()) {
            bmp.recycle();
        }
        instance.mImageLiveCache.clear();
        instance = null;
    }

    private static class ImageDownloadMessageData {
        public String                         url;
        public Bitmap                         bitmap;
        public final WeakReference<ImageView> viewRef;
        public int                            flags;

        public ImageDownloadMessageData(String url, ImageView view) {
            this.url = url;
            viewRef = new WeakReference<ImageView>(view);
        }
    }

    private class LooperThread extends Thread {
        public Handler mHandler;

        @Override
        public void run() {
            Looper.prepare();

            synchronized (instance) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == MSG_STOP) {
                            Looper.myLooper().quit();
                            return;
                        }
                        // separate from stop message to do the looper quit once
                        if (mStopped) {
                            return;
                        }
                        ImageDownloadMessageData messageData = (ImageDownloadMessageData) msg.obj;

                        boolean thumbMode = (messageData.flags & FLAG_GET_THUMBNAIL) != 0;
                        boolean limitWidthMode = (messageData.flags & FLAG_LIMIT_WIDTH) != 0;

                        // check cache in case the dowload has already be done
                        if (mImageLiveCache.containsKey(messageData.url)) {
                            messageData.bitmap = mImageLiveCache.get(messageData.url);
                        } else {
                            switch (msg.what) {
                                case MSG_LOAD_FROM_SD: {
                                    // read from SD
                                    View v = messageData.viewRef.get();
                                    if (v != null) {
                                        File cacheDir = v.getContext().getExternalCacheDir();
                                        String fileName = md5(messageData.url);
                                        File img;
                                        if (thumbMode) {
                                            img = new File(cacheDir + THUMB_FOLDER, fileName);
                                        } else if (limitWidthMode) {
                                            img = new File(cacheDir + RESIZED_FOLDER, fileName);
                                        } else {
                                            img = new File(cacheDir, fileName);
                                        }
                                        // messageData.bitmap = shrinkBitmap(img.getPath(), BITMAP_MAX_WIDTH, BITMAP_MAX_HEIGHT);
                                        messageData.bitmap = BitmapFactory.decodeFile(img.getPath());
                                    } else {
                                        // view has been garbaged
                                        return;
                                    }
                                    break;
                                }
                                case MSG_DOWNLOAD: {
                                    // download bitmap
                                    Bitmap bitmap = downloadBitmap(messageData.url);
                                    Bitmap thumbBmp = null;
                                    Bitmap limitWidthBmp = null;

                                    // save to SD cache
                                    if (bitmap != null && isSDCacheWritable()) {
                                        View v = messageData.viewRef.get();
                                        if (v != null) {
                                            File cacheDir = v.getContext().getExternalCacheDir();
                                            File img = new File(cacheDir, md5(messageData.url));
                                            File thumb = new File(cacheDir + THUMB_FOLDER, md5(messageData.url));
                                            File resized = new File(cacheDir + RESIZED_FOLDER, md5(messageData.url));

                                            try {
                                                // make rounded corners
                                                if ((messageData.flags & FLAG_ROUNDED_CORNERS) != 0) {
                                                    // Log.d("ImageDownloader.handleMessage():", "create rounded corners");
                                                    bitmap = getRoundedCornerBitmap(bitmap);
                                                }

                                                // save image to SD
                                                if (img.createNewFile()) {
                                                    OutputStream out = new FileOutputStream(img);
                                                    bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
                                                    out.flush();
                                                    out.close();
                                                }

                                                // make thumnail if needed
                                                if ((messageData.flags & FLAG_NO_THUMBNAIL) == 0) {
                                                    // create thumb
                                                    double thumb_ratio = (double) THUMB_MAX_HEIGHT / (double) bitmap.getHeight();
                                                    thumbBmp = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * thumb_ratio),
                                                                                         (int) (bitmap.getHeight() * thumb_ratio), true);

                                                    double limit_width_ratio = (double) BITMAP_MAX_WIDTH / (double) bitmap.getWidth();
                                                    limitWidthBmp = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * limit_width_ratio),
                                                                                              (int) (bitmap.getHeight() * limit_width_ratio), true);
                                                    // recycle image if not needed
                                                    if (thumbMode || limitWidthMode) {
                                                        bitmap.recycle();
                                                    }

                                                    // save thumb to SD
                                                    if (thumb.createNewFile()) {
                                                        OutputStream out = new FileOutputStream(thumb);
                                                        thumbBmp.compress(Bitmap.CompressFormat.PNG, 80, out);
                                                        out.flush();
                                                        out.close();
                                                    } else {
                                                        // Log.d("ImageDownloader.LooperThread.run():", "failed to create thumb");
                                                    }

                                                    // save resized img (limited width) to SD
                                                    if (resized.createNewFile()) {
                                                        OutputStream out = new FileOutputStream(resized);
                                                        limitWidthBmp.compress(Bitmap.CompressFormat.PNG, 80, out);
                                                        out.flush();
                                                        out.close();
                                                    } else {
                                                        // Log.d("ImageDownloader.LooperThread.run():", "failed to create thumb");
                                                    }

                                                    // recycle image if not needed
                                                    if (!thumbMode) {
                                                        thumbBmp.recycle();
                                                    }
                                                    
                                                    if (!limitWidthMode) {
                                                        limitWidthBmp.recycle();
                                                    }
                                                }
                                            } catch (FileNotFoundException e) {
                                                e.printStackTrace();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    // store bitmap
                                    if (thumbMode) {
                                        messageData.bitmap = thumbBmp;
                                    } else if (limitWidthMode) {
                                        messageData.bitmap = limitWidthBmp;
                                    } else {
                                        messageData.bitmap = bitmap;
                                    }

                                    break;
                                }

                                default:
                                    break;
                            }

                            if (messageData.bitmap == null) {
                                return;
                            }

                            // add to live cache
                            String key;
                            if (thumbMode) {
                                key = messageData.url + THUMB_FOLDER;
                            } else {
                                key = messageData.url;
                            }
                            mImageLiveCache.put(key, messageData.bitmap);
                        }

                        // send message to ui handler to refresh view
                        Message respMessage = new Message();
                        respMessage.obj = messageData;
                        mUiHandler.sendMessage(respMessage);
                    }
                };

                instance.notifyAll();
            }

            Looper.loop();
        }

        public void stopLooper() {
            Message msg = new Message();
            msg.what = MSG_STOP;
            mHandler.sendMessageAtFrontOfQueue(msg);
        }

        public void enqueueMessage(Message msg, boolean priorityMessage) {
            // for paranoia only, seem to be useless now...
            if (mHandler == null) {
                synchronized (instance) {
                    try {
                        instance.wait(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (priorityMessage) {
                mHandler.sendMessageAtFrontOfQueue(msg);
            } else {
                mHandler.sendMessage(msg);
            }
        }
        
    }

    public static Bitmap downloadBitmap(String url) {
        final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
                return null;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    // final Bitmap bitmap = shrinkBitmap(new FlushedInputStream(inputStream), BITMAP_MAX_WIDTH, BITMAP_MAX_HEIGHT);
                    final Bitmap bitmap = BitmapFactory.decodeStream(new FlushedInputStream(inputStream));
                    return bitmap;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (Exception e) {
            // Could provide a more explicit error message for IOException or IllegalStateException
            getRequest.abort();
            Log.w("ImageDownloader", "Error while retrieving bitmap from " + url, e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return null;
    }
    
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = 20;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();
        return output;
    }

    public static Bitmap shrinkBitmap(String file, int width, int height) {
        try {
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inJustDecodeBounds = true;
            bmpFactoryOptions.inPurgeable = true;
            bmpFactoryOptions.inInputShareable = true;
            Bitmap bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
            computeRatio(width, height, bmpFactoryOptions);
            bmpFactoryOptions.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
            return bitmap;
        } catch (OutOfMemoryError e) {
            Log.e("ImageDownloader.shrinkBitmap():", "memory !");
            return null;
        }
    }

    public static Bitmap shrinkBitmap(InputStream stream, int width, int height) {
        try {
            // TODO check for a better solution for handling purgation
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);
            computeRatio(width, height, bmpFactoryOptions);

            if (bitmap != null) {
                final int ratio = bmpFactoryOptions.inSampleSize;
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bmpFactoryOptions.outWidth / ratio, bmpFactoryOptions.outHeight / ratio, false);
                bitmap.recycle();
                return scaledBitmap;
            }
            return null;
        } catch (OutOfMemoryError e) {
            Log.e("ImageDownloader.shrinkBitmap():", "memory !");
            return null;
        }
    }

    private static void computeRatio(int width, int height, BitmapFactory.Options bmpFactoryOptions) {
        int heightRatio;
        int widthRatio;
        if (bmpFactoryOptions.outHeight > bmpFactoryOptions.outWidth) {
            heightRatio = (int) Math.floor(bmpFactoryOptions.outHeight / (float) width);
            widthRatio = (int) Math.floor(bmpFactoryOptions.outWidth / (float) height);
        } else {
            heightRatio = (int) Math.floor(bmpFactoryOptions.outHeight / (float) height);
            widthRatio = (int) Math.floor(bmpFactoryOptions.outWidth / (float) width);
        }

        if (heightRatio > 1 || widthRatio > 1) {
            if (heightRatio < widthRatio) {
                bmpFactoryOptions.inSampleSize = widthRatio;
            } else {
                bmpFactoryOptions.inSampleSize = heightRatio;
            }
        } else {
            bmpFactoryOptions.inSampleSize = 1;
        }
    }

    private static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<ImageDownloadMessageData> imageDownloadDataReference;

        public DownloadedDrawable(ImageDownloadMessageData imageDownloadData) {
            super(Color.TRANSPARENT);
            imageDownloadDataReference = new WeakReference<ImageDownloadMessageData>(imageDownloadData);
        }

        public ImageDownloadMessageData getImageDownloadData() {
            return imageDownloadDataReference.get();
        }
    }

    private static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int readByte = read();
                    if (readByte < 0) {
                        break; // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    public static String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class ImageDownloaderCache<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private final int         mCapacity;

        public ImageDownloaderCache(int capacity) {
            super(capacity);
            mCapacity = capacity;
        }

        @Override
        public V put(K key, V value) {
            if (size() > mCapacity - 1) {
                // remove oldest object
                final K oldest = keySet().iterator().next();
                V item = get(oldest);
                remove(oldest);
                onRemoveItem(item);
            }
            return super.put(key, value);
        }

        protected void onRemoveItem(V item) {

        }
    }
}
