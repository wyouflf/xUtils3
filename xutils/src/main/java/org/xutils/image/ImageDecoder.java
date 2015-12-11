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
            throw new Callback.CancelledException("cancelled during decode image");
        }

        Drawable result = null;
        if (!options.isIgnoreGif() && isGif(file)) {
            Movie movie = null;
            synchronized (gifDecodeLock) { // decode with lock
                movie = decodeGif(file, options, cancelable);
            }
            if (movie != null) {
                result = new GifDrawable(movie, (int) file.length());
            }
        } else {
            Bitmap bitmap = null;
            { // decode with lock
                try {
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
                        throw new Callback.CancelledException("cancelled during decode image");
                    }

                    bitmapDecodeWorker.incrementAndGet();
                    // get from thumb cache
                    if (options.isCompress()) {
                        bitmap = getThumbCache(file, options);
                    }
                    if (bitmap == null) {
                        bitmap = decodeBitmap(file, options, cancelable);
                        // save to thumb cache
                        if (bitmap != null && options.isCompress()) {
                            final Bitmap finalBitmap = bitmap;
                            THUMB_CACHE_EXECUTOR.execute(new Runnable() {
                                @Override
                                public void run() {
                                    saveThumbCache(file, options, finalBitmap);
                                }
                            });
                        }
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

    /**
     * 转化文件为Bitmap, 更好的支持WEBP.
     *
     * @param file
     * @param options
     * @param cancelable
     * @return
     * @throws IOException
     */
    public static Bitmap decodeBitmap(File file, ImageOptions options, Callback.Cancelable cancelable) throws IOException {
        {// check params
            if (file == null || !file.exists() || file.length() < 1) return null;
            if (options == null) {
                options = ImageOptions.DEFAULT;
            }
            if (options.getMaxWidth() <= 0 || options.getMaxHeight() <= 0) {
                options.optimizeMaxSize(null);
            }
        }

        Bitmap result = null;
        try {
            if (cancelable != null && cancelable.isCancelled()) {
                throw new Callback.CancelledException("cancelled during decode image");
            }

            // prepare bitmap options
            final BitmapFactory.Options bitmapOps = new BitmapFactory.Options();
            bitmapOps.inJustDecodeBounds = true;
            bitmapOps.inPurgeable = true;
            bitmapOps.inInputShareable = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOps);
            bitmapOps.inJustDecodeBounds = false;
            bitmapOps.inPreferredConfig = options.getConfig();
            int rotateAngle = 0;
            int rawWidth = bitmapOps.outWidth;
            int rawHeight = bitmapOps.outHeight;
            int optionWith = options.getWidth();
            int optionHeight = options.getHeight();
            if (options.isAutoRotate()) {
                rotateAngle = getRotateAngle(file.getAbsolutePath());
                if ((rotateAngle / 90) % 2 == 1) {
                    rawWidth = bitmapOps.outHeight;
                    rawHeight = bitmapOps.outWidth;
                }
            }
            if (!options.isCrop() && optionWith > 0 && optionHeight > 0) {
                if ((rotateAngle / 90) % 2 == 1) {
                    bitmapOps.outWidth = optionHeight;
                    bitmapOps.outHeight = optionWith;
                } else {
                    bitmapOps.outWidth = optionWith;
                    bitmapOps.outHeight = optionHeight;
                }
            }
            bitmapOps.inSampleSize = calculateSampleSize(
                    rawWidth, rawHeight,
                    options.getMaxWidth(), options.getMaxHeight());

            if (cancelable != null && cancelable.isCancelled()) {
                throw new Callback.CancelledException("cancelled during decode image");
            }

            // decode file
            Bitmap bitmap = null;
            if (isWebP(file)) {
                bitmap = WebPFactory.decodeFile(file.getAbsolutePath(), bitmapOps);
            }
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOps);
            }
            if (bitmap == null) {
                throw new IOException("decode image error");
            }

            { // 旋转和缩放处理
                if (cancelable != null && cancelable.isCancelled()) {
                    throw new Callback.CancelledException("cancelled during decode image");
                }
                if (rotateAngle != 0) {
                    bitmap = rotate(bitmap, rotateAngle, true);
                }
                if (cancelable != null && cancelable.isCancelled()) {
                    throw new Callback.CancelledException("cancelled during decode image");
                }
                if (options.isCrop() && optionWith > 0 && optionHeight > 0) {
                    bitmap = cut2ScaleSize(bitmap, optionWith, optionHeight, true);
                }
            }

            if (bitmap == null) {
                throw new IOException("decode image error");
            }

            { // 圆角和方块处理
                if (cancelable != null && cancelable.isCancelled()) {
                    throw new Callback.CancelledException("cancelled during decode image");
                }
                if (options.isCircular()) {
                    bitmap = cut2Circular(bitmap, true);
                } else if (options.getRadius() > 0) {
                    bitmap = cut2RoundCorner(bitmap, options.getRadius(), options.isSquare(), true);
                } else if (options.isSquare()) {
                    bitmap = cut2Square(bitmap, true);
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

    /**
     * 转换文件为Movie, 可用于创建GifDrawable.
     *
     * @param file
     * @param options
     * @param cancelable
     * @return
     * @throws IOException
     */
    public static Movie decodeGif(File file, ImageOptions options, Callback.Cancelable cancelable) throws IOException {
        {// check params
            if (file == null || !file.exists() || file.length() < 1) return null;
            /*if (options == null) {
                options = ImageOptions.DEFAULT; // not use
            }
            if (options.getMaxWidth() <= 0 || options.getMaxHeight() <= 0) {
                options.optimizeMaxSize(null);
            }*/
        }

        InputStream in = null;
        try {
            if (cancelable != null && cancelable.isCancelled()) {
                throw new Callback.CancelledException("cancelled during decode image");
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

    /**
     * 计算压缩采样倍数
     *
     * @param rawWidth
     * @param rawHeight
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static int calculateSampleSize(final int rawWidth, final int rawHeight,
                                          final int maxWidth, final int maxHeight) {
        int sampleSize = 1;

        if (rawWidth > maxWidth || rawHeight > maxHeight) {
            if (rawWidth > rawHeight) {
                sampleSize = Math.round((float) rawHeight / (float) maxHeight);
            } else {
                sampleSize = Math.round((float) rawWidth / (float) maxWidth);
            }

            if (sampleSize < 1) {
                sampleSize = 1;
            }

            final float totalPixels = rawWidth * rawHeight;

            final float maxTotalPixels = maxWidth * maxHeight * 2;

            while (totalPixels / (sampleSize * sampleSize) > maxTotalPixels) {
                sampleSize++;
            }
        }
        return sampleSize;
    }

    /**
     * 裁剪方形图片
     *
     * @param source
     * @param recycleSource 裁剪成功后销毁原图
     * @return
     */
    public static Bitmap cut2Square(Bitmap source, boolean recycleSource) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width == height) {
            return source;
        }

        int squareWith = Math.min(width, height);
        Bitmap result = Bitmap.createBitmap(source, (width - squareWith) / 2,
                (height - squareWith) / 2, squareWith, squareWith);
        if (result != null) {
            if (recycleSource && result != source) {
                source.recycle();
                source = null;
            }
        } else {
            result = source;
        }
        return result;
    }

    /**
     * 裁剪圆形图片
     *
     * @param source
     * @param recycleSource 裁剪成功后销毁原图
     * @return
     */
    public static Bitmap cut2Circular(Bitmap source, boolean recycleSource) {
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
            if (recycleSource) {
                source.recycle();
                source = null;
            }
        } else {
            result = source;
        }
        return result;
    }

    /**
     * 裁剪圆角
     *
     * @param source
     * @param radius
     * @param isSquare
     * @param recycleSource 裁剪成功后销毁原图
     * @return
     */
    public static Bitmap cut2RoundCorner(Bitmap source, int radius, boolean isSquare, boolean recycleSource) {
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
            if (recycleSource) {
                source.recycle();
                source = null;
            }
        } else {
            result = source;
        }
        return result;
    }

    /**
     * 裁剪并缩放至指定大小
     *
     * @param source
     * @param dstWidth
     * @param dstHeight
     * @param recycleSource 裁剪成功后销毁原图
     * @return
     */
    public static Bitmap cut2ScaleSize(Bitmap source, int dstWidth, int dstHeight, boolean recycleSource) {
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
            if (recycleSource && result != source) {
                source.recycle();
                source = null;
            }
        } else {
            result = source;
        }
        return result;
    }

    /**
     * 旋转图片
     *
     * @param source
     * @param angle
     * @param recycleSource
     * @return
     */
    public static Bitmap rotate(Bitmap source, int angle, boolean recycleSource) {
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
            if (recycleSource && result != source) {
                source.recycle();
                source = null;
            }
        } else {
            result = source;
        }
        return result;
    }

    /**
     * 获取图片旋转角度
     *
     * @param filePath
     * @return
     */
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
            byte[] data = WebPFactory.encodeBitmap(bitmap, quality);
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
        if (!WebPFactory.available()) return;

        DiskCacheEntity entity = new DiskCacheEntity();
        entity.setKey(
                file.getAbsolutePath() + "@" + file.lastModified() + options.toString());
        DiskCacheFile cacheFile = null;
        OutputStream out = null;
        try {
            cacheFile = THUMB_CACHE.createDiskCacheFile(entity);
            if (cacheFile != null) {
                out = new FileOutputStream(cacheFile);
                byte[] encoded = WebPFactory.encodeBitmap(thumbBitmap, 80);
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
        if (!WebPFactory.available()) return null;

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
                return WebPFactory.decodeFile(cacheFile.getAbsolutePath(), bitmapOps);
            }
        } catch (Throwable ex) {
            LogUtil.w(ex.getMessage(), ex);
        } finally {
            IOUtil.closeQuietly(cacheFile);
        }
        return null;
    }
}
