package org.xutils.common.util;


import android.content.Context;
import android.text.TextUtils;

import org.xutils.x;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DecimalFormat;

/**
 * @author: wyouflf
 * @date: 2015/06/26
 * 进程间锁, 仅在同一个应用中有效.
 * 一个锁的最大有效期时为1min.
 */
public final class ProcessLock implements Closeable {

    private FileLock mFileLock = null;
    private File mFile;
    private Closeable mStream;

    private final static long MAX_AGE = 1000 * 60; // 1min
    private final static String LOCK_FILE_DIR = "process_lock";

    static {
        IOUtil.deleteFileOrDir(x.app().getDir(LOCK_FILE_DIR, Context.MODE_PRIVATE));
    }

    private ProcessLock(FileLock fileLock, File file, Closeable stream) {
        mFileLock = fileLock;
        mFile = file;
        mStream = stream;
    }

    /**
     * 获取进程锁
     *
     * @param lockName
     * @param writeMode 是否写入模式(支持读并发).
     * @return null 或 进程锁, 如果锁已经被占用, 返回null.
     */
    public static ProcessLock tryLock(String lockName, boolean writeMode) {
        FileInputStream in = null;
        FileOutputStream out = null;
        Closeable stream = null;
        FileChannel channel = null;
        try {
            File file = new File(x.app().getDir(LOCK_FILE_DIR, Context.MODE_PRIVATE), customHash(lockName));
            if (file.exists()) {
                if (file.lastModified() + MAX_AGE < System.currentTimeMillis()) {
                    IOUtil.deleteFileOrDir(file);
                }
                return null;
            }
            if (file.exists() || file.createNewFile()) {
                if (writeMode) {
                    out = new FileOutputStream(file, false);
                    channel = out.getChannel();
                    stream = out;
                } else {
                    in = new FileInputStream(file);
                    channel = in.getChannel();
                    stream = in;
                }
                if (channel != null) {
                    FileLock fileLock = channel.tryLock(0L, Long.MAX_VALUE, !writeMode);
                    if (isValid(fileLock)) {
                        LogUtil.d("lock: " + file.getName() + ":" + android.os.Process.myPid());
                        return new ProcessLock(fileLock, file, stream);
                    } else {
                        release(fileLock, file, out);
                    }
                } else {
                    throw new IOException("can not get file channel:" + file.getAbsolutePath());
                }
            }
        } catch (Throwable ignored) {
            LogUtil.d("tryLock: " + lockName + ", " + ignored.getMessage());
            IOUtil.closeQuietly(in);
            IOUtil.closeQuietly(out);
            IOUtil.closeQuietly(channel);
        }

        return null;
    }

    /**
     * 获取进程锁
     *
     * @param lockName
     * @param writeMode         是否写入模式(支持读并发).
     * @param maxWaitTimeMillis 最大值 1000 * 60
     * @return null 或 进程锁, 如果锁已经被占用, 则在超时时间内继续尝试获取该锁.
     */
    public static ProcessLock tryLock(String lockName, boolean writeMode, long maxWaitTimeMillis) {
        ProcessLock lock = null;
        final long expiryTime = System.currentTimeMillis() + maxWaitTimeMillis;
        while (System.currentTimeMillis() < expiryTime) {
            lock = tryLock(lockName, writeMode);
            if (lock != null) {
                break;
            } else {
                try {
                    Thread.sleep(1); // milliseconds
                } catch (Throwable ignored) {
                }
            }
        }

        return lock;
    }

    /**
     * 锁是否有效
     *
     * @return
     */
    public boolean isValid() {
        return isValid(mFileLock);
    }

    /**
     * 释放锁
     */
    public void release() {
        release(mFileLock, mFile, mStream);
    }

    /**
     * 释放锁
     */
    @Override
    public void close() throws IOException {
        release();
    }

    private static boolean isValid(FileLock fileLock) {
        return fileLock != null && fileLock.isValid();
    }

    private static void release(FileLock fileLock, File file, Closeable stream) {
        LogUtil.d("release: " + file.getName() + ":" + android.os.Process.myPid());
        if (fileLock != null) {
            IOUtil.closeQuietly(stream);
            IOUtil.closeQuietly(fileLock.channel());
            try {
                fileLock.release();
            } catch (Throwable ignored) {
            }
        }
        IOUtil.deleteFileOrDir(file);
    }

    private final static DecimalFormat FORMAT = new DecimalFormat("0.##################");

    /**
     * 取得字符串的自定义hash值, 尽量保证255字节内的hash不重复.
     *
     * @param str
     * @return
     */
    private static String customHash(String str) {
        if (TextUtils.isEmpty(str)) return "0";
        double hash = 0.0;
        byte[] bytes = str.getBytes();
        for (int i = 0; i < str.length(); i++) {
            hash = (255.0 * hash + bytes[i]) * 0.005;
        }
        return FORMAT.format(hash);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.release();
    }
}
