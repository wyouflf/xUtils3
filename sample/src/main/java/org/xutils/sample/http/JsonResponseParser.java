package org.xutils.sample.http;

import org.xutils.http.app.ResponseParser;
import org.xutils.http.request.UriRequest;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wyouflf on 15/11/5.
 * 添加在params对象的注解参数中.
 * 如果实现 InputStreamResponseParser, 可实现自定义流数据转换, 例如用于转换protobuff对象.
 */
public class JsonResponseParser implements ResponseParser {

    @Override
    public void checkResponse(UriRequest request) throws Throwable {
        // custom check ?
        // get headers ?
    }

    /**
     * 转换result为resultType类型的对象
     *
     * @param resultType  返回值类型(可能带有泛型信息)
     * @param resultClass 返回值类型
     * @param result      字符串数据
     * @return
     * @throws Throwable
     */
    @Override
    public Object parse(Type resultType, Class<?> resultClass, String result) throws Throwable {
        // TODO: json to java bean
        if (resultClass == List.class) {
            // 这里只是个示例, 不做json转换.
            List<JsonDemoResponse> list = new ArrayList<JsonDemoResponse>();
            JsonDemoResponse baiduResponse = new JsonDemoResponse();
            baiduResponse.setTest(result);
            list.add(baiduResponse);
            return list;
            // fastJson 解析示例:
            // return JSON.parseArray(result, (Class<?>) ParameterizedTypeUtil.getParameterizedType(resultType, List.class, 0));
        } else {
            // 这里只是个示例, 不做json转换.
            JsonDemoResponse baiduResponse = new JsonDemoResponse();
            baiduResponse.setTest(result);
            return baiduResponse;
            // fastjson 解析示例:
            // return JSON.parseObject(result, resultType);
        }

    }

    // 如果实现InputStreamResponseParser，可以在这里转换pb数据.
    //@Override
    //public Object parse(Type resultType, Class<?> resultClass, InputStream result) throws Throwable {
    //}
}
