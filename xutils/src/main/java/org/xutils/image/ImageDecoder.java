package org.xutils.image;

import android.backport.webp.WebPFactory;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.DiskCacheFile;
import org.xutils.cache.LruDiskCache;
import org.xutils.common.Callback;
import org.xutils.common.task.PriorityExecutor;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.x;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wyouflf on 15/10/9.
 * ImageDecoder for ImageLoader
 */
public final class ImageDecoder {

    private final static int BITMAP_DECODE_MAX_WORKER;
    private final static AtomicInteger bitmapDecodeWorker = new AtomicInteger(0);
    private final static Object bitmapDecodeLock = new Object();

    private final static Object gifDecodeLock = new Object();
    private final static byte[] GIF_HEADER = new byte[]{'G', 'I', 'F'};
    private final static byte[] WEBP_HEADER = new byte[]{'W', 'E', 'B', 'P'};

    private final static Executor THUMB_CACHE_EXECUTOR = new PriorityExecutor(1);
    private final static LruDiskCache THUMB_CACHE = LruDiskCache.getDiskCache("xUtils_img_thumb");

    static {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        BITMAP_DECODE_MAX_WORKER = cpuCount > 4 ? 2 : 1;
    }

    private ImageDecoder() {
    }

    /*package*/
    static void clearCacheFiles() {
        THUMB_CACHE.clearCacheFiles();
    }

    /**
     * decode image file for ImageLoader
     *
     * @param file
     * @param options
     * @param cancelable
     * @return
     * @throws IOException
     */
    /*package*/
    static Drawable decodeFileWithLock(final File file,
                                       final ImageOptions options,
                                       final Callback.Cancelable cancelable) throws IOException {
        if (file == null || !file.exists() || file.length() < 1) return null;
        if (cancelable != null && cancelable.isCancelled()) {
            return null;
        }

        Drawable result = null;
        if (!options.isIgnoreGif() && isGif(file)) {
            Movie movie = null;
            synchronized (gifDecodeLock) { // decode with lock
                movie = decodeGif(file, options, cancelable);
            }
            if (movie != null) {
                result = new ReusableGifDrawable(movie, (int) file.length());
            }
        } else {
            Bitmap bitmap = null;
            { // decode with lock
                while (bitmapDecodeWorker.get() >= BITMAP_DECODE_MAX_WORKER
                        && (cancelable == null || !cancelable.isCancelled())) {
                    synchronized (bitmapDecodeLock) {
                        try {
                            bitmapDecodeLock.wait();
                        } catch (Throwable ignored) {
                        }
                    }
                }
                if (cancelable != null && cancelable.isCancelled()) {
                    return null;
                }
                try {
                    bitmapDecodeWorker.incrementAndGet();
                    // get from thumb cache
                    if (options.isCompress()) {
                        bitmap = getThumbCache(file, options);
                    }
                    if (bitmap == null) {
                        bitmap = decodeBitmap(file, options, cancelable);
                    }
                } finally {
                    bitmapDecodeWorker.decrementAndGet();
                    synchronized (bitmapDecodeLock) {
                        bitmapDecodeLock.notifyAll();
                    }
                }
            }
            if (bitmap != null) {
                result = new ReusableBitmapDrawable(x.app().getResources(), bitmap);
                // save to thumb cache
                if (options.isCompress()) {
                    final Bitmap finalBitmap = bitmap;
                    THUMB_CACHE_EXECUTOR.execute(new Runnable() {
                        @Override
                        public void run() {
                            saveThumbCache(file, options, finalBitmap);
                        }
                    });
                }
            }
        }
        return result;
    }

