package org.xutils.http;

/**
 * 进度控制接口, updateProgress方式中ProgressCallback#onLoading.
 * 默认最长间隔300毫秒调用一次.
 * Author: wyouflf
 * Time: 2014/05/23
 */
public interface ProgressHandler {
    /**
     * @param total
     * @param current
     * @param forceUpdateUI
     * @return continue
     */
    boolean updateProgress(long total, long current, boolean forceUpdateUI);
}
