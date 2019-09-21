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

import android.text.TextUtils;

import java.util.Date;
import java.util.HashMap;

public final class DbModel {

    /**
     * key: columnName
     * value: valueStr
     */
    private final HashMap<String, String> dataMap = new HashMap<String, String>();

    public String getString(String columnName) {
        return dataMap.get(columnName);
    }

    public int getInt(String columnName, int defaultValue) {
        String value = dataMap.get(columnName);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        } else {
            try {
                return Integer.valueOf(value);
            } catch (Throwable ex) {
                return defaultValue;
            }
        }
    }

    public boolean getBoolean(String columnName) {
        String value = dataMap.get(columnName);
        if (value != null) {
            return value.length() == 1 ? "1".equals(value) : Boolean.valueOf(value);
        }
        return false;
    }

    public double getDouble(String columnName, double defaultValue) {
        String value = dataMap.get(columnName);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        } else {
            try {
                return Double.valueOf(value);
            } catch (Throwable ex) {
                return defaultValue;
            }
        }
    }

    public float getFloat(String columnName, float defaultValue) {
        String value = dataMap.get(columnName);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        } else {
            try {
                return Float.valueOf(value);
            } catch (Throwable ex) {
                return defaultValue;
            }
        }
    }

    public long getLong(String columnName, long defaultValue) {
        String value = dataMap.get(columnName);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        } else {
            try {
                return Long.valueOf(value);
            } catch (Throwable ex) {
                return defaultValue;
            }
        }
    }

    public Date getDate(String columnName, long defaultTime) {
        return new Date(getLong(columnName, defaultTime));
    }

    public java.sql.Date getSqlDate(String columnName, long defaultTime) {
        return new java.sql.Date(getLong(columnName, defaultTime));
    }

    public void add(String columnName, String valueStr) {
        dataMap.put(columnName, valueStr);
    }

    /**
     * @return key: columnName
     */
    public HashMap<String, String> getDataMap() {
        return dataMap;
    }

    /**
     * 列数据是否空
     */
    public boolean isEmpty(String columnName) {
        return TextUtils.isEmpty(dataMap.get(columnName));
    }
}
