package org.xutils.cache;


import android.text.TextUtils;

import org.xutils.DbManager;
import org.xutils.common.task.PriorityExecutor;
import org.xutils.common.util.FileUtil;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.common.util.MD5;
import org.xutils.common.util.ProcessLock;
import org.xutils.config.DbConfigs;
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.ex.DbException;
import org.xutils.ex.FileLockedException;
import org.xutils.x;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Created by wyouflf on 15/7/23.
 * 使用sqlite索引实现的LruDiskCache
 */
public final class LruDiskCache {

    private static final HashMap<String, LruDiskCache> DISK_CACHE_MAP = new HashMap<String, LruDiskCache>(5);

    private static final int LIMIT_COUNT = 5000; // 限制最多5000条数据
    private static final long LIMIT_SIZE = 1024L * 1024L * 100L; // 限制最多100M文件

    private static final int LOCK_WAIT = 1000 * 3; // 3s
    private static final String CACHE_DIR_NAME = "xUtils_cache";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final Executor trimExecutor = new PriorityExecutor(1);

    private boolean available = false;
    private final DbManager cacheDb;
    private File cacheDir;
    private long diskCacheSize = LIMIT_SIZE;

    public synchronized static LruDiskCache getDiskCache(String dirName) {
        if (TextUtils.isEmpty(dirName)) dirName = CACHE_DIR_NAME;
        LruDiskCache cache = DISK_CACHE_MAP.get(dirName);
        if (cache == null) {
            cache = new LruDiskCache(dirName);
            DISK_CACHE_MAP.put(dirName, cache);
        }
        return cache;
    }

    private LruDiskCache(String dirName) {
        this.cacheDb = x.getDb(DbConfigs.HTTP.getConfig());
        this.cacheDir = FileUtil.getCacheDir(dirName);
        if (this.cacheDir != null && (this.cacheDir.exists() || this.cacheDir.mkdirs())) {
            available = true;
        }
        deleteNoIndexFiles();
    }

    public LruDiskCache setMaxSize(long maxSize) {
        if (maxSize > 0L) {
            long diskFreeSize = FileUtil.getDiskAvailableSize();
            if (diskFreeSize > maxSize) {
                diskCacheSize = maxSize;
            } else {
                diskCacheSize = diskFreeSize;
            }
        }
        return this;
    }

    public DiskCacheEntity get(String key) {
        if (!available || TextUtils.isEmpty(key)) return null;

        deleteExpiry();

        DiskCacheEntity result = null;
        try {
            result = this.cacheDb.selector(DiskCacheEntity.class)
                    .where("key", "=", key).findFirst();
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }

        // update hint & lastAccess...
        if (result != null) {
            result.setHits(result.getHits() + 1);
            result.setLastAccess(System.currentTimeMillis());
            try {
                this.cacheDb.update(result, "hits", "lastAccess");
            } catch (Throwable ex) {
                LogUtil.e(ex.getMessage(), ex);
            }
        }

        return result;
    }

    public void put(DiskCacheEntity entity) {
        if (!available
                || entity == null
                || TextUtils.isEmpty(entity.getTextContent())
                || entity.getExpires() < System.currentTimeMillis()) {
            return;
        }

        try {
            cacheDb.replace(entity);
        } catch (DbException ex) {
            LogUtil.e(ex.getMessage(), ex);
        }

        trimSize();
    }

