package org.xutils.cache;

import org.xutils.common.util.IOUtil;
import org.xutils.common.util.ProcessLock;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Created by wyouflf on 15/8/3.
 * 磁盘缓存文件, 操作完成后必须及时调用close()方法关闭.
 */
public final class DiskCacheFile extends File implements Closeable {

    private final DiskCacheEntity cacheEntity;
    private final ProcessLock lock;

    /*package*/ DiskCacheFile(String path, DiskCacheEntity cacheEntity, ProcessLock lock) {
        super(path);
        this.cacheEntity = cacheEntity;
        this.lock = lock;
    }

    @Override
    public void close() throws IOException {
        IOUtil.closeQuietly(lock);
    }

    public DiskCacheFile commit() throws IOException {
        return getDiskCache().commitDiskCacheFile(this);
    }

    public LruDiskCache getDiskCache() {
        String dirName = this.getParentFile().getName();
        return LruDiskCache.getDiskCache(dirName);
    }

    public DiskCacheEntity getCacheEntity() {
        return cacheEntity;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.close();
    }
}
