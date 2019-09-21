package org.xutils.http.request;

import android.content.Context;

import org.xutils.http.RequestParams;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Created by wyouflf on 15/11/4.
 * Assets资源文件请求
 */
public class AssetsRequest extends ResRequest {

    public AssetsRequest(RequestParams params, Type loadType) throws Throwable {
        super(params, loadType);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            Context context = params.getContext();
            String assetsPath = queryUrl.replace("assets://", "");
            inputStream = context.getResources().getAssets().open(assetsPath);
            contentLength = inputStream.available();
        }
        return inputStream;
    }
}