    public DiskCacheFile getDiskCacheFile(String key) {
        if (!available || TextUtils.isEmpty(key)) {
            return null;
        }

        deleteExpiry();

        DiskCacheFile result = null;
        DiskCacheEntity entity = get(key);
        if (entity != null && new File(entity.getPath()).exists()) {
            ProcessLock processLock = ProcessLock.tryLock(entity.getPath(), false, LOCK_WAIT);
            if (processLock != null && processLock.isValid()) {
                result = new DiskCacheFile(entity, entity.getPath(), processLock);
                if (!result.exists()) {
                    try {
                        cacheDb.delete(entity);
                    } catch (DbException ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                    result = null;
                }
            }
        }

        return result;
    }

    public DiskCacheFile createDiskCacheFile(DiskCacheEntity entity) throws IOException {
        if (!available || entity == null) {
            return null;
        }

        DiskCacheFile result = null;

        entity.setPath(new File(this.cacheDir, MD5.md5(entity.getKey())).getAbsolutePath());
        String tempFilePath = entity.getPath() + TEMP_FILE_SUFFIX;
        ProcessLock processLock = ProcessLock.tryLock(tempFilePath, true);
        if (processLock != null && processLock.isValid()) {
            result = new DiskCacheFile(entity, tempFilePath, processLock);
            if (!result.getParentFile().exists()) {
                result.mkdirs();
            }
        } else {
            throw new FileLockedException(entity.getPath());
        }

        return result;
    }

    public void clearCacheFiles() {
        IOUtil.deleteFileOrDir(cacheDir);
    }

    /**
     * 添加缓存文件
     *
     * @param cacheFile
     */
    /*package*/ DiskCacheFile commitDiskCacheFile(DiskCacheFile cacheFile) throws IOException {
        if (cacheFile != null && cacheFile.length() < 1L) {
            IOUtil.closeQuietly(cacheFile);
            return null;
        }
        if (!available || cacheFile == null) {
            return null;
        }

        DiskCacheFile result = null;
        DiskCacheEntity cacheEntity = cacheFile.cacheEntity;
        if (cacheFile.getName().endsWith(TEMP_FILE_SUFFIX)) { // is temp file
            ProcessLock processLock = null;
            DiskCacheFile destFile = null;
            try {
                String destPath = cacheEntity.getPath();
                processLock = ProcessLock.tryLock(destPath, true, LOCK_WAIT);
                if (processLock != null && processLock.isValid()) { // lock
                    destFile = new DiskCacheFile(cacheEntity, destPath, processLock);
                    if (cacheFile.renameTo(destFile)) {
                        try {
                            result = destFile;
                            cacheDb.replace(cacheEntity);
                        } catch (DbException ex) {
                            LogUtil.e(ex.getMessage(), ex);
                        }

                        trimSize();
                    } else {
                        throw new IOException("rename:" + cacheFile.getAbsolutePath());
                    }
                } else {
                    throw new FileLockedException(destPath);
                }
            } finally {
                if (result == null) {
                    result = cacheFile;
                    IOUtil.closeQuietly(destFile);
                    IOUtil.closeQuietly(processLock);
                    IOUtil.deleteFileOrDir(destFile);
                } else {
                    IOUtil.closeQuietly(cacheFile);
                    IOUtil.deleteFileOrDir(cacheFile);
                }
            }
        } else {
            result = cacheFile;
        }

        return result;
    }

    private void trimSize() {
        trimExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (available) {

                    // trim db
                    try {
                        int count = (int) cacheDb.selector(DiskCacheEntity.class).count();
                        if (count > LIMIT_COUNT + 10) {
                            List<DiskCacheEntity> rmList = cacheDb.selector(DiskCacheEntity.class)
                                    .orderBy("lastAccess").orderBy("hits")
                                    .limit(count - LIMIT_COUNT).offset(0).findAll();
                            if (rmList != null && rmList.size() > 0) {
                                // delete cache files
                                for (DiskCacheEntity entity : rmList) {
                                    String path = entity.getPath();
                                    if (!TextUtils.isEmpty(path)) {
                                        if (deleteFileWithLock(path)
                                                && deleteFileWithLock(path + TEMP_FILE_SUFFIX)) {
                                            // delete db entity
                                            cacheDb.delete(entity);
                                        }
                                    }
                                }

                            }
                        }
                    } catch (DbException ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }

                    // trim disk
                    try {
                        while (FileUtil.getFileOrDirSize(cacheDir) > diskCacheSize) {
                            List<DiskCacheEntity> rmList = cacheDb.selector(DiskCacheEntity.class)
                                    .orderBy("lastAccess").orderBy("hits").limit(10).offset(0).findAll();
                            if (rmList != null && rmList.size() > 0) {
                                // delete cache files
                                for (DiskCacheEntity entity : rmList) {
                                    String path = entity.getPath();
                                    if (!TextUtils.isEmpty(path)) {
                                        if (deleteFileWithLock(path)
                                                && deleteFileWithLock(path + TEMP_FILE_SUFFIX)) {
                                            // delete db entity
                                            cacheDb.delete(entity);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (DbException ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                }
            }
        });
    }

    private void deleteExpiry() {
        try {
            WhereBuilder whereBuilder = WhereBuilder.b("expires", "<", System.currentTimeMillis());
            List<DiskCacheEntity> rmList = cacheDb.selector(DiskCacheEntity.class).where(whereBuilder).findAll();
            // delete db entities
            cacheDb.delete(DiskCacheEntity.class, whereBuilder);
            if (rmList != null && rmList.size() > 0) {
                // delete cache files
                for (DiskCacheEntity entity : rmList) {
                    String path = entity.getPath();
                    if (!TextUtils.isEmpty(path)) {
                        deleteFileWithLock(path);
                    }
                }
            }
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    /**
     * 清理未被数据库索引的历史缓存文件
     */
    private void deleteNoIndexFiles() {
        trimExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (available) {
                    try {
                        File[] fileList = cacheDir.listFiles();
                        if (fileList != null) {
                            for (File file : fileList) {
                                try {
                                    long count = cacheDb.selector(DiskCacheEntity.class)
                                            .where("path", "=", file.getAbsolutePath()).count();
                                    if (count < 1) {
                                        IOUtil.deleteFileOrDir(file);
                                    }
                                } catch (Throwable ex) {
                                    LogUtil.e(ex.getMessage(), ex);
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                }
            }
        });
    }

    private boolean deleteFileWithLock(String path) {
        ProcessLock processLock = null;
        try {
            processLock = ProcessLock.tryLock(path, true);
            if (processLock != null && processLock.isValid()) { // lock
                File file = new File(path);
                return IOUtil.deleteFileOrDir(file);
            }
        } finally {
            IOUtil.closeQuietly(processLock);
        }
        return false;
    }
}
