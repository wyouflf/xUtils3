package org.xutils.http.loader;

import android.text.TextUtils;

import org.json.JSONObject;
import org.xutils.cache.DiskCacheEntity;
import org.xutils.common.util.IOUtil;
import org.xutils.http.RequestParams;
import org.xutils.http.request.UriRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Author: wyouflf
 * Time: 2014/06/16
 */
/*package*/ class MapLoader extends Loader<Map<String, Object>> {

    private String charset = "UTF-8";
    private String resultStr = null;

    @Override
    public Loader<Map<String, Object>> newInstance() {
        return new MapLoader();
    }

    @Override
    public void setParams(final RequestParams params) {
        if (params != null) {
            String charset = params.getCharset();
            if (!TextUtils.isEmpty(charset)) {
                this.charset = charset;
            }
        }
    }

    @Override
    public Map<String, Object> load(final InputStream in) throws Throwable {
        resultStr = IOUtil.readStr(in, charset);
        return json2Map(resultStr);
    }

    @Override
    public Map<String, Object> load(final UriRequest request) throws Throwable {
        request.sendRequest();
        return this.load(request.getInputStream());
    }

    @Override
    public Map<String, Object> loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable {
        if (cacheEntity != null) {
            String text = cacheEntity.getTextContent();
            if (!TextUtils.isEmpty(text)) {
                return json2Map(text);
            }
        }

        return null;
    }

    @Override
    public void save2Cache(UriRequest request) {
        saveStringCache(request, resultStr);
    }

    private static Map<String, Object> json2Map(String jsonStr) throws Throwable {
        JSONObject jsonObject = new JSONObject(jsonStr);
        Map<String, Object> valueMap = new HashMap<String, Object>();
        Iterator<String> keysItr = jsonObject.keys();
        if (keysItr != null) {
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = jsonObject.get(key);
                valueMap.put(key, value);
            }
        }
        return valueMap;
    }
}
