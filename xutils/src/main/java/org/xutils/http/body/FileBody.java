package org.xutils.http.body;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by wyouflf on 15/8/13.
 */
public class FileBody extends InputStreamBody {

    private File file;
    private String contentType;

    public FileBody(File file) throws IOException {
        this(file, null);
    }

    public FileBody(File file, String contentType) throws IOException {
        super(new FileInputStream(file));
        this.file = file;
        this.contentType = contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        if (TextUtils.isEmpty(contentType)) {
            contentType = getFileContentType(file);
        }
        return contentType;
    }

    public static String getFileContentType(File file) {
        String filename = file.getName();
        String contentType = HttpURLConnection.guessContentTypeFromName(filename);
        if (TextUtils.isEmpty(contentType)) {
            contentType = "application/octet-stream";
        } else {
            contentType = contentType.replaceFirst("\\/jpg$", "/jpeg");
        }
        return contentType;
    }
}
