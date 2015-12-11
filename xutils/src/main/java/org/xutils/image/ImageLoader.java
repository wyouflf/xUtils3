package org.xutils.image;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;

import org.xutils.cache.LruCache;
import org.xutils.cache.LruDiskCache;
import org.xutils.common.Callback;
import org.xutils.common.task.Priority;
import org.xutils.common.task.PriorityExecutor;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.ex.FileLockedException;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wyouflf on 15/10/9.
 * 图片加载控制
 */
/*package*/ final class ImageLoader implements
        Callback.PrepareCallback<File, Drawable>,
        Callback.CacheCallback<Drawable>,
        Callback.ProgressCallback<Drawable>,
        Callback.Cancelable {

    private MemCacheKey key;
    private ImageOptions options;
    private WeakReference<ImageView> viewRef;

    private final static AtomicLong SEQ_SEEK = new AtomicLong(0);
    private final long seq = SEQ_SEEK.incrementAndGet();

    private volatile boolean stopped = false;
    private volatile boolean cancelled = false;
    private Callback.Cancelable cancelable;
    private Callback.CommonCallback<Drawable> callback;
    private Callback.PrepareCallback<File, Drawable> prepareCallback;
    private Callback.CacheCallback<Drawable> cacheCallback;
    private Callback.ProgressCallback<Drawable> progressCallback;

    private final static String DISK_CACHE_DIR_NAME = "xUtils_img";
    private final static Executor EXECUTOR = new PriorityExecutor(10);
    private final static int MEM_CACHE_MIN_SIZE = 1024 * 1024 * 4; // 4M
    private final static LruCache<MemCacheKey, Drawable> MEM_CACHE =
            new LruCache<MemCacheKey, Drawable>(MEM_CACHE_MIN_SIZE) {
                private boolean deepClear = false;

                @Override
                protected int sizeOf(MemCacheKey key, Drawable value) {
                    if (value instanceof BitmapDrawable) {
                        Bitmap bitmap = ((BitmapDrawable) value).getBitmap();
                        return bitmap == null ? 0 : bitmap.getByteCount();
                    } else if (value instanceof GifDrawable) {
                        return ((GifDrawable) value).getByteCount();
                    }
                    return super.sizeOf(key, value);
                }

                @Override
                public void trimToSize(int maxSize) {
                    if (maxSize < 0) {
                        deepClear = true;
                    }
                    super.trimToSize(maxSize);
                    deepClear = false;
                }

                @Override
                protected void entryRemoved(boolean evicted, MemCacheKey key, Drawable oldValue, Drawable newValue) {
                    super.entryRemoved(evicted, key, oldValue, newValue);
                    if (evicted && deepClear && oldValue instanceof ReusableDrawable) {
                        ((ReusableDrawable) oldValue).setMemCacheKey(null);
                    }
                }
            };

    static {
        int memClass = ((ActivityManager) x.app()
                .getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

        // Use 1/8th of the available memory for this memory cache.
        int cacheSize = 1024 * 1024 * memClass / 8;
        if (cacheSize < MEM_CACHE_MIN_SIZE) {
            cacheSize = MEM_CACHE_MIN_SIZE;
        }
        MEM_CACHE.resize(cacheSize);
    }

    private ImageLoader() {
    }

    /*package*/
    static void clearMemCache() {
        MEM_CACHE.evictAll();
    }

    /*package*/
    static void clearCacheFiles() {
        LruDiskCache.getDiskCache(DISK_CACHE_DIR_NAME).clearCacheFiles();
    }

    private final static HashMap<String, FakeImageView> FAKE_IMG_MAP = new HashMap<String, FakeImageView>();

    /**
     * load from Network or DiskCache, invoke in any thread.
     *
     * @param url
     * @param options
     * @param callback
     */
    /*package*/
    static Cancelable doLoadDrawable(final String url,
                                     final ImageOptions options,
                                     final Callback.CommonCallback<Drawable> callback) {
        if (TextUtils.isEmpty(url)) {
            postBindArgsException(null, options, "url is null", callback);
            return null;
        }

        FakeImageView fakeImageView = null;
        synchronized (FAKE_IMG_MAP) {
            fakeImageView = FAKE_IMG_MAP.get(url);
            if (fakeImageView == null) {
                fakeImageView = new FakeImageView();
            }
        }
        return doBind(fakeImageView, url, options, callback);
    }

    /**
     * load from Network or DiskCache, invoke in any thread.
     *
     * @param url
     * @param options
     * @param callback
     */
    /*package*/
    static Cancelable doLoadFile(final String url,
                                 final ImageOptions options,
                                 final Callback.CommonCallback<File> callback) {
        if (TextUtils.isEmpty(url)) {
            postBindArgsException(null, options, "url is null", callback);
            return null;
        }

        RequestParams params = new RequestParams(url);
        params.setCacheDirName(DISK_CACHE_DIR_NAME);
        params.setConnectTimeout(1000 * 8);
        params.setPriority(Priority.BG_LOW);
        params.setExecutor(EXECUTOR);
        params.setUseCookie(false);
        if (options != null) {
            ImageOptions.ParamsBuilder paramsBuilder = options.getParamsBuilder();
            if (paramsBuilder != null) {
                params = paramsBuilder.buildParams(params, options);
            }
        }
        return x.http().get(params, callback);
    }

    /**
     * load from Network or DiskCache, invoke in ui thread.
     *
     * @param view
     * @param url
     * @param options
     * @param callback
     */
    /*package*/
    static Cancelable doBind(final ImageView view,
                             final String url,
                             final ImageOptions options,
                             final Callback.CommonCallback<Drawable> callback) {

        // check params
        ImageOptions localOptions = options;
        {
            if (view == null) {
                postBindArgsException(null, localOptions, "view is null", callback);
                return null;
            }

            if (TextUtils.isEmpty(url)) {
                postBindArgsException(view, localOptions, "url is null", callback);
                return null;
            }

            if (localOptions == null) {
                localOptions = ImageOptions.DEFAULT;
            }
            localOptions.optimizeMaxSize(view);
        }

        // stop the old loader
        MemCacheKey key = new MemCacheKey(url, localOptions);
        Drawable oldDrawable = view.getDrawable();
        if (oldDrawable instanceof AsyncDrawable) {
            ImageLoader loader = ((AsyncDrawable) oldDrawable).getImageLoader();
            if (loader != null && !loader.stopped) {
                if (key.equals(loader.key)) {
                    // repetitive url and options binding to the same View.
                    // not need callback to ui.
                    return null;
                } else {
                    loader.cancel();
                }
            }
        } else if (oldDrawable instanceof ReusableDrawable) {
            MemCacheKey oldKey = ((ReusableDrawable) oldDrawable).getMemCacheKey();
            if (oldKey != null && oldKey.equals(key)) {
                MEM_CACHE.put(key, oldDrawable);
            }
        }

        // load from Memory Cache
        Drawable memDrawable = null;
        if (localOptions.isUseMemCache()) {
            memDrawable = MEM_CACHE.get(key);
            if (memDrawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) memDrawable).getBitmap();
                if (bitmap == null || bitmap.isRecycled()) {
                    memDrawable = null;
                }
            }
        }
        if (memDrawable != null) { // has mem cache
            boolean trustMemCache = false;
            try {
                if (callback instanceof ProgressCallback) {
                    ((ProgressCallback) callback).onWaiting();
                }
                // hit mem cache
                view.setScaleType(localOptions.getImageScaleType());
                view.setImageDrawable(memDrawable);
                trustMemCache = true;
                if (callback instanceof CacheCallback) {
                    trustMemCache = ((CacheCallback<Drawable>) callback).onCache(memDrawable);
                    if (!trustMemCache) {
                        // not trust the cache
                        // load from Network or DiskCache
                        return new ImageLoader().doLoad(view, url, localOptions, callback);
                    }
                } else if (callback != null) {
                    callback.onSuccess(memDrawable);
                }
            } catch (Throwable ex) {
                LogUtil.e(ex.getMessage(), ex);
                // try load from Network or DiskCache
                trustMemCache = false;
                return new ImageLoader().doLoad(view, url, localOptions, callback);
            } finally {
                if (trustMemCache && callback != null) {
                    try {
                        callback.onFinished();
                    } catch (Throwable ignored) {
                        LogUtil.e(ignored.getMessage(), ignored);
                    }
                }
            }
        } else {
            // load from Network or DiskCache
            return new ImageLoader().doLoad(view, url, localOptions, callback);
        }
        return null;
    }

    /**
     * load from Network or DiskCache
     *
     * @param view
     * @param url
     * @param options
     * @param callback
     */
    private Cancelable doLoad(ImageView view,
                              String url,
                              ImageOptions options,
                              Callback.CommonCallback<Drawable> callback) {

        this.viewRef = new WeakReference<ImageView>(view);
        this.options = options;
        this.key = new MemCacheKey(url, options);
        this.callback = callback;
        if (callback instanceof Callback.ProgressCallback) {
            this.progressCallback = (Callback.ProgressCallback<Drawable>) callback;
        }
        if (callback instanceof Callback.PrepareCallback) {
            this.prepareCallback = (Callback.PrepareCallback<File, Drawable>) callback;
        }
        if (callback instanceof Callback.CacheCallback) {
            this.cacheCallback = (Callback.CacheCallback<Drawable>) callback;
        }

        // set loadingDrawable
        Drawable loadingDrawable = null;
        if (options.isForceLoadingDrawable()) {
            loadingDrawable = options.getLoadingDrawable(view);
            view.setScaleType(options.getPlaceholderScaleType());
            view.setImageDrawable(new AsyncDrawable(this, loadingDrawable));
        } else {
            loadingDrawable = view.getDrawable();
            view.setImageDrawable(new AsyncDrawable(this, loadingDrawable));
        }

        // request
        RequestParams params = new RequestParams(url);
        params.setCacheDirName(DISK_CACHE_DIR_NAME);
        params.setConnectTimeout(1000 * 8);
        params.setPriority(Priority.BG_LOW);
        params.setExecutor(EXECUTOR);
        params.setCancelFast(true);
        params.setUseCookie(false);
        ImageOptions.ParamsBuilder paramsBuilder = options.getParamsBuilder();
        if (paramsBuilder != null) {
            params = paramsBuilder.buildParams(params, options);
        }
        if (view instanceof FakeImageView) {
            synchronized (FAKE_IMG_MAP) {
                FAKE_IMG_MAP.put(url, (FakeImageView) view);
            }
        }
        return cancelable = x.http().get(params, this);
    }

    @Override
    public void cancel() {
        stopped = true;
        cancelled = true;
        if (cancelable != null) {
            cancelable.cancel();
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled || !validView4Callback(false);
    }

    @Override
    public void onWaiting() {
        if (progressCallback != null) {
            progressCallback.onWaiting();
        }
    }

    @Override
    public void onStarted() {
        if (validView4Callback(true) && progressCallback != null) {
            progressCallback.onStarted();
        }
    }

    @Override
    public void onLoading(long total, long current, boolean isDownloading) {
        if (validView4Callback(true) && progressCallback != null) {
            progressCallback.onLoading(total, current, isDownloading);
        }
    }

    @Override
    public Drawable prepare(File rawData) {
        if (!validView4Callback(true)) return null;

        try {
            Drawable result = null;
            if (prepareCallback != null) {
                result = prepareCallback.prepare(rawData);
            }
            if (result == null) {
                result = ImageDecoder.decodeFileWithLock(rawData, options, this);
            }
            if (result != null) {
                if (result instanceof ReusableDrawable) {
                    ((ReusableDrawable) result).setMemCacheKey(key);
                    MEM_CACHE.put(key, result);
                }
            }
            return result;
        } catch (IOException ex) {
            IOUtil.deleteFileOrDir(rawData);
            LogUtil.w(ex.getMessage(), ex);
        }
        return null;
    }

    private boolean hasCache = false;

    @Override
    public boolean onCache(Drawable result) {
        if (!validView4Callback(true)) return false;

        if (result != null) {
            hasCache = true;
            setSuccessDrawable4Callback(result);
            if (cacheCallback != null) {
                return cacheCallback.onCache(result);
            } else if (callback != null) {
                callback.onSuccess(result);
                return true;
            }
            return true;
        }

        return false;
    }

    @Override
    public void onSuccess(Drawable result) {
        if (!validView4Callback(!hasCache)) return;

        if (result != null) {
            setSuccessDrawable4Callback(result);
            if (callback != null) {
                callback.onSuccess(result);
            }
        }
    }

    @Override
    public void onError(Throwable ex, boolean isOnCallback) {
        stopped = true;
        if (!validView4Callback(false)) return;

        if (ex instanceof FileLockedException) {
            LogUtil.d("ImageFileLocked: " + key.url);
            doBind(viewRef.get(), key.url, options, callback);
            return;
        }

        LogUtil.e(key.url, ex);
        setErrorDrawable4Callback();
        if (callback != null) {
            callback.onError(ex, isOnCallback);
        }
    }

    @Override
    public void onCancelled(CancelledException cex) {
        stopped = true;
        if (!validView4Callback(false)) return;

        if (callback != null) {
            callback.onCancelled(cex);
        }
    }

    @Override
    public void onFinished() {
        stopped = true;
        ImageView view = viewRef.get();
        if (view instanceof FakeImageView) {
            synchronized (FAKE_IMG_MAP) {
                FAKE_IMG_MAP.remove(key.url);
            }
        }

        if (!validView4Callback(false)) return;

        if (callback != null) {
            callback.onFinished();
        }
    }

    private boolean validView4Callback(boolean forceValidAsyncDrawable) {
        final ImageView view = viewRef.get();
        if (view != null) {
            Drawable otherDrawable = view.getDrawable();
            if (otherDrawable instanceof AsyncDrawable) {
                ImageLoader otherLoader = ((AsyncDrawable) otherDrawable).getImageLoader();
                if (otherLoader != null) {
                    if (otherLoader == this) {
                        if (view.getVisibility() != View.VISIBLE) {
                            otherLoader.cancel();
                            return false;
                        } else {
                            return true;
                        }
                    } else {
                        if (this.seq > otherLoader.seq) {
                            otherLoader.cancel();
                            return true;
                        } else {
                            this.cancel();
                            return false;
                        }
                    }
                }
            } else if (forceValidAsyncDrawable) {
                this.cancel();
                return false;
            }
            return true;
        }
        return false;
    }

    private synchronized void setSuccessDrawable4Callback(final Drawable drawable) {
        final ImageView view = viewRef.get();
        if (view != null) {
            view.setScaleType(options.getImageScaleType());
            if (drawable instanceof GifDrawable) {
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            if (options.getAnimation() != null) {
                ImageAnimationHelper.animationDisplay(view, drawable, options.getAnimation());
            } else if (options.isFadeIn()) {
                ImageAnimationHelper.fadeInDisplay(view, drawable);
            } else {
                view.setImageDrawable(drawable);
            }
        }
    }

    private synchronized void setErrorDrawable4Callback() {
        final ImageView view = viewRef.get();
        if (view != null) {
            Drawable drawable = options.getFailureDrawable(view);
            view.setScaleType(options.getPlaceholderScaleType());
            view.setImageDrawable(drawable);
        }
    }

    private static void postBindArgsException(
            final ImageView view, final ImageOptions options,
            final String exMsg, final Callback.CommonCallback<?> callback) {
        x.task().autoPost(new Runnable() {
            @Override
            public void run() {
                try {
                    if (callback instanceof ProgressCallback) {
                        ((ProgressCallback) callback).onWaiting();
                    }
                    if (view != null && options != null) {
                        view.setScaleType(options.getPlaceholderScaleType());
                        view.setImageDrawable(options.getFailureDrawable(view));
                    }
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException(exMsg), false);
                    }
                } catch (Throwable ex) {
                    if (callback != null) {
                        try {
                            callback.onError(ex, true);
                        } catch (Throwable ignored) {
                            LogUtil.e(ignored.getMessage(), ignored);
                        }
                    }
                } finally {
                    if (callback != null) {
                        try {
                            callback.onFinished();
                        } catch (Throwable ignored) {
                            LogUtil.e(ignored.getMessage(), ignored);
                        }
                    }
                }
            }
        });
    }

    @SuppressLint("ViewConstructor")
    private final static class FakeImageView extends ImageView {

        private Drawable drawable;

        public FakeImageView() {
            super(x.app());
        }

        @Override
        public void setImageDrawable(Drawable drawable) {
            this.drawable = drawable;
        }

        @Override
        public Drawable getDrawable() {
            return drawable;
        }

        @Override
        public void setLayerType(int layerType, Paint paint) {
        }

        @Override
        public void setScaleType(ScaleType scaleType) {
        }

        @Override
        public void startAnimation(Animation animation) {
        }
    }
}
