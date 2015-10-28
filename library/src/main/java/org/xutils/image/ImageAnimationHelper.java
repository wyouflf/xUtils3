package org.xutils.image;

import android.graphics.drawable.Drawable;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import org.xutils.common.util.LogUtil;

import java.lang.reflect.Method;

/**
 * Created by wyouflf on 15/10/13.
 * ImageView Animation Helper
 */
public final class ImageAnimationHelper {

    private final static Method cloneMethod;

    static {
        Method method = null;
        try {
            method = Animation.class.getDeclaredMethod("clone");
            method.setAccessible(true);
        } catch (Throwable ex) {
            method = null;
            LogUtil.w(ex.getMessage(), ex);
        }
        cloneMethod = method;
    }

    private ImageAnimationHelper() {
    }

    public static void fadeInDisplay(final ImageView imageView, Drawable drawable) {
        AlphaAnimation fadeAnimation = new AlphaAnimation(0F, 1F);
        fadeAnimation.setDuration(300);
        fadeAnimation.setInterpolator(new DecelerateInterpolator());
        imageView.setImageDrawable(drawable);
        imageView.startAnimation(fadeAnimation);
    }

    public static void animationDisplay(ImageView imageView, Drawable drawable, Animation animation) {
        imageView.setImageDrawable(drawable);
        if (cloneMethod != null && animation != null) {
            try {
                imageView.startAnimation((Animation) cloneMethod.invoke(animation));
            } catch (Throwable ex) {
                imageView.startAnimation(animation);
            }
        } else {
            imageView.startAnimation(animation);
        }
    }
}
