package org.xutils.sample.http;

import org.xutils.http.annotation.HttpResponse;

/**
 * Created by wyouflf on 15/11/5.
 * json 返回值示例, 如果JsonDemoResponse作为Callback的泛型,
 * 那么xUtils将自动调用JsonResponseParser将字符串转换为JsonDemoResponse.
 *
 * @HttpResponse 注解结合 ResponseParser接口非常适合做json, xml, protobuf等类型数据的解析,
 * 当然也可以通过PrepareCallback来实现自定义数据类型的转换, 参考:
 * {@link org.xutils.http.loader.LoaderFactory}
 * 和 {@link org.xutils.common.Callback.PrepareCallback}.
 * LoaderFactory提供PrepareCallback第一个泛型参数类型的自动转换,
 * 第二个泛型参数需要在prepare方法中实现.
 * (LoaderFactory中已经默认提供了部分常用类型的转换实现, 其他类型需要自己注册.)
 */
@HttpResponse(parser = JsonResponseParser.class)
public class JsonDemoResponse {
    // some properties

    private String test;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    @Override
    public String toString() {
        return test;
    }
}
