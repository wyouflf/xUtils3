package org.xutils.http.body;

import android.net.Uri;
import android.text.TextUtils;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.util.Map;

import okio.BufferedSink;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
public class BodyParamsBody extends RequestBody {

    private byte[] content;
    private String charset;

    public BodyParamsBody(Map<String, Object> params, String charset) throws IOException {
        StringBuilder contentSb = new StringBuilder();
        if (params != null) {
            for (Map.Entry<String, Object> kv : params.entrySet()) {
                String name = kv.getKey();
                Object value = kv.getValue();
                if (!TextUtils.isEmpty(name) && value != null) {
                    if (contentSb.length() > 0) {
                        contentSb.append("&");
                    }
                    contentSb.append(Uri.encode(name, charset))
                            .append("=")
                            .append(Uri.encode(value.toString(), charset));
                }
            }
        }

        this.content = contentSb.toString().getBytes(charset);
        this.charset = charset;
    }

    @Override
    public long contentLength() {
        return content.length;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("application/x-www-form-urlencoded;charset=" + charset);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        sink.write(this.content);
        sink.flush();
    }
}
