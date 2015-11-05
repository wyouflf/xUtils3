/*
 * Copyright (c) 2013. wyouflf (wyouflf@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xutils.db.table;

import org.xutils.common.util.LogUtil;
import org.xutils.db.converter.ColumnConverter;
import org.xutils.db.converter.ColumnConverterFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;

public final class ColumnUtils {

    private ColumnUtils() {
    }

    private static final HashSet<Class<?>> BOOLEAN_TYPES = new HashSet<Class<?>>(2);
    private static final HashSet<Class<?>> INTEGER_TYPES = new HashSet<Class<?>>(2);
    private static final HashSet<Class<?>> AUTO_INCREMENT_TYPES = new HashSet<Class<?>>(4);

    static {
        BOOLEAN_TYPES.add(boolean.class);
        BOOLEAN_TYPES.add(Boolean.class);

        INTEGER_TYPES.add(int.class);
        INTEGER_TYPES.add(Integer.class);

        AUTO_INCREMENT_TYPES.addAll(INTEGER_TYPES);
        AUTO_INCREMENT_TYPES.add(long.class);
        AUTO_INCREMENT_TYPES.add(Long.class);
    }

    public static boolean isAutoIdType(Class<?> fieldType) {
        return AUTO_INCREMENT_TYPES.contains(fieldType);
    }

    public static boolean isInteger(Class<?> fieldType) {
        return INTEGER_TYPES.contains(fieldType);
    }

    public static boolean isBoolean(Class<?> fieldType) {
        return BOOLEAN_TYPES.contains(fieldType);
    }

    /* package */
    static Method findGetMethod(Class<?> entityType, Field field) {
        if (Object.class.equals(entityType)) return null;

        String fieldName = field.getName();
        Method getMethod = null;
        if (isBoolean(field.getType())) {
            getMethod = findBooleanGetMethod(entityType, fieldName);
        }
        if (getMethod == null) {
            String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            try {
                getMethod = entityType.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                LogUtil.d(methodName + " not exist");
            }
        }

        if (getMethod == null) {
            return findGetMethod(entityType.getSuperclass(), field);
        }
        return getMethod;
    }

    /* package */
    static Method findSetMethod(Class<?> entityType, Field field) {
        if (Object.class.equals(entityType)) return null;

        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        Method setMethod = null;
        if (isBoolean(fieldType)) {
            setMethod = findBooleanSetMethod(entityType, fieldName, fieldType);
        }
        if (setMethod == null) {
            String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            try {
                setMethod = entityType.getDeclaredMethod(methodName, fieldType);
            } catch (NoSuchMethodException e) {
                LogUtil.d(methodName + " not exist");
            }
        }

        if (setMethod == null) {
            return findSetMethod(entityType.getSuperclass(), field);
        }
        return setMethod;
    }

    @SuppressWarnings("unchecked")
    public static Object convert2DbValueIfNeeded(final Object value) {
        Object result = value;
        if (value != null) {
            Class<?> valueType = value.getClass();
            ColumnConverter converter = ColumnConverterFactory.getColumnConverter(valueType);
            result = converter.fieldValue2DbValue(value);
        }
        return result;
    }

    private static Method findBooleanGetMethod(Class<?> entityType, final String fieldName) {
        String methodName = null;
        if (fieldName.startsWith("is")) {
            methodName = fieldName;
        } else {
            methodName = "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        }
        try {
            return entityType.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            LogUtil.d(methodName + " not exist");
        }
        return null;
    }

    private static Method findBooleanSetMethod(Class<?> entityType, final String fieldName, Class<?> fieldType) {
        String methodName = null;
        if (fieldName.startsWith("is")) {
            methodName = "set" + fieldName.substring(2, 3).toUpperCase() + fieldName.substring(3);
        } else {
            methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        }
        try {
            return entityType.getDeclaredMethod(methodName, fieldType);
        } catch (NoSuchMethodException e) {
            LogUtil.d(methodName + " not exist");
        }
        return null;
    }

}
