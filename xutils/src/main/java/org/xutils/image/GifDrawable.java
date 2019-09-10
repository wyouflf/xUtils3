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

    private int byteCount;
    private int rate = 300;
    private volatile boolean running;

    private final Movie movie;
    private final int duration;
    private final long begin = SystemClock.uptimeMillis();

    public GifDrawable(Movie movie, int byteCount) {
        this.movie = movie;
        this.byteCount = byteCount;
        this.duration = movie.duration();
    }

    public int getDuration() {
        return duration;
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

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    @Override
    public void draw(Canvas canvas) {
        try {
            int time = duration > 0 ? (int) (SystemClock.uptimeMillis() - begin) % duration : 0;
            movie.setTime(time);
            movie.draw(canvas, 0, 0);
            start();
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
            running = false;
            this.unscheduleSelf(this);
        }
    }

    @Override
    public boolean isRunning() {
        return running && duration > 0;
    }

    @Override
    public void run() {
        if (duration > 0) {
            this.invalidateSelf();
            this.scheduleSelf(this, SystemClock.uptimeMillis() + rate);
        }
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
        return movie.isOpaque() ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }

}
