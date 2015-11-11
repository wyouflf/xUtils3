package org.xutils.sample.http;

import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpRequest;
import org.xutils.http.app.DefaultParamsBuilder;

/**
 * Created by wyouflf on 15/11/4.
 */
@HttpRequest(
        host = "https://www.baidu.com",
        path = "s",
        builder = DefaultParamsBuilder.class/*可选参数, 控制参数构建过程, 定义参数签名, SSL证书等*/)
public class BaiduParams extends RequestParams {
    public String wd;
    //public long timestamp = System.currentTimeMillis();
    //public File uploadFile;
}
