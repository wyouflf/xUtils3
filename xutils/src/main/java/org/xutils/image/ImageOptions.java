package org.xutils.image;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;

import org.xutils.common.util.DensityUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.http.RequestParams;

/**
 * Created by wyouflf on 15/8/21.
 * 图片加载参数
 */
public class ImageOptions {

    public final static ImageOptions DEFAULT = new ImageOptions();

    // region ###################### decode options (equals & hashcode prop) ################
    private int maxWidth = 0;
    private int maxHeight = 0;
    private int width = 0; // 小于0时不采样压缩. 等于0时自动识别ImageView的宽高和maxWidth.
    private int height = 0; // 小于0时不采样压缩. 等于0时自动识别ImageView的宽高和maxHeight.
    private boolean crop = false; // crop to (width, height)

    private int radius = 0;
    private boolean square = false;
    private boolean circular = false;
    private boolean autoRotate = false;
    private boolean compress = true;
    private Bitmap.Config config = Bitmap.Config.RGB_565;

    // gif option
    private boolean ignoreGif = true;
    // end region ########################################## decode options #################

    // region ############# display options
    private int loadingDrawableId = 0;
    private int failureDrawableId = 0;
    private Drawable loadingDrawable = null;
    private Drawable failureDrawable = null;
    private boolean forceLoadingDrawable = true;

    private ImageView.ScaleType placeholderScaleType = ImageView.ScaleType.CENTER_INSIDE;
    private ImageView.ScaleType imageScaleType = ImageView.ScaleType.CENTER_CROP;

    private boolean fadeIn = false;
    private Animation animation = null;
    // end region ############ display options

    // extends
    private boolean useMemCache = true;
    private ParamsBuilder paramsBuilder;

    protected ImageOptions() {
    }

