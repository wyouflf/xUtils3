package org.xutils.http.body;

import android.text.TextUtils;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import org.xutils.common.Callback;
import org.xutils.http.ProgressCallbackHandler;

import java.io.IOException;
import java.io.InputStream;

import okio.BufferedSink;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
public class InputStreamBody extends RequestBody implements ProgressBody {

    private InputStream content;
    private String contentType;

    private long total;
    private long current = 0;

    private ProgressCallbackHandler callBackHandler;

    public InputStreamBody(InputStream inputStream, String contentType) {
        this.content = inputStream;
        if (TextUtils.isEmpty(contentType)) {
            this.contentType = "application/octet-stream";
        } else {
            this.contentType = contentType;
        }
        try {
            this.total = inputStream.available();
        } catch (IOException e) {
            this.total = -1;
        }
    }

    @Override
    public void setProgressCallbackHandler(ProgressCallbackHandler progressCallbackHandler) {
        this.callBackHandler = progressCallbackHandler;
    }

    @Override
    public long contentLength() throws IOException {
        return total;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (callBackHandler != null && !callBackHandler.updateProgress(total, current, true)) {
            throw new Callback.CancelledException("upload stopped!");
        }

        byte[] buffer = new byte[1024];
        try {
            int len = 0;
            while ((len = content.read(buffer)) != -1) {
                sink.write(buffer, 0, len);
                current += len;
                if (callBackHandler != null && !callBackHandler.updateProgress(total, current, false)) {
                    throw new Callback.CancelledException("upload stopped!");
                }
            }
            sink.flush();

            if (callBackHandler != null) {
                callBackHandler.updateProgress(total, total, true);
            }
        } finally {
            try {
                content.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
