package org.xutils.http.loader;

import android.text.TextUtils;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.cache.DiskCacheFile;
import org.xutils.cache.LruDiskCache;
import org.xutils.common.Callback;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.common.util.ProcessLock;
import org.xutils.ex.FileLockedException;
import org.xutils.ex.HttpException;
import org.xutils.http.RequestParams;
import org.xutils.http.request.UriRequest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Date;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 * 下载参数策略:
 * 1. RequestParams#saveFilePath不为空时, 目标文件保存在saveFilePath;
 * 否则由Cache策略分配文件下载路径.
 * 2. 下载时临时目标文件路径为tempSaveFilePath, 下载完后进行a: CacheFile#commit; b:重命名等操作.
 * 断点下载策略:
 * 1. 要下载的目标文件不存在或小于 CHECK_SIZE 时删除目标文件, 重新下载.
 * 2. 若文件存在且大于 CHECK_SIZE, range = fileLen - CHECK_SIZE , 校验check_buffer, 相同: 继续下载;
 * 不相同: 删掉目标文件, 并抛出RuntimeException(HttpRetryHandler会使下载重新开始).
 */
public class FileLoader extends Loader<File> {

    private static final int CHECK_SIZE = 512;

    private String tempSaveFilePath;
    private String saveFilePath;
    private boolean isAutoResume;
    private boolean isAutoRename;
    private long contentLength;
    private String responseFileName;

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

    @Override
    public File load(final InputStream in) throws Throwable {
        File targetFile = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            targetFile = new File(tempSaveFilePath);
            if (targetFile.isDirectory()) {
                // 防止文件正在写入时, 父文件夹被删除, 继续写入时造成偶现文件节点异常问题.
                IOUtil.deleteFileOrDir(targetFile);
            }
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
                        } else {
                            contentLength -= CHECK_SIZE;
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

            if (progressHandler != null && !progressHandler.updateProgress(total, current, true)) {
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
                if (progressHandler != null) {
                    if (!progressHandler.updateProgress(total, current, false)) {
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

            if (progressHandler != null) {
                progressHandler.updateProgress(total, current, true);
            }
        } finally {
            IOUtil.closeQuietly(bis);
            IOUtil.closeQuietly(bos);
        }

        return autoRename(targetFile);
    }

    @Override
    public File load(final UriRequest request) throws Throwable {
        ProcessLock processLock = null;
        File result = null;
        try {

            // 处理[下载逻辑1](见文件头doc)
            saveFilePath = params.getSaveFilePath();
            diskCacheFile = null;
            if (TextUtils.isEmpty(saveFilePath)) {

                if (progressHandler != null && !progressHandler.updateProgress(0, 0, false)) {
                    throw new Callback.CancelledException("download stopped!");
                }

                // 保存路径为空, 存入磁盘缓存.
                initDiskCacheFile(request);
            } else {
                tempSaveFilePath = saveFilePath + ".tmp";
            }

            if (progressHandler != null && !progressHandler.updateProgress(0, 0, false)) {
                throw new Callback.CancelledException("download stopped!");
            }

            // 等待, 若不能下载则取消此次下载.
            processLock = ProcessLock.tryLock(saveFilePath + "_lock", true);
            if (processLock == null || !processLock.isValid()) {
                throw new FileLockedException("download exists: " + saveFilePath);
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
                params.setHeader("RANGE", "bytes=" + range + "-");
            }

            if (progressHandler != null && !progressHandler.updateProgress(0, 0, false)) {
                throw new Callback.CancelledException("download stopped!");
            }

            request.sendRequest(); // may be throw an HttpException

            contentLength = request.getContentLength();
            if (isAutoRename) {
                responseFileName = getResponseFileName(request);
            }
            if (isAutoResume) {
                isAutoResume = isSupportRange(request);
            }

            if (progressHandler != null && !progressHandler.updateProgress(0, 0, false)) {
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
            if (httpException.getCode() == 416) {
                if (diskCacheFile != null) {
                    result = diskCacheFile.commit();
                } else {
                    result = new File(tempSaveFilePath);
                }
                // 从缓存获取文件, 不rename和断点, 直接退出.
                if (result != null && result.exists()) {
                    if (isAutoRename) {
                        responseFileName = getResponseFileName(request);
                    }
                    result = autoRename(result);
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
            saveFilePath = diskCacheFile.getAbsolutePath();
            // diskCacheFile is a temp path, diskCacheFile.commit() return the dest file.
            tempSaveFilePath = saveFilePath;
            isAutoRename = false;
        } else {
            throw new IOException("create cache file error:" + request.getCacheKey());
        }
    }

    // 处理[下载逻辑2.b](见文件头doc)
    private File autoRename(File loadedFile) {
        if (isAutoRename && loadedFile.exists() && !TextUtils.isEmpty(responseFileName)) {
            File newFile = new File(loadedFile.getParent(), responseFileName);
            while (newFile.exists()) {
                newFile = new File(loadedFile.getParent(), System.currentTimeMillis() + responseFileName);
            }
            return loadedFile.renameTo(newFile) ? newFile : loadedFile;
        } else if (!saveFilePath.equals(tempSaveFilePath)) {
            File newFile = new File(saveFilePath);
            return loadedFile.renameTo(newFile) ? newFile : loadedFile;
        } else {
            return loadedFile;
        }
    }

    private static String getResponseFileName(UriRequest request) {
        if (request == null) return null;
        String disposition = request.getResponseHeader("Content-Disposition");
        if (!TextUtils.isEmpty(disposition)) {
            int startIndex = disposition.indexOf("filename=");
            if (startIndex > 0) {
                startIndex += 9; // "filename=".length()
                int endIndex = disposition.indexOf(";", startIndex);
                if (endIndex < 0) {
                    endIndex = disposition.length();
                }
                if (endIndex > startIndex) {
                    try {
                        String name = URLDecoder.decode(
                                disposition.substring(startIndex, endIndex),
                                request.getParams().getCharset());
                        if (name.startsWith("\"") && name.endsWith("\"")) {
                            name = name.substring(1, name.length() - 1);
                        }
                        return name;
                    } catch (UnsupportedEncodingException ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSupportRange(UriRequest request) {
        if (request == null) return false;
        String ranges = request.getResponseHeader("Accept-Ranges");
        if (ranges != null) {
            return ranges.contains("bytes");
        }
        ranges = request.getResponseHeader("Content-Range");
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
}
