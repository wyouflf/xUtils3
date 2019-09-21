package org.xutils.http.loader;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Author: wyouflf
 * Time: 2014/05/26
 */
public final class LoaderFactory {

    private LoaderFactory() {
    }

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
        converterHashMap.put(InputStream.class, new InputStreamLoader());

        BooleanLoader booleanLoader = new BooleanLoader();
        converterHashMap.put(boolean.class, booleanLoader);
        converterHashMap.put(Boolean.class, booleanLoader);

        IntegerLoader integerLoader = new IntegerLoader();
        converterHashMap.put(int.class, integerLoader);
        converterHashMap.put(Integer.class, integerLoader);
    }

    public static Loader<?> getLoader(Type type) {
        Loader<?> result = converterHashMap.get(type);
        if (result == null) {
            result = new ObjectLoader(type);
        } else {
            result = result.newInstance();
        }
        return result;
    }

    public static <T> void registerLoader(Type type, Loader<T> loader) {
        converterHashMap.put(type, loader);
    }
}
