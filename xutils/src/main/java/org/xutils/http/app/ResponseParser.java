package org.xutils.http.app;


import java.lang.reflect.Type;

/**
 * Created by wyouflf on 15/8/4.
 * {@link org.xutils.http.annotation.HttpResponse} 注解的返回值转换模板
 *
 * @param <ResponseDataType> 支持String, byte[], JSONObject, JSONArray, InputStream
 */
public interface ResponseParser<ResponseDataType> extends RequestInterceptListener {

    /**
     * 转换result为resultType类型的对象
     *
     * @param resultType  返回值类型(可能带有泛型信息)
     * @param resultClass 返回值类型
     * @param result      网络返回数据(支持String, byte[], JSONObject, JSONArray, InputStream)
     * @return 请求结果, 类型为resultType
     */
    Object parse(Type resultType, Class<?> resultClass, ResponseDataType result) throws Throwable;
}
