package org.xutils.http.body;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import okio.BufferedSink;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
public class StringBody extends RequestBody {

    private byte[] content;
    private String charset;

    public StringBody(String str, String charset) throws UnsupportedEncodingException {
        this.content = str.getBytes(charset);
        this.charset = charset;
    }

    @Override
    public long contentLength() throws IOException {
        return content.length;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("application/json;charset=" + charset);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        sink.write(content);
        sink.flush();
    }
}
