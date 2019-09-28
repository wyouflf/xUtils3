package org.xutils.http.body;

import android.text.TextUtils;

import org.xutils.common.util.KeyValue;
import org.xutils.common.util.LogUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
public class UrlEncodedBody implements RequestBody {

    private byte[] content;
    private String charset = "UTF-8";

    public UrlEncodedBody(List<KeyValue> params, String charset) throws IOException {
        if (!TextUtils.isEmpty(charset)) {
            this.charset = charset;
        }
        StringBuilder contentSb = new StringBuilder();
        if (params != null) {
            for (KeyValue kv : params) {
                String name = kv.key;
                String value = kv.getValueStrOrNull();
                if (!TextUtils.isEmpty(name) && value != null) {
                    if (contentSb.length() > 0) {
                        contentSb.append("&");
                    }
                    contentSb.append(URLEncoder.encode(name, this.charset).replaceAll("\\+", "%20"))
                            .append("=")
                            .append(URLEncoder.encode(value, this.charset).replaceAll("\\+", "%20"));
                }
            }
        }

        this.content = contentSb.toString().getBytes(this.charset);
    }

    @Override
    public long getContentLength() {
        return content.length;
    }

    @Override
    public void setContentType(String contentType) {
        if (!TextUtils.isEmpty(contentType)) {
            LogUtil.w("ignored Content-Type: " + contentType);
        }
    }

    @Override
    public String getContentType() {
        return "application/x-www-form-urlencoded;charset=" + charset;
    }

    @Override
    public void writeTo(OutputStream sink) throws IOException {
        sink.write(this.content);
        sink.flush();
    }
}
