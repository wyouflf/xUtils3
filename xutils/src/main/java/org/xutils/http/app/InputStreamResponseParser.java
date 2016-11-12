package org.xutils.http.app;

import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * Created by wyouflf on 16/2/2.
 */
public abstract class InputStreamResponseParser implements ResponseParser {

    public abstract Object parse(Type resultType, Class<?> resultClass, InputStream result) throws Throwable;

    /**
     * Deprecated, see {@link InputStreamResponseParser#parse(Type, Class, InputStream)}
     *
     * @throws Throwable
     */
    @Override
    @Deprecated
    public final Object parse(Type resultType, Class<?> resultClass, String result) throws Throwable {
        return null;
    }
}
