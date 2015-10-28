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
    private HashMap<String, String> dataMap = new HashMap<String, String>();

    public String getString(String columnName) {
        return dataMap.get(columnName);
    }

    public int getInt(String columnName) {
        return Integer.valueOf(dataMap.get(columnName));
    }

    public boolean getBoolean(String columnName) {
        String value = dataMap.get(columnName);
        if (value != null) {
            return value.length() == 1 ? "1".equals(value) : Boolean.valueOf(value);
        }
        return false;
    }

    public double getDouble(String columnName) {
        return Double.valueOf(dataMap.get(columnName));
    }

    public float getFloat(String columnName) {
        return Float.valueOf(dataMap.get(columnName));
    }

    public long getLong(String columnName) {
        return Long.valueOf(dataMap.get(columnName));
    }

    public Date getDate(String columnName) {
        long date = Long.valueOf(dataMap.get(columnName));
        return new Date(date);
    }

    public java.sql.Date getSqlDate(String columnName) {
        long date = Long.valueOf(dataMap.get(columnName));
        return new java.sql.Date(date);
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
     * @param columnName
     * @return
     */
    public boolean isEmpty(String columnName) {
        return TextUtils.isEmpty(dataMap.get(columnName));
    }
}
