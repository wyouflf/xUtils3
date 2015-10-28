package org.xutils.image;

import android.graphics.Movie;

/*package*/ final class ReusableGifDrawable extends GifDrawable implements ReusableDrawable {

    private MemCacheKey key;

    public ReusableGifDrawable(Movie movie, int byteCount) {
        super(movie, byteCount);
    }

    @Override
    public MemCacheKey getMemCacheKey() {
        return key;
    }

    @Override
    public void setMemCacheKey(MemCacheKey key) {
        this.key = key;
    }
}
