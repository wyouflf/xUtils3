package org.xutils.http.body;

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

    public StringBody(String str, String charset) throws UnsupportedEncodingException {
        this.content = str.getBytes(charset);
        this.charset = charset;
    }

    @Override
    public long getContentLength() {
        return content.length;
    }

    @Override
    public String getContentType() {
        return "application/json;charset=" + charset;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        out.write(content);
        out.flush();
    }
}
