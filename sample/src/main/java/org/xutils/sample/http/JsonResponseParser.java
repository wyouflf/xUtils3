package org.xutils.sample.http;

import org.xutils.http.app.ResponseParser;
import org.xutils.http.request.UriRequest;

import java.lang.reflect.Type;

/**
 * Created by wyouflf on 15/11/5.
 */
public class JsonResponseParser implements ResponseParser {

    @Override
    public void checkResponse(UriRequest request) throws Throwable {
        // custom check ?
        // check header ?
    }

    @Override
    public Object parse(Type resultType, Class<?> resultClass, String result) throws Throwable {
        // TODO: json to java bean
        return null;
    }
}
