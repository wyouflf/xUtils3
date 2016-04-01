package org.xutils.http.body;

import android.text.TextUtils;

/**
 * Created by wyouflf on 15/8/13.
 * Wrapper for RequestBody value.
 */
public final class BodyItemWrapper {

    private final Object value;
    private final String fileName;
    private final String contentType;

    public BodyItemWrapper(Object value, String contentType) {
        this(value, contentType, null);
    }

    public BodyItemWrapper(Object value, String contentType, String fileName) {
        this.value = value;
        if (TextUtils.isEmpty(contentType)) {
            this.contentType = "application/octet-stream";
        } else {
            this.contentType = contentType;
        }
        this.fileName = fileName;
    }

    public Object getValue() {
        return value;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }
}
