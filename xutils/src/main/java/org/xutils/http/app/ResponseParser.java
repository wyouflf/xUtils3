package org.xutils.http.app;


import org.xutils.http.request.UriRequest;

import java.lang.reflect.Type;

/**
 * Created by wyouflf on 15/8/4.
 */
public interface ResponseParser {

    void checkResponse(UriRequest request) throws Throwable;

    Object parse(Type resultType, Class<?> resultClass, String result) throws Throwable;
}
