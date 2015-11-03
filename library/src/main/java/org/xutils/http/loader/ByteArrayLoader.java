package org.xutils.http.loader;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.common.util.IOUtil;
import org.xutils.http.request.UriRequest;

import java.io.InputStream;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
/*package*/ class ByteArrayLoader extends Loader<byte[]> {

    @Override
    public Loader<byte[]> newInstance() {
        return new ByteArrayLoader();
    }

    @Override
    public byte[] load(final InputStream in) throws Throwable {
        return IOUtil.readBytes(in);
    }

    @Override
    public byte[] load(final UriRequest request) throws Throwable {
        request.sendRequest();
        return this.load(request.getInputStream());
    }

    @Override
    public byte[] loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable {
        return null;
    }

    @Override
    public void save2Cache(final UriRequest request) {
    }
}
