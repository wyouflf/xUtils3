package org.xutils.http.body;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by wyouflf on 15/8/13.
 */
public class FileBody extends InputStreamBody {

    public FileBody(File file) throws IOException {
        super(new FileInputStream(file), getFileContentType(file));
    }

    /*package*/
    static String getFileContentType(File file) {
        String filename = file.getName();
        String contentType = HttpURLConnection.guessContentTypeFromName(filename);
        if (null == contentType) {
            contentType = "application/octet-stream";
        }
        return contentType.replaceFirst("\\/jpg$", "/jpeg");
    }
}
