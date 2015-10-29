package org.xutils.http.body;

import android.text.TextUtils;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Created by wyouflf on 15/8/13.
 */
public final class ContentTypeInputStream extends FilterInputStream {

    private InputStream base;
    private String contentType;

    public ContentTypeInputStream(InputStream in, String contentType) {
        super(in);
        this.base = in;
        if (TextUtils.isEmpty(contentType)) {
            this.contentType = "application/octet-stream";
        } else {
            this.contentType = contentType;
        }
    }

    public InputStream getBase() {
        return base;
    }

    public String getContentType() {
        return contentType;
    }
}