    public static boolean isGif(File file) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] header = IOUtil.readBytes(in, 0, 3);
            return Arrays.equals(GIF_HEADER, header);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        } finally {
            IOUtil.closeQuietly(in);
        }

        return false;
    }

    public static boolean isWebP(File file) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] header = IOUtil.readBytes(in, 8, 4);
            return Arrays.equals(WEBP_HEADER, header);
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        } finally {
            IOUtil.closeQuietly(in);
        }

        return false;
    }

    public static Bitmap decodeBitmap(final File file, final ImageOptions options, Callback.Cancelable cancelable) throws IOException {
        Bitmap result = null;
        try {
            if (cancelable != null && cancelable.isCancelled()) {
                return null;
            }

            // prepare bitmap options
            final BitmapFactory.Options bitmapOps = new BitmapFactory.Options();
            bitmapOps.inJustDecodeBounds = true;
            bitmapOps.inPurgeable = true;
            bitmapOps.inInputShareable = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOps);
            bitmapOps.inSampleSize = calculateSampleSize(
                    bitmapOps.outWidth, bitmapOps.outHeight,
                    options.getMaxWidth(), options.getMaxHeight());
            bitmapOps.inJustDecodeBounds = false;
            bitmapOps.inPreferredConfig = options.getConfig();
            int optionWith = options.getWidth();
            int optionHeight = options.getHeight();
            if (!options.isCrop() && optionWith > 0 && optionHeight > 0) {
                bitmapOps.outWidth = optionWith;
                bitmapOps.outHeight = optionHeight;
            }

            if (cancelable != null && cancelable.isCancelled()) {
                return null;
            }

            // decode file
            Bitmap bitmap = null;
            if (isWebP(file)) {
                bitmap = WebPFactory.nativeDecodeFile(file.getAbsolutePath(), bitmapOps);
            }
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOps);
            }
            if (bitmap == null) {
                throw new IOException("decode image error");
            }

            { // 旋转和缩放处理
                if (cancelable != null && cancelable.isCancelled()) {
                    return null;
                }
                if (options.isAutoRotate()) {
                    bitmap = rotate(bitmap, getRotateAngle(file.getAbsolutePath()));
                }
                if (cancelable != null && cancelable.isCancelled()) {
                    return null;
                }
                if (options.isCrop() && optionWith > 0 && optionHeight > 0) {
                    bitmap = createCenterCropScaledBitmap(bitmap, optionWith, optionHeight);
                }
            }

            if (bitmap == null) {
                throw new IOException("decode image error");
            }

            { // 圆角和方块处理
                if (cancelable != null && cancelable.isCancelled()) {
                    return null;
                }
                if (options.isCircular()) {
                    bitmap = createCircularImage(bitmap);
                } else if (options.getRadius() > 0) {
                    bitmap = createRoundCornerImage(bitmap, options.getRadius(), options.isSquare());
                } else if (options.isSquare()) {
                    bitmap = createSquareImage(bitmap);
                }
            }

            if (bitmap == null) {
                throw new IOException("decode image error");
            }

            result = bitmap;
        } catch (IOException ex) {
            throw ex;
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
            result = null;
        }

        return result;
    }

    public static Movie decodeGif(File file, ImageOptions options, Callback.Cancelable cancelable) throws IOException {
        InputStream in = null;
        try {
            if (cancelable != null && cancelable.isCancelled()) {
                return null;
            }
            in = new BufferedInputStream(new FileInputStream(file));
            Movie movie = Movie.decodeStream(in);
            if (movie == null) {
                throw new IOException("decode image error");
            }
            return movie;
        } catch (IOException ex) {
            throw ex;
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
            return null;
        } finally {
            IOUtil.closeQuietly(in);
        }
    }

    public static int calculateSampleSize(final int rawWidth, final int rawHeight,
                                          final int maxWidth, final int maxHeight) {
        int sampleSize = 1;

        if (rawWidth > maxWidth || rawHeight > maxHeight) {
            if (rawWidth > rawHeight) {
                sampleSize = Math.round((float) rawHeight / (float) maxHeight);
            } else {
                sampleSize = Math.round((float) rawWidth / (float) maxWidth);
            }

            final float totalPixels = rawWidth * rawHeight;

            final float maxTotalPixels = maxWidth * maxHeight * 2;

            while (totalPixels / (sampleSize * sampleSize) > maxTotalPixels) {
                sampleSize++;
            }
        }
        return sampleSize;
    }

    public static Bitmap createSquareImage(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width == height) {
            return source;
        }

        int squareWith = Math.min(width, height);
        Bitmap result = Bitmap.createBitmap(source, (width - squareWith) / 2,
                (height - squareWith) / 2, squareWith, squareWith);
        if (result != null) {
            if (result != source) {
                source.recycle();
                source = null;
            }
        } else {
            result = source;
        }
        return result;
    }

    public static Bitmap createCircularImage(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int diameter = Math.min(width, height);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap result = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        if (result != null) {
            Canvas canvas = new Canvas(result);
            canvas.drawCircle(diameter / 2, diameter / 2, diameter / 2, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(source, (diameter - width) / 2, (diameter - height) / 2, paint);
            source.recycle();
            source = null;
        } else {
            result = source;
        }
        return result;
    }

    public static Bitmap createRoundCornerImage(Bitmap source, int radius, boolean isSquare) {
        if (radius <= 0) return source;

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int targetWidth = sourceWidth;
        int targetHeight = sourceHeight;
        if (isSquare) {
            targetWidth = targetHeight = Math.min(sourceWidth, sourceHeight);
        }

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        if (result != null) {
            Canvas canvas = new Canvas(result);
            RectF rect = new RectF(0, 0, targetWidth, targetHeight);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(source,
                    (targetWidth - sourceWidth) / 2, (targetHeight - sourceHeight) / 2, paint);
            source.recycle();
            source = null;
        } else {
            result = source;
        }
        return result;
    }

    public static Bitmap createCenterCropScaledBitmap(Bitmap source, int dstWidth, int dstHeight) {
        final int width = source.getWidth();
        final int height = source.getHeight();
        if (width == dstWidth && height == dstHeight) {
            return source;
        }

        // scale
        Matrix m = new Matrix();
        int l = 0, t = 0, r = width, b = height;
        {
            float sx = dstWidth / (float) width;
            float sy = dstHeight / (float) height;

            if (sx > sy) {
                sy = sx;
                l = 0;
                r = width;
                t = (int) ((height - dstHeight / sx) / 2);
                b = (int) ((height + dstHeight / sx) / 2);
            } else {
                sx = sy;
                l = (int) ((width - dstWidth / sx) / 2);
                r = (int) ((width + dstWidth / sx) / 2);
                t = 0;
                b = height;
            }
            m.setScale(sx, sy);
        }

        Bitmap result = Bitmap.createBitmap(source, l, t, r - l, b - t, m, true);

        if (result != null) {
            if (result != source) {
                source.recycle();
                source = null;
            }
        } else {
            result = source;
        }
        return result;
    }

    public static Bitmap rotate(Bitmap source, int angle) {
        Bitmap result = null;

        if (angle != 0) {

            Matrix m = new Matrix();
            m.setRotate(angle);
            try {
                result = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), m, true);
            } catch (Throwable ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
        }

        if (result != null) {
            if (result != source) {
                source.recycle();
                source = null;
            }
        } else {
            result = source;
        }
        return result;
    }

    public static int getRotateAngle(String filePath) {
        int angle = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    angle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;
                default:
                    angle = 0;
                    break;
            }
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
        return angle;
    }

    /**
     * 压缩bitmap, 更好的支持webp.
     *
     * @param bitmap
     * @param format
     * @param quality
     * @param out
     * @throws IOException
     */
    public static void compress(Bitmap bitmap, Bitmap.CompressFormat format, int quality, OutputStream out) throws IOException {
        if (format == Bitmap.CompressFormat.WEBP) {
            byte[] data = WebPFactory.nativeEncodeBitmap(bitmap, quality);
            out.write(data);
        } else {
            bitmap.compress(format, quality, out);
        }
    }

    /**
     * 根据文件的修改时间和图片的属性保存缩略图
     *
     * @param file
     * @param options
     * @param thumbBitmap
     */
    private static void saveThumbCache(File file, ImageOptions options, Bitmap thumbBitmap) {
        DiskCacheEntity entity = new DiskCacheEntity();
        entity.setKey(
                file.getAbsolutePath() + "@" + file.lastModified() + options.toString());
        DiskCacheFile cacheFile = null;
        OutputStream out = null;
        try {
            cacheFile = THUMB_CACHE.createDiskCacheFile(entity);
            if (cacheFile != null) {
                out = new FileOutputStream(cacheFile);
                byte[] encoded = WebPFactory.nativeEncodeBitmap(thumbBitmap, 80);
                out.write(encoded);
                out.flush();
                cacheFile = cacheFile.commit();
            }
        } catch (Throwable ex) {
            IOUtil.deleteFileOrDir(cacheFile);
            LogUtil.w(ex.getMessage(), ex);
        } finally {
            IOUtil.closeQuietly(cacheFile);
            IOUtil.closeQuietly(out);
        }
    }

    /**
     * 根据文件的修改时间和图片的属性获取缩略图
     *
     * @param file
     * @param options
     * @return
     */
    private static Bitmap getThumbCache(File file, ImageOptions options) {
        DiskCacheFile cacheFile = null;
        try {
            cacheFile = THUMB_CACHE.getDiskCacheFile(
                    file.getAbsolutePath() + "@" + file.lastModified() + options.toString());
            if (cacheFile != null && cacheFile.exists()) {
                BitmapFactory.Options bitmapOps = new BitmapFactory.Options();
                bitmapOps.inJustDecodeBounds = false;
                bitmapOps.inPurgeable = true;
                bitmapOps.inInputShareable = true;
                bitmapOps.inPreferredConfig = Bitmap.Config.ARGB_8888;
                return WebPFactory.nativeDecodeFile(cacheFile.getAbsolutePath(), bitmapOps);
            }
        } catch (Throwable ex) {
            LogUtil.w(ex.getMessage(), ex);
        } finally {
            IOUtil.closeQuietly(cacheFile);
        }
        return null;
    }
}
