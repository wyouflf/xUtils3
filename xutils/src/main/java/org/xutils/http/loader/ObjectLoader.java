package org.xutils.http.loader;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.common.util.ParameterizedTypeUtil;
import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpResponse;
import org.xutils.http.app.ResponseParser;
import org.xutils.http.request.UriRequest;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

/**
 * Created by lei.jiao on 2014/6/27.
 * 其他对象的下载转换.
 * 使用类型上的@HttpResponse注解信息进行数据转换.
 */
/*package*/ class ObjectLoader extends Loader<Object> {

    private final Type objectType;
    private final Class<?> objectClass;
    private final ResponseParser parser;
    private final Loader<?> innerLoader;

    public ObjectLoader(Type objectType) {
        this.objectType = objectType;

        // check loadType & resultType
        if (objectType instanceof ParameterizedType) {
            objectClass = (Class<?>) ((ParameterizedType) objectType).getRawType();
        } else if (objectType instanceof TypeVariable) {
            throw new IllegalArgumentException(
                    "not support callback type " + objectType.toString());
        } else {
            objectClass = (Class<?>) objectType;
        }

        HttpResponse response = null;
        Type itemType = objectType;
        if (List.class.equals(objectClass)) {
            itemType = ParameterizedTypeUtil.getParameterizedType(this.objectType, List.class, 0);
            Class<?> itemClass = null;
            if (itemType instanceof ParameterizedType) {
                itemClass = (Class<?>) ((ParameterizedType) itemType).getRawType();
            } else if (itemType instanceof TypeVariable) {
                throw new IllegalArgumentException(
                        "not support callback type " + itemType.toString());
            } else {
                itemClass = (Class<?>) itemType;
            }

            response = itemClass.getAnnotation(HttpResponse.class);
        } else {
            response = objectClass.getAnnotation(HttpResponse.class);
        }
        if (response != null) {
            try {
                Class<? extends ResponseParser> parserCls = response.parser();
                this.parser = parserCls.newInstance();
                this.innerLoader = LoaderFactory.getLoader(
                        ParameterizedTypeUtil.getParameterizedType(parserCls, ResponseParser.class, 0));
            } catch (Throwable ex) {
                throw new RuntimeException("create parser error", ex);
            }
        } else {
            throw new IllegalArgumentException("not found @HttpResponse from " + itemType);
        }

        if (innerLoader instanceof ObjectLoader) {
            throw new IllegalArgumentException("not support callback type " + itemType);
        }
    }

    @Override
    public Loader<Object> newInstance() {
        throw new IllegalAccessError("use constructor create ObjectLoader.");
    }

    @Override
    public void setParams(final RequestParams params) {
        this.innerLoader.setParams(params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object load(final UriRequest request) throws Throwable {
        request.setResponseParser(parser);
        Object innerLoaderResult = innerLoader.load(request);
        return parser.parse(objectType, objectClass, innerLoaderResult);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable {
        Object innerLoaderResult = innerLoader.loadFromCache(cacheEntity);
        return parser.parse(objectType, objectClass, innerLoaderResult);
    }

    @Override
    public void save2Cache(UriRequest request) {
        innerLoader.save2Cache(request);
    }
}