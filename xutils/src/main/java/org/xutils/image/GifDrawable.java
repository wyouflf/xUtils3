package org.xutils.image;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import org.xutils.common.util.LogUtil;

public class GifDrawable extends Drawable implements Runnable, Animatable {
    private Movie movie;
    private int byteCount;

    private volatile boolean running;
    private int duration;
    private long begin = SystemClock.uptimeMillis();

    public GifDrawable(Movie movie, int byteCount) {
        this.movie = movie;
        this.byteCount = byteCount;
        this.duration = movie.duration();
    }

    public Movie getMovie() {
        return movie;
    }

    public int getByteCount() {
        if (byteCount == 0) {
            byteCount = (movie.width() * movie.height() * 3) * (5/*fake frame count*/);
        }
        return byteCount;
    }

    @Override
    public void draw(Canvas canvas) {
        try {
            int ms = duration > 0 ? (int) (SystemClock.uptimeMillis() - begin) % duration : 0;
            movie.setTime(ms);
            movie.draw(canvas, 0, 0);
            if (duration > 0) {
                start();
            }
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    @Override
    public void start() {
        if (!isRunning()) {
            running = true;
            run();
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            unscheduleSelf(this);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        this.invalidateSelf();
        scheduleSelf(this, SystemClock.uptimeMillis() + 300); // scheduleSelf(runnable, when)
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public int getIntrinsicWidth() {
        return movie.width();
    }

    @Override
    public int getIntrinsicHeight() {
        return movie.height();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

}
