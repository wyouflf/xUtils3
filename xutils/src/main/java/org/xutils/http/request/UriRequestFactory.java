package org.xutils.http.request;

import org.xutils.common.util.LogUtil;
import org.xutils.http.RequestParams;
import org.xutils.http.app.RequestTracker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

/**
 * Created by wyouflf on 15/11/4.
 * Uri请求创建工厂
 */
public final class UriRequestFactory {

    private static Class<? extends RequestTracker> defaultTrackerCls;
    private static Class<? extends AssetsRequest> assetsRequestCls;

    private UriRequestFactory() {
    }

    public static UriRequest getUriRequest(RequestParams params, Type loadType) throws Throwable {
        String uri = params.getUri();
        if (uri.startsWith("http")) {
            return new HttpRequest(params, loadType);
        } else if (uri.startsWith("assets://")) {
            if (assetsRequestCls != null) {
                Constructor<? extends AssetsRequest> constructor
                        = assetsRequestCls.getConstructor(RequestParams.class, Class.class);
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

    public static void registerDefaultTrackerClass(Class<? extends RequestTracker> trackerCls) {
        UriRequestFactory.defaultTrackerCls = trackerCls;
    }

    public static RequestTracker getDefaultTracker() {
        try {
            return defaultTrackerCls == null ? null : defaultTrackerCls.newInstance();
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
        return null;
    }

    public static void registerAssetsRequestClass(Class<? extends AssetsRequest> assetsRequestCls) {
        UriRequestFactory.assetsRequestCls = assetsRequestCls;
    }
}
