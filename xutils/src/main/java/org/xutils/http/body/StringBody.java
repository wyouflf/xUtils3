package org.xutils.http.body;

import android.text.TextUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
public class StringBody implements RequestBody {

    private byte[] content;
    private String charset;
    private String contentType;

    public StringBody(String str, String charset) throws UnsupportedEncodingException {
        this.content = str.getBytes(charset);
        this.charset = charset;
    }

    @Override
    public long getContentLength() {
        return content.length;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return TextUtils.isEmpty(contentType) ? "application/json;charset=" + charset : contentType;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(content);
        out.flush();
    }
}
