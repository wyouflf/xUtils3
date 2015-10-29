package org.xutils.http.loader;


import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.http.RequestParams;
import org.xutils.http.app.ResponseTracker;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: wyouflf
 * Time: 2014/05/26
 */
public final class LoaderFactory {

    private LoaderFactory() {
    }

    private static ResponseTracker defaultTracker;

    /**
     * key: loadType
     */
    private static final HashMap<Class<?>, ResponseTracker> trackerHashMap = new HashMap<Class<?>, ResponseTracker>();

    /**
     * key: loadType
     */
    private static final HashMap<Class<?>, Loader> converterHashMap = new HashMap<Class<?>, Loader>();

    static {
        converterHashMap.put(Map.class, new MapLoader());
        converterHashMap.put(JSONObject.class, new JSONObjectLoader());
        converterHashMap.put(JSONArray.class, new JSONArrayLoader());
        converterHashMap.put(String.class, new StringLoader());
        converterHashMap.put(File.class, new FileLoader());
        converterHashMap.put(byte[].class, new ByteArrayLoader());
        BooleanLoader booleanLoader = new BooleanLoader();
        converterHashMap.put(boolean.class, booleanLoader);
        converterHashMap.put(Boolean.class, booleanLoader);
        IntegerLoader integerLoader = new IntegerLoader();
        converterHashMap.put(int.class, integerLoader);
        converterHashMap.put(Integer.class, integerLoader);
    }

    @SuppressWarnings("unchecked")
    public static Loader<?> getLoader(Class<?> type, RequestParams params) {
        Loader<?> result = converterHashMap.get(type);
        if (result == null) {
            result = new ObjectLoader(type);
        } else {
            result = result.newInstance();
            ResponseTracker tracker = trackerHashMap.get(type);
            result.setResponseTracker(tracker == null ? defaultTracker : tracker);
        }
        result.setParams(params);
        return result;
    }

    public static <T> void registerLoader(Class<T> type, Loader<T> loader) {
        converterHashMap.put(type, loader);
    }

    public static void registerDefaultTracker(ResponseTracker tracker) {
        defaultTracker = tracker;
    }

    public static void registerTracker(Class<?> type, ResponseTracker tracker) {
        trackerHashMap.put(type, tracker);
    }
}
