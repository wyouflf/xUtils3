package org.xutils.sample.download;

/**
 * Created by wyouflf on 15/11/10.
 */
public enum DownloadState {
    WAITING(0), STARTED(1), FINISHED(2), STOPPED(3), ERROR(4);

    private final int value;

    DownloadState(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static DownloadState valueOf(int value) {
        switch (value) {
            case 0:
                return WAITING;
            case 1:
                return STARTED;
            case 2:
                return FINISHED;
            case 3:
                return STOPPED;
            case 4:
                return ERROR;
            default:
                return STOPPED;
        }
    }
}
