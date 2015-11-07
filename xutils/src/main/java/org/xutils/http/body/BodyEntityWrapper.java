package org.xutils.http.body;

import android.text.TextUtils;

/**
 * Created by wyouflf on 15/8/13.
 * Wrapper for RequestBody value.
 */
public final class BodyEntityWrapper<T> {

    private final T object;
    private final String fileName;
    private final String contentType;

    public BodyEntityWrapper(T object, String contentType) {
        this(object, contentType, null);
    }

    public BodyEntityWrapper(T object, String contentType, String fileName) {
        this.object = object;
        if (TextUtils.isEmpty(contentType)) {
            this.contentType = "application/octet-stream";
        } else {
            this.contentType = contentType;
        }
        this.fileName = fileName;
    }

    public T getObject() {
        return object;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }
}
