package org.xutils.http.loader;


import org.json.JSONArray;
import org.json.JSONObject;
import org.xutils.http.RequestParams;
import org.xutils.http.app.RequestTracker;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Author: wyouflf
 * Time: 2014/05/26
 */
public final class LoaderFactory {

    private LoaderFactory() {
    }

    private static RequestTracker defaultTracker;

    /**
     * key: loadType
     */
    private static final HashMap<Type, RequestTracker> trackerHashMap = new HashMap<Type, RequestTracker>();

    /**
     * key: loadType
     */
    private static final HashMap<Type, Loader> converterHashMap = new HashMap<Type, Loader>();

    static {
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
    public static Loader<?> getLoader(Type type, RequestParams params) {
        Loader<?> result = converterHashMap.get(type);
        if (result == null) {
            result = new ObjectLoader(type);
        } else {
            result = result.newInstance();
        }
        result.setParams(params);
        RequestTracker tracker = trackerHashMap.get(type);
        result.setResponseTracker(tracker == null ? defaultTracker : tracker);
        return result;
    }

    public static <T> void registerLoader(Type type, Loader<T> loader) {
        converterHashMap.put(type, loader);
    }

    public static void registerDefaultTracker(RequestTracker tracker) {
        defaultTracker = tracker;
    }

    public static void registerTracker(Type type, RequestTracker tracker) {
        trackerHashMap.put(type, tracker);
    }
}
