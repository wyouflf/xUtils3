package org.xutils.sample.http;

import org.xutils.http.annotation.HttpResponse;

/**
 * Created by wyouflf on 15/11/5.
 * json 返回值示例
 */
@HttpResponse(parser = JsonResponseParser.class)
public class BaiduResponse {
    // some properties
}
