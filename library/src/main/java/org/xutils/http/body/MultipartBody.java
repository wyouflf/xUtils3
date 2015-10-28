package org.xutils.http.body;


import android.text.TextUtils;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import org.xutils.common.Callback;
import org.xutils.http.ProgressCallbackHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;
import okio.Source;
import okio.Timeout;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
public class MultipartBody extends RequestBody implements ProgressBody {

    private static byte[] BOUNDARY_PREFIX_BYTES = "--------7da3d81520810".getBytes();
    private static byte[] END_BYTES = "\r\n".getBytes();
    private static byte[] TWO_DASHES_BYTES = "--".getBytes();
    private byte[] boundaryPostfixBytes;
    private String contentType;
    private String charset;

    private Map<String, Object> multipartParams;
    private long total = 0;
    private long current = 0;

    public MultipartBody(Map<String, Object> multipartParams, String charset) {
        this.multipartParams = multipartParams;
        this.charset = charset;
        generateContentType();

        // calc total
        CounterBufferedSink counter = new CounterBufferedSink();
        try {
            this.writeTo(counter);
            this.total = counter.count;
        } catch (IOException e) {
            this.total = -1;
        }
    }

    private ProgressCallbackHandler callBackHandler;

    @Override
    public void setProgressCallbackHandler(ProgressCallbackHandler progressCallbackHandler) {
        this.callBackHandler = progressCallbackHandler;
    }

    private void generateContentType() {
        String boundaryPostfix = Double.toHexString(Math.random() * 0xFFFF);
        boundaryPostfixBytes = boundaryPostfix.getBytes();
        contentType = "multipart/form-data; boundary=" + new String(BOUNDARY_PREFIX_BYTES) + boundaryPostfix;
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

        for (Map.Entry<String, Object> kv : multipartParams.entrySet()) {
            String name = kv.getKey();
            Object value = kv.getValue();
            if (!TextUtils.isEmpty(name) && value != null) {
                if (!writeEntry(sink, name, value, charset, boundaryPostfixBytes)) {
                    throw new Callback.CancelledException("upload stopped!");
                }
            }
        }
        writeLine(sink, TWO_DASHES_BYTES, BOUNDARY_PREFIX_BYTES, boundaryPostfixBytes, TWO_DASHES_BYTES);
        sink.flush();

        if (callBackHandler != null) {
            callBackHandler.updateProgress(total, total, true);
        }
    }

