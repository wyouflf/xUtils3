package org.xutils.sample.http;

import org.xutils.common.util.LogUtil;
import org.xutils.http.app.ResponseParser;
import org.xutils.http.request.UriRequest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wyouflf on 15/11/5.
 * 添加在params对象的注解参数中.
 * 如果泛型为 byte[] 或 InputStream, 可以方便的转换protobuf对象.
 */
public class JsonResponseParser implements ResponseParser<String> {

    @Override
    public void beforeRequest(UriRequest request) throws Throwable {
        // custom check params?
        LogUtil.d(request.getParams().toString());
    }

    @Override
    public void afterRequest(UriRequest request) throws Throwable {
        // custom check response Headers?
        LogUtil.d("response code:" + request.getResponseCode());
    }

    /**
     * 转换result为resultType类型的对象
     *
     * @param resultType  返回值类型(可能带有泛型信息)
     * @param resultClass 返回值类型
     * @param result      网络返回数据(支持String, byte[], JSONObject, JSONArray, InputStream)
     * @return 请求结果, 类型为resultType
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
}
