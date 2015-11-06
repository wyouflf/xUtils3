package org.xutils.http.body;

import android.text.TextUtils;

/**
 * Created by wyouflf on 15/8/13.
 */
public final class ContentTypeWrapper<T> {

    private final T object;
    private final String contentType;

    public ContentTypeWrapper(T object, String contentType) {
        this.object = object;
        if (TextUtils.isEmpty(contentType)) {
            this.contentType = "application/octet-stream";
        } else {
            this.contentType = contentType;
        }
    }

    public T getObject() {
        return object;
    }

    public String getContentType() {
        return contentType;
    }
}
