package org.xutils.http.body;


import android.text.TextUtils;

import org.xutils.common.Callback;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.KeyValue;
import org.xutils.http.ProgressHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: wyouflf
 * Time: 2014/05/30
 */
public class MultipartBody implements ProgressBody {

    private static byte[] BOUNDARY_PREFIX_BYTES = "--------7da3d81520810".getBytes();
    private static byte[] END_BYTES = "\r\n".getBytes();
    private static byte[] TWO_DASHES_BYTES = "--".getBytes();
    private byte[] boundaryPostfixBytes;
    private String contentType; // multipart/subtype; boundary=xxx...
    private String charset = "UTF-8";

    private List<KeyValue> multipartParams;
    private long total = 0;
    private long current = 0;

    public MultipartBody(List<KeyValue> multipartParams, String charset) {
        if (!TextUtils.isEmpty(charset)) {
            this.charset = charset;
        }
        this.multipartParams = multipartParams;
        generateContentType();

        // calc total
        CounterOutputStream counter = new CounterOutputStream();
        try {
            this.writeTo(counter);
            this.total = counter.total.get();
        } catch (IOException e) {
            this.total = -1;
        }
    }

    private ProgressHandler callBackHandler;

    @Override
    public void setProgressHandler(ProgressHandler progressHandler) {
        this.callBackHandler = progressHandler;
    }

    private void generateContentType() {
        String boundaryPostfix = Double.toHexString(Math.random() * 0xFFFF);
        boundaryPostfixBytes = boundaryPostfix.getBytes();
        contentType = "multipart/form-data; boundary=" + new String(BOUNDARY_PREFIX_BYTES) + boundaryPostfix;
    }

    @Override
    public long getContentLength() {
        return total;
    }

    /**
     * only change subType:
     * "multipart/subType; boundary=xxx..."
     *
     * @param subType "form-data" or "related"
     */
    @Override
    public void setContentType(String subType) {
        int index = contentType.indexOf(";");
        this.contentType = "multipart/" + subType + contentType.substring(index);
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {

        if (callBackHandler != null && !callBackHandler.updateProgress(total, current, true)) {
            throw new Callback.CancelledException("upload stopped!");
        }

        for (KeyValue kv : multipartParams) {
            String name = kv.key;
            Object value = kv.value;
            if (!TextUtils.isEmpty(name) && value != null) {
                writeEntry(out, name, value);
            }
        }
        writeLine(out, TWO_DASHES_BYTES, BOUNDARY_PREFIX_BYTES, boundaryPostfixBytes, TWO_DASHES_BYTES);
        out.flush();

        if (callBackHandler != null) {
            callBackHandler.updateProgress(total, total, true);
        }
    }

    /**
     * 写入multipart中的一项
     *
     * @param out
     * @param name
     * @param value
     * @throws IOException
     */
    private void writeEntry(OutputStream out, String name, Object value) throws IOException {
        writeLine(out, TWO_DASHES_BYTES, BOUNDARY_PREFIX_BYTES, boundaryPostfixBytes);

        String fileName = "";
        String contentType = null;
        if (value instanceof BodyItemWrapper) {
            BodyItemWrapper wrapper = (BodyItemWrapper) value;
            value = wrapper.getValue();
            fileName = wrapper.getFileName();
            contentType = wrapper.getContentType();
        }

        if (value instanceof File) {
            File file = (File) value;
            if (TextUtils.isEmpty(fileName)) {
                fileName = file.getName();
            }
            if (TextUtils.isEmpty(contentType)) {
                contentType = FileBody.getFileContentType(file);
            }
            writeLine(out, buildContentDisposition(name, fileName, charset));
            writeLine(out, buildContentType(value, contentType, charset));
            writeLine(out); // 内容前空一行
            writeFile(out, file);
            writeLine(out);
        } else {
            writeLine(out, buildContentDisposition(name, fileName, charset));
            writeLine(out, buildContentType(value, contentType, charset));
            writeLine(out); // 内容前空一行
            if (value instanceof InputStream) {
                writeStreamAndCloseIn(out, (InputStream) value);
                writeLine(out);
            } else {
                byte[] content;
                if (value instanceof byte[]) {
                    content = (byte[]) value;
                } else {
                    content = String.valueOf(value).getBytes(charset);
                }
                writeLine(out, content);
                current += content.length;
                if (callBackHandler != null && !callBackHandler.updateProgress(total, current, false)) {
                    throw new Callback.CancelledException("upload stopped!");
                }
            }
        }
    }

    private void writeLine(OutputStream out, byte[]... bs) throws IOException {
        if (bs != null) {
            for (byte[] b : bs) {
                out.write(b);
            }
        }
        out.write(END_BYTES);
    }

    private void writeFile(OutputStream out, File file) throws IOException {
        if (out instanceof CounterOutputStream) {
            ((CounterOutputStream) out).addFile(file);
        } else {
            writeStreamAndCloseIn(out, new FileInputStream(file));
        }
    }

    private void writeStreamAndCloseIn(OutputStream out, InputStream in) throws IOException {
        if (out instanceof CounterOutputStream) {
            ((CounterOutputStream) out).addStream(in);
        } else {
            try {
                int len;
                byte[] buf = new byte[1024];
                while ((len = in.read(buf)) >= 0) {
                    out.write(buf, 0, len);
                    current += len;
                    if (callBackHandler != null && !callBackHandler.updateProgress(total, current, false)) {
                        throw new Callback.CancelledException("upload stopped!");
                    }
                }
            } finally {
                IOUtil.closeQuietly(in);
            }
        }
    }

    private static byte[] buildContentDisposition(String name, String fileName, String charset) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder("Content-Disposition: form-data");
        result.append("; name=\"").append(name.replace("\"", "\\\"")).append("\"");
        if (!TextUtils.isEmpty(fileName)) {
            result.append("; filename=\"").append(fileName.replace("\"", "\\\"")).append("\"");
        }
        return result.toString().getBytes(charset);
    }

    private static byte[] buildContentType(Object value, String contentType, String charset) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder("Content-Type: ");
        if (TextUtils.isEmpty(contentType)) {
            if (value instanceof String) {
                contentType = "text/plain; charset=" + charset;
            } else {
                contentType = "application/octet-stream";
            }
        } else {
            contentType = contentType.replaceFirst("\\/jpg$", "/jpeg");
        }
        result.append(contentType);
        return result.toString().getBytes(charset);
    }

    private class CounterOutputStream extends OutputStream {

        final AtomicLong total = new AtomicLong(0L);

        public CounterOutputStream() {
        }

        public void addFile(File file) {
            if (total.get() == -1L) return;
            total.addAndGet(file.length());
        }

        public void addStream(InputStream inputStream) {
            if (total.get() == -1L) return;
            long length = InputStreamBody.getInputStreamLength(inputStream);
            if (length > 0) {
                total.addAndGet(length);
            } else {
                total.set(-1L);
            }
        }

        @Override
        public void write(int oneByte) throws IOException {
            if (total.get() == -1L) return;
            total.incrementAndGet();
        }

        @Override
        public void write(byte[] buffer) throws IOException {
            if (total.get() == -1L) return;
            total.addAndGet(buffer.length);
        }

        @Override
        public void write(byte[] buffer, int offset, int count) throws IOException {
            if (total.get() == -1L) return;
            total.addAndGet(count);
        }
    }
}
