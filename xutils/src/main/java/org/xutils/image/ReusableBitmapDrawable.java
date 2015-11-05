package org.xutils.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.io.InputStream;

/*package*/ final class ReusableBitmapDrawable extends BitmapDrawable implements ReusableDrawable {

    private MemCacheKey key;

    public ReusableBitmapDrawable(Resources res, Bitmap bitmap) {
        super(res, bitmap);
    }

    public ReusableBitmapDrawable(Resources res, String filepath) {
        super(res, filepath);
    }

    public ReusableBitmapDrawable(Resources res, InputStream is) {
        super(res, is);
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
