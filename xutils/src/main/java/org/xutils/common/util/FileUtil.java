package org.xutils.common.util;

import android.os.Environment;
import android.os.StatFs;

import org.xutils.x;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileUtil {

    private FileUtil() {
    }

    public static File getCacheDir(String dirName) {
        File result;
        if (existsSdcard()) {
            File cacheDir = x.app().getExternalCacheDir();
            if (cacheDir == null) {
                result = new File(Environment.getExternalStorageDirectory(),
                        ".android/" + x.app().getPackageName() + "/" + dirName);
            } else {
                result = new File(cacheDir, dirName);
            }
        } else {
            result = new File(x.app().getFilesDir(), dirName);
        }
        if (result.exists() || result.mkdirs()) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * 检查磁盘空间是否大于10mb
     *
     * @return true 大于
     */
    public static boolean isDiskAvailable() {
        long size = getDiskAvailableSize();
        return size > 10 * 1024 * 1024; // > 10bm
    }

    /**
     * 获取磁盘可用空间
     *
     * @return byte 单位 kb
     */
    public static long getDiskAvailableSize() {
        if (!existsSdcard()) return 0;
        File path = Environment.getExternalStorageDirectory(); // 取得sdcard文件路径
        StatFs stat = new StatFs(path.getAbsolutePath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
        // (availableBlocks * blockSize)/1024 KIB 单位
        // (availableBlocks * blockSize)/1024 /1024 MIB单位
    }

    public static Boolean existsSdcard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static long getFileOrDirSize(File file) {
        if (!file.exists()) return 0;
        if (!file.isDirectory()) return file.length();

        long length = 0;
        File[] list = file.listFiles();
        if (list != null) { // 文件夹被删除时, 子文件正在被写入, 文件属性异常返回null.
            for (File item : list) {
                length += getFileOrDirSize(item);
            }
        }

        return length;
    }

    /**
     * 复制文件到指定文件
     *
     * @param fromPath 源文件
     * @param toPath   复制到的文件
     * @return true 成功，false 失败
     */
    public static boolean copy(String fromPath, String toPath) {
        boolean result = false;
        File from = new File(fromPath);
        if (!from.exists()) {
            return result;
        }

        File toFile = new File(toPath);
        IOUtil.deleteFileOrDir(toFile);
        File toDir = toFile.getParentFile();
        if (toDir.exists() || toDir.mkdirs()) {
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                in = new FileInputStream(from);
                out = new FileOutputStream(toFile);
                IOUtil.copy(in, out);
                result = true;
            } catch (Throwable ex) {
                LogUtil.d(ex.getMessage(), ex);
                result = false;
            } finally {
                IOUtil.closeQuietly(in);
                IOUtil.closeQuietly(out);
            }
        }
        return result;
    }
}
