package org.xutils.http.app;

import com.squareup.okhttp.Response;

/**
 * Created by wyouflf on 15/8/4.
 */
public interface ResponseParser {

    void checkResponse(Response response) throws Throwable;

    Object parse(Class<?> resultType, String result) throws Throwable;
}
