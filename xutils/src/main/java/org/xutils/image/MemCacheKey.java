package org.xutils.image;

/**
 * Created by wyouflf on 15/10/20.
 */
/*package*/ final class MemCacheKey {
    public final String url;
    public final ImageOptions options;

    public MemCacheKey(String url, ImageOptions options) {
        this.url = url;
        this.options = options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemCacheKey that = (MemCacheKey) o;

        if (!url.equals(that.url)) return false;
        return options.equals(that.options);

    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + options.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return url + options.toString();
    }
}
