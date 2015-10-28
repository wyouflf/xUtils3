package org.xutils.image;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import org.xutils.x;
import org.xutils.ImageManager;
import org.xutils.common.Callback;

import java.io.File;

/**
 * Created by wyouflf on 15/10/9.
 */
public final class ImageManagerImpl implements ImageManager {


    private static final Object lock = new Object();
    private static ImageManagerImpl instance;

    private ImageManagerImpl() {
    }

    public static void registerInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ImageManagerImpl();
                }
            }
        }
        x.Ext.setImageManager(instance);
    }


    @Override
    public void bind(ImageView view, String url) {
        ImageLoader.doBind(view, url, null, null);
    }

    @Override
    public void bind(ImageView view, String url, ImageOptions options) {
        ImageLoader.doBind(view, url, options, null);
    }

    @Override
    public void bind(ImageView view, String url, Callback.CommonCallback<Drawable> callback) {
        ImageLoader.doBind(view, url, null, callback);
    }

    @Override
    public void bind(ImageView view, String url, ImageOptions options, Callback.CommonCallback<Drawable> callback) {
        ImageLoader.doBind(view, url, options, callback);
    }

    @Override
    public Callback.Cancelable loadDrawable(String url, ImageOptions options, Callback.CommonCallback<Drawable> callback) {
        return ImageLoader.doLoadDrawable(url, options, callback);
    }

    @Override
    public Callback.Cancelable loadFile(String url, ImageOptions options, Callback.CommonCallback<File> callback) {
        return ImageLoader.doLoadFile(url, options, callback);
    }

    @Override
    public void clearCacheFiles() {
        ImageLoader.clearCacheFiles();
        ImageDecoder.clearCacheFiles();
    }
}
