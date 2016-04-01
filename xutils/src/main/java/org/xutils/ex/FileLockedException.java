package org.xutils.ex;

/**
 * Created by wyouflf on 15/10/9.
 */
public class FileLockedException extends BaseException {
    private static final long serialVersionUID = 1L;

    public FileLockedException(String detailMessage) {
        super(detailMessage);
    }
}
