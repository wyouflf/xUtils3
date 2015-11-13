package org.xutils.http.request;

import org.xutils.http.RequestParams;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

/**
 * Created by wyouflf on 15/11/4.
 * Uri请求创建工厂
 */
public final class UriRequestFactory {

    private static Class<? extends AssetsRequest> ASSETS_REQUEST_CLS;

    private UriRequestFactory() {
    }

    public static UriRequest getUriRequest(RequestParams params, Type loadType) throws Throwable {
        String uri = params.getUri();
        if (uri.startsWith("http")) {
            return new HttpRequest(params, loadType);
        } else if (uri.startsWith("assets://")) {
            if (ASSETS_REQUEST_CLS != null) {
                Constructor<? extends AssetsRequest> constructor
                        = ASSETS_REQUEST_CLS.getConstructor(RequestParams.class, Class.class);
                return constructor.newInstance(params, loadType);
            } else {
                return new AssetsRequest(params, loadType);
            }
        } else if (uri.startsWith("file:") || uri.startsWith("/")) {
            return new LocalFileRequest(params, loadType);
        } else {
            throw new IllegalArgumentException("The url not be support: " + uri);
        }
    }

    public static void registerAssetsRequestClass(Class<? extends AssetsRequest> assetsRequestCls) {
        ASSETS_REQUEST_CLS = assetsRequestCls;
    }
}