    /*package*/
    final void optimizeMaxSize(ImageView view) {
        if (width > 0 && height > 0) {
            maxWidth = width;
            maxHeight = height;
            return;
        }

        int screenWidth = DensityUtil.getScreenWidth();
        int screenHeight = DensityUtil.getScreenHeight();

        if (this == DEFAULT) {
            maxWidth = width = screenWidth * 3 / 2;
            maxHeight = height = screenHeight * 3 / 2;
            return;
        }

        if (width < 0) {
            maxWidth = screenWidth * 3 / 2;
            compress = false;
        }
        if (height < 0) {
            maxHeight = screenHeight * 3 / 2;
            compress = false;
        }

        if (view == null && maxWidth <= 0 && maxHeight <= 0) {
            maxWidth = screenWidth;
            maxHeight = screenHeight;
        } else {
            int tempWidth = maxWidth;
            int tempHeight = maxHeight;

            if (view != null) {
                final ViewGroup.LayoutParams params = view.getLayoutParams();
                if (params != null) {

                    if (tempWidth <= 0) {
                        if (params.width > 0) {
                            tempWidth = params.width;
                            if (this.width <= 0) {
                                this.width = tempWidth;
                            }
                        } else if (params.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
                            tempWidth = view.getWidth();
                        }
                    }

                    if (tempHeight <= 0) {
                        if (params.height > 0) {
                            tempHeight = params.height;
                            if (this.height <= 0) {
                                this.height = tempHeight;
                            }
                        } else if (params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                            tempHeight = view.getHeight();
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    if (tempWidth <= 0) tempWidth = view.getMaxWidth();
                    if (tempHeight <= 0) tempHeight = view.getMaxHeight();
                }
            }

            if (tempWidth <= 0) tempWidth = screenWidth;
            if (tempHeight <= 0) tempHeight = screenHeight;

            maxWidth = tempWidth;
            maxHeight = tempHeight;
        }
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isCrop() {
        return crop;
    }

    public int getRadius() {
        return radius;
    }

    public boolean isSquare() {
        return square;
    }

    public boolean isCircular() {
        return circular;
    }

    public boolean isIgnoreGif() {
        return ignoreGif;
    }

    public boolean isAutoRotate() {
        return autoRotate;
    }

    public boolean isCompress() {
        return compress;
    }

    public Bitmap.Config getConfig() {
        return config;
    }

    public Drawable getLoadingDrawable(ImageView view) {
        if (loadingDrawable == null && loadingDrawableId > 0 && view != null) {
            try {
                loadingDrawable = view.getResources().getDrawable(loadingDrawableId);
            } catch (Throwable ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
        }
        return loadingDrawable;
    }

    public Drawable getFailureDrawable(ImageView view) {
        if (failureDrawable == null && failureDrawableId > 0 && view != null) {
            try {
                failureDrawable = view.getResources().getDrawable(failureDrawableId);
            } catch (Throwable ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
        }
        return failureDrawable;
    }

    public boolean isFadeIn() {
        return fadeIn;
    }

    public Animation getAnimation() {
        return animation;
    }

    public ImageView.ScaleType getPlaceholderScaleType() {
        return placeholderScaleType;
    }

    public ImageView.ScaleType getImageScaleType() {
        return imageScaleType;
    }

    public boolean isForceLoadingDrawable() {
        return forceLoadingDrawable;
    }

    public boolean isUseMemCache() {
        return useMemCache;
    }

    public ParamsBuilder getParamsBuilder() {
        return paramsBuilder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageOptions options = (ImageOptions) o;

        if (maxWidth != options.maxWidth) return false;
        if (maxHeight != options.maxHeight) return false;
        if (width != options.width) return false;
        if (height != options.height) return false;
        if (crop != options.crop) return false;
        if (radius != options.radius) return false;
        if (square != options.square) return false;
        if (circular != options.circular) return false;
        if (autoRotate != options.autoRotate) return false;
        if (compress != options.compress) return false;
        return config == options.config;

    }

    @Override
    public int hashCode() {
        int result = maxWidth;
        result = 31 * result + maxHeight;
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + (crop ? 1 : 0);
        result = 31 * result + radius;
        result = 31 * result + (square ? 1 : 0);
        result = 31 * result + (circular ? 1 : 0);
        result = 31 * result + (autoRotate ? 1 : 0);
        result = 31 * result + (compress ? 1 : 0);
        result = 31 * result + (config != null ? config.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("_");
        sb.append(maxWidth).append("_");
        sb.append(maxHeight).append("_");
        sb.append(width).append("_");
        sb.append(height).append("_");
        sb.append(radius).append("_");
        sb.append(config).append("_");
        sb.append(crop ? 1 : 0).append(square ? 1 : 0).append(circular ? 1 : 0);
        sb.append(autoRotate ? 1 : 0).append(compress ? 1 : 0);
        return sb.toString();
    }

    public interface ParamsBuilder {
        RequestParams buildParams(RequestParams params, ImageOptions options);
    }

    public static class Builder {

        protected ImageOptions options;

        public Builder() {
            newImageOptions();
        }

        protected void newImageOptions() {
            options = new ImageOptions();
        }

        public ImageOptions build() {
            return options;
        }

        /**
         * 小于0时不采样压缩. 等于0时自动识别ImageView的宽高和(maxWidth, maxHeight).
         */
        public Builder setSize(int width, int height) {
            options.width = width;
            options.height = height;
            return this;
        }

        public Builder setCrop(boolean crop) {
            options.crop = crop;
            return this;
        }

        public Builder setRadius(int radius) {
            options.radius = radius;
            return this;
        }

        public Builder setSquare(boolean square) {
            options.square = square;
            return this;
        }

        public Builder setCircular(boolean circular) {
            options.circular = circular;
            return this;
        }

        public Builder setAutoRotate(boolean autoRotate) {
            options.autoRotate = autoRotate;
            return this;
        }

        public Builder setConfig(Bitmap.Config config) {
            options.config = config;
            return this;
        }

        public Builder setIgnoreGif(boolean ignoreGif) {
            options.ignoreGif = ignoreGif;
            return this;
        }

        public Builder setLoadingDrawableId(int loadingDrawableId) {
            options.loadingDrawableId = loadingDrawableId;
            return this;
        }

        public Builder setLoadingDrawable(Drawable loadingDrawable) {
            options.loadingDrawable = loadingDrawable;
            return this;
        }

        public Builder setFailureDrawableId(int failureDrawableId) {
            options.failureDrawableId = failureDrawableId;
            return this;
        }

        public Builder setFailureDrawable(Drawable failureDrawable) {
            options.failureDrawable = failureDrawable;
            return this;
        }

        public Builder setFadeIn(boolean fadeIn) {
            options.fadeIn = fadeIn;
            return this;
        }

        public Builder setAnimation(Animation animation) {
            options.animation = animation;
            return this;
        }

        public Builder setPlaceholderScaleType(ImageView.ScaleType placeholderScaleType) {
            options.placeholderScaleType = placeholderScaleType;
            return this;
        }

        public Builder setImageScaleType(ImageView.ScaleType imageScaleType) {
            options.imageScaleType = imageScaleType;
            return this;
        }

        public Builder setForceLoadingDrawable(boolean forceLoadingDrawable) {
            options.forceLoadingDrawable = forceLoadingDrawable;
            return this;
        }

        public Builder setUseMemCache(boolean useMemCache) {
            options.useMemCache = useMemCache;
            return this;
        }

        public Builder setParamsBuilder(ParamsBuilder paramsBuilder) {
            options.paramsBuilder = paramsBuilder;
            return this;
        }
    }

}
