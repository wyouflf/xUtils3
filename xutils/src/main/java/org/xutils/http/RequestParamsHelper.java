package org.xutils.http;

import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.util.LogUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Created by wyouflf on 16/1/23.
 */
/*package*/ final class RequestParamsHelper {

    private static final ClassLoader BOOT_CL = String.class.getClassLoader();

    private RequestParamsHelper() {
    }

    /*package*/ interface ParseKVListener {
        void onParseKV(String name, Object value);
    }

    /*package*/
    static void parseKV(Object entity, Class<?> type, ParseKVListener listener) {
        if (type == null || type == RequestParams.class || type == Object.class) {
            return;
        } else {
            ClassLoader cl = type.getClassLoader();
            if (cl == null || cl == BOOT_CL) {
                return;
            }
        }

        Field[] fields = type.getDeclaredFields();
        if (fields != null && fields.length > 0) {
            for (Field field : fields) {
                if (!Modifier.isTransient(field.getModifiers())
                        && field.getType() != Parcelable.Creator.class) {
                    field.setAccessible(true);
                    try {
                        String name = field.getName();
                        Object value = field.get(entity);
                        if (value != null) {
                            listener.onParseKV(name, value);
                        }
                    } catch (IllegalAccessException ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                }
            }
        }

        parseKV(entity, type.getSuperclass(), listener);
    }

    /*package*/
    static Object parseJSONObject(Object value) {
        Object result = value;
        ClassLoader cl = value.getClass().getClassLoader();
        if (cl != null && cl != BOOT_CL) {
            final JSONObject jo = new JSONObject();
            parseKV(value, value.getClass(), new ParseKVListener() {
                @Override
                public void onParseKV(String name, Object value) {
                    try {
                        value = parseJSONObject(value);
                        jo.put(name, value);
                    } catch (JSONException ex) {
                        LogUtil.e(ex.getMessage(), ex);
                    }
                }
            });
            result = jo;
        }
        return result;
    }

}