    private boolean writeEntry(BufferedSink sink,
                               String name, Object value,
                               String charset, byte[] boundaryPostfixBytes) throws IOException {
        writeLine(sink, TWO_DASHES_BYTES, BOUNDARY_PREFIX_BYTES, boundaryPostfixBytes);
        if (value instanceof File) {
            File file = (File) value;
            String filename = file.getName();
            String contentType = FileBody.getFileContentType(file);
            writeLine(sink, ("Content-Disposition: form-data; name=\""
                    + name.replace("\"", "%22") + "\"; filename=\""
                    + filename.replace("\"", "%22") + "\"").getBytes());
            writeLine(sink, ("Content-Type: " + contentType).getBytes());
            writeLine(sink); // 内容前空一行
            if (!writeStreamAndCloseIn(sink, new FileInputStream(file))) {
                return false;
            }
        } else {
            writeLine(sink, ("Content-Disposition: form-data; name=\""
                    + name.replace("\"", "%22") + "\"").getBytes());
            if (value instanceof InputStream) {
                if (value instanceof WrappedInputStream) {
                    WrappedInputStream wIn = (WrappedInputStream) value;
                    value = wIn.getBase();
                    String contentType = wIn.getContentType();
                    writeLine(sink, ("Content-Type: " + contentType).getBytes());
                }
                writeLine(sink); // 内容前空一行
                if (!writeStreamAndCloseIn(sink, (InputStream) value)) {
                    return false;
                }
            } else {
                byte[] content;
                if (value instanceof byte[]) {
                    content = (byte[]) value;
                } else {
                    writeLine(sink, ("Content-Type:text/plain; charset:" + charset).getBytes());
                    content = String.valueOf(value).getBytes(charset);
                }
                writeLine(sink); // 内容前空一行
                writeLine(sink, content);
                current += content.length;
                if (callBackHandler != null && !callBackHandler.updateProgress(total, current, false)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void writeLine(BufferedSink sink, byte[]... bs) throws IOException {
        if (bs != null) {
            for (byte[] b : bs) {
                sink.write(b);
            }
        }
        sink.write(END_BYTES);
    }

    private boolean writeStreamAndCloseIn(BufferedSink sink, InputStream in) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) >= 0) {
            sink.write(buf, 0, len);
            current += len;
            if (callBackHandler != null && !callBackHandler.updateProgress(total, current, false)) {
                return false;
            }
        }
        in.close();
        sink.write(END_BYTES);
        return true;
    }

    /**
     * 仅用来统计大小, 不写入数据.
     */
    private class CounterBufferedSink implements BufferedSink {

        long count = 0;

        public CounterBufferedSink() {
        }

        @Override
        public Buffer buffer() {
            return null;
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            count += byteCount;
        }

        @Override
        public BufferedSink write(ByteString byteString) throws IOException {
            count += byteString.size();
            return this;
        }

        @Override
        public BufferedSink write(byte[] source) throws IOException {
            count += source.length;
            return this;
        }

        @Override
        public BufferedSink write(byte[] source, int offset, int byteCount) throws IOException {
            count += byteCount;
            return this;
        }

        @Override
        public long writeAll(Source source) throws IOException {
            throw new RuntimeException("not impl");
        }

        @Override
        public BufferedSink write(Source source, long byteCount) throws IOException {
            count += byteCount;
            return this;
        }

        @Override
        public BufferedSink writeUtf8(String string) throws IOException {
            count += string.getBytes("utf-8").length;
            return this;
        }

        @Override
        public BufferedSink writeUtf8(String string, int beginIndex, int endIndex) throws IOException {
            count += endIndex - beginIndex;
            return this;
        }

        @Override
        public BufferedSink writeUtf8CodePoint(int codePoint) throws IOException {
            throw new RuntimeException("not impl");
        }

        @Override
        public BufferedSink writeString(String string, Charset charset) throws IOException {
            count += string.getBytes(charset).length;
            return this;
        }

        @Override
        public BufferedSink writeString(String string, int beginIndex, int endIndex, Charset charset) throws IOException {
            throw new RuntimeException("not impl");
        }

        @Override
        public BufferedSink writeByte(int b) throws IOException {
            count++;
            return this;
        }

        @Override
        public BufferedSink writeShort(int s) throws IOException {
            count += 2;
            return this;
        }

        @Override
        public BufferedSink writeShortLe(int s) throws IOException {
            count += 2;
            return this;
        }

        @Override
        public BufferedSink writeInt(int i) throws IOException {
            count += 4;
            return this;
        }

        @Override
        public BufferedSink writeIntLe(int i) throws IOException {
            count += 4;
            return this;
        }

        @Override
        public BufferedSink writeLong(long v) throws IOException {
            throw new RuntimeException("not impl");
        }

        @Override
        public BufferedSink writeLongLe(long v) throws IOException {
            throw new RuntimeException("not impl");
        }

        @Override
        public BufferedSink writeDecimalLong(long v) throws IOException {
            throw new RuntimeException("not impl");
        }

        @Override
        public BufferedSink writeHexadecimalUnsignedLong(long v) throws IOException {
            throw new RuntimeException("not impl");
        }

        @Override
        public BufferedSink emitCompleteSegments() throws IOException {
            return this;
        }

        @Override
        public BufferedSink emit() throws IOException {
            return this;
        }

        @Override
        public OutputStream outputStream() {
            return null;
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public Timeout timeout() {
            return null;
        }

        @Override
        public void close() throws IOException {
        }
    }
}
