package org.xutils.http.loader;

import android.text.TextUtils;

import com.squareup.okhttp.Response;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.DiskCacheFile;
import org.xutils.cache.LruDiskCache;
import org.xutils.common.Callback;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.ProcessLock;
import org.xutils.ex.HttpException;
import org.xutils.http.ProgressCallbackHandler;
import org.xutils.http.RequestParams;
import org.xutils.http.UriRequest;
import org.xutils.http.app.ResponseTracker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 * <p/>
 * 下载参数策略:
 * 1. RequestParams#saveFilePath不为空时, 目标文件保存在saveFilePath;
 * 否则由Cache策略分配文件下载路径.
 * 2. 下载时临时目标文件路径为tempSaveFilePath, 下载完后进行a: CacheFile#commit; b:重命名等操作.
 * <p/>
 * 断点下载策略:
 * 1. 要下载的目标文件不存在或小于 CHECK_SIZE 时删除目标文件, 重新下载.
 * 2. 若文件存在且大于 CHECK_SIZE, range = fileLen - CHECK_SIZE , 校验check_buffer, 相同: 继续下载;
 * 不相同: 删掉目标文件, 并抛出RuntimeException(HttpRetryHandler会使下载重新开始).
 */
/*package*/ class FileLoader implements Loader<File> {

    private static final int CHECK_SIZE = 1024;
    private static final int LOCK_WAIT = 1000 * 3; // 3s

    private String tempSaveFilePath;
    private String saveFilePath;
    private boolean isAutoResume;
    private boolean isAutoRename;
    private long contentLength;
    private String responseFileName;

    private RequestParams params;
    private DiskCacheFile diskCacheFile;

    @Override
    public Loader<File> newInstance() {
        return new FileLoader();
    }

    @Override
    public void setParams(final RequestParams params) {
        if (params != null) {
            this.params = params;
            isAutoResume = params.isAutoResume();
            isAutoRename = params.isAutoRename();
        }
    }

    private ProgressCallbackHandler callBackHandler;

    @Override
    public void setProgressCallbackHandler(final ProgressCallbackHandler progressCallbackHandler) {
        this.callBackHandler = progressCallbackHandler;
    }

    @Override
    public File load(final InputStream in) throws Throwable {
        File targetFile = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            targetFile = new File(tempSaveFilePath);
            if (!targetFile.exists()) {
                File dir = targetFile.getParentFile();
                if (dir.exists() || dir.mkdirs()) {
                    targetFile.createNewFile();
                }
            }

            // 处理[断点逻辑2](见文件头doc)
            long targetFileLen = targetFile.length();
            if (isAutoResume && targetFileLen > 0) {
                FileInputStream fis = null;
                try {
                    long filePos = targetFileLen - CHECK_SIZE;
                    if (filePos > 0) {
                        fis = new FileInputStream(targetFile);
                        byte[] fileCheckBuffer = IOUtil.readBytes(fis, filePos, CHECK_SIZE);
                        byte[] checkBuffer = IOUtil.readBytes(in, 0, CHECK_SIZE);
                        if (!Arrays.equals(checkBuffer, fileCheckBuffer)) {
                            IOUtil.closeQuietly(fis); // 先关闭文件流, 否则文件删除会失败.
                            IOUtil.deleteFileOrDir(targetFile);
                            throw new RuntimeException("need retry");
                        }
                    } else {
                        IOUtil.deleteFileOrDir(targetFile);
                        throw new RuntimeException("need retry");
                    }
                } finally {
                    IOUtil.closeQuietly(fis);
                }
            }

            // 开始下载
            long current = 0;
            FileOutputStream fileOutputStream = null;
            if (isAutoResume) {
                current = targetFileLen;
                fileOutputStream = new FileOutputStream(targetFile, true);
            } else {
                fileOutputStream = new FileOutputStream(targetFile);
            }

            long total = contentLength + current;
            bis = new BufferedInputStream(in);
            bos = new BufferedOutputStream(fileOutputStream);

            if (callBackHandler != null && !callBackHandler.updateProgress(total, current, true)) {
                throw new Callback.CancelledException("download stopped!");
            }

            byte[] tmp = new byte[4096];
            int len;
            while ((len = bis.read(tmp)) != -1) {

                // 防止父文件夹被其他进程删除, 继续写入时造成父文件夹变为0字节文件的问题.
                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                    throw new IOException("parent be deleted!");
                }

                bos.write(tmp, 0, len);
                current += len;
                if (callBackHandler != null) {
                    if (!callBackHandler.updateProgress(total, current, false)) {
                        bos.flush();
                        throw new Callback.CancelledException("download stopped!");
                    }
                }
            }
            bos.flush();
            // 处理[下载逻辑2.a](见文件头doc)
            if (diskCacheFile != null) {
                targetFile = diskCacheFile.commit();
            }

            if (callBackHandler != null) {
                callBackHandler.updateProgress(total, current, true);
            }
        } finally {
            IOUtil.closeQuietly(bis);
            IOUtil.closeQuietly(bos);
        }

        // 处理[下载逻辑2.b](见文件头doc)
        if (isAutoRename && targetFile.exists() && !TextUtils.isEmpty(responseFileName)) {
            File newFile = new File(targetFile.getParent(), responseFileName);
            while (newFile.exists()) {
                newFile = new File(targetFile.getParent(), System.currentTimeMillis() + responseFileName);
            }
            return targetFile.renameTo(newFile) ? newFile : targetFile;
        } else if (!saveFilePath.equals(tempSaveFilePath)) {
            File newFile = new File(saveFilePath);
            return targetFile.renameTo(newFile) ? newFile : targetFile;
        } else {
            return targetFile;
        }
    }

    @Override
    public File load(final UriRequest request) throws Throwable {

        // 如果连接是本地文件直接返回
        File file = request.getFile();
        if (file != null) return file;

        ProcessLock processLock = null;
        File result = null;
        try {

            // 处理[下载逻辑1](见文件头doc)
            saveFilePath = params.getSaveFilePath();
            diskCacheFile = null;
            if (TextUtils.isEmpty(saveFilePath)) {

                if (callBackHandler != null && !callBackHandler.updateProgress(0, 0, false)) {
                    throw new Callback.CancelledException("download stopped!");
                }

                // 保存路径为空, 存入磁盘缓存.
                initDiskCacheFile(request);
            } else {
                tempSaveFilePath = saveFilePath + ".tmp";
            }

            if (callBackHandler != null && !callBackHandler.updateProgress(0, 0, false)) {
                throw new Callback.CancelledException("download stopped!");
            }

            // 等待, 若不能下载则取消此次下载.
            processLock = ProcessLock.tryLock(saveFilePath + "_lock", true, LOCK_WAIT);
            if (processLock == null || !processLock.isValid()) {
                throw new Callback.CancelledException("download exists: " + saveFilePath);
            }

            params = request.getParams();
            {// 处理[断点逻辑1](见文件头doc)
                long range = 0;
                if (isAutoResume) {
                    File tempFile = new File(tempSaveFilePath);
                    long fileLen = tempFile.length();
                    if (fileLen <= CHECK_SIZE) {
                        IOUtil.deleteFileOrDir(tempFile);
                        range = 0;
                    } else {
                        range = fileLen - CHECK_SIZE;
                    }
                }
                // retry 时需要覆盖RANGE参数
                params.addHeader("RANGE", "bytes=" + range + "-");
            }

            if (callBackHandler != null && !callBackHandler.updateProgress(0, 0, false)) {
                throw new Callback.CancelledException("download stopped!");
            }

            request.sendRequest();

            contentLength = request.getContentLength();
            if (isAutoRename) {
                responseFileName = getResponseFileName(request.getResponse());
            }
            if (isAutoResume) {
                isAutoResume = isSupportRange(request.getResponse());
            }

            if (callBackHandler != null && !callBackHandler.updateProgress(0, 0, false)) {
                throw new Callback.CancelledException("download stopped!");
            }

            if (diskCacheFile != null) {
                DiskCacheEntity entity = diskCacheFile.getCacheEntity();
                entity.setLastAccess(System.currentTimeMillis());
                entity.setEtag(request.getETag());
                entity.setExpires(request.getExpiration());
                entity.setLastModify(new Date(request.getLastModified()));
            }
            result = this.load(request.getInputStream());
        } catch (HttpException httpException) {
            if (httpException.getExceptionCode() == 416) {
                if (saveFilePath != null) {
                    result = new File(saveFilePath);
                } else {
                    result = LruDiskCache.getDiskCache(params.getCacheDirName()).getDiskCacheFile(request.getCacheKey());
                }
                // 从缓存获取文件, 不rename和断点, 直接退出.
                if (result != null && result.exists()) {
                    return result;
                } else {
                    IOUtil.deleteFileOrDir(result);
                    throw new IllegalStateException("cache file not found" + request.getCacheKey());
                }
            } else {
                throw httpException;
            }
        } finally {
            IOUtil.closeQuietly(processLock);
            IOUtil.closeQuietly(diskCacheFile);
        }
        return result;
    }

    // 保存路径为空, 存入磁盘缓存.
    private void initDiskCacheFile(final UriRequest request) throws Throwable {

        DiskCacheEntity entity = new DiskCacheEntity();
        entity.setKey(request.getCacheKey());
        diskCacheFile = LruDiskCache.getDiskCache(params.getCacheDirName()).createDiskCacheFile(entity);

        if (diskCacheFile != null) {
            saveFilePath = diskCacheFile.getPath();
            // diskCacheFile is a temp path, diskCacheFile.commit() return the dest file.
            tempSaveFilePath = saveFilePath;
            isAutoRename = false;
        } else {
            throw new IOException("create cache file error:" + request.getCacheKey());
        }
    }

    private static String getResponseFileName(Response response) {
        if (response == null) return null;
        String disposition = response.header("Content-Disposition");
        if (!TextUtils.isEmpty(disposition)) {
            int startIndex = disposition.indexOf("filename=");
            if (startIndex > 0) {
                startIndex += 9; // "filename=".length()
                int endIndex = disposition.indexOf(";", startIndex);
                if (endIndex < 0) {
                    endIndex = disposition.length();
                }
                return disposition.substring(startIndex, endIndex);
            }
        }
        return null;
    }

    private static boolean isSupportRange(Response response) {
        if (response == null) return false;
        String ranges = response.header("Accept-Ranges");
        if (ranges != null) {
            return ranges.contains("bytes");
        }
        ranges = response.header("Content-Range");
        return ranges != null && ranges.contains("bytes");
    }

    @Override
    public File loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable {
        return LruDiskCache.getDiskCache(params.getCacheDirName()).getDiskCacheFile(cacheEntity.getKey());
    }

    @Override
    public void save2Cache(final UriRequest request) {
        // already saved by diskCacheFile#commit
    }

    private ResponseTracker tracker;

    @Override
    public void setResponseTracker(ResponseTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public ResponseTracker getResponseTracker() {
        return tracker;
    }
}
