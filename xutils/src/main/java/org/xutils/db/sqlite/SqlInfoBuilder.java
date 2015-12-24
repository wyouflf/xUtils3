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

package org.xutils.db.sqlite;

import org.xutils.common.util.KeyValue;
import org.xutils.db.table.ColumnEntity;
import org.xutils.db.table.TableEntity;
import org.xutils.ex.DbException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Build "insert", "replace",，"update", "delete" and "create" sql.
 */
public final class SqlInfoBuilder {


    private static final ConcurrentHashMap<TableEntity<?>, String> INSERT_SQL_CACHE = new ConcurrentHashMap<TableEntity<?>, String>();
    private static final ConcurrentHashMap<TableEntity<?>, String> REPLACE_SQL_CACHE = new ConcurrentHashMap<TableEntity<?>, String>();

    private SqlInfoBuilder() {
    }

    //*********************************************** insert sql ***********************************************

    public static SqlInfo buildInsertSqlInfo(TableEntity<?> table, Object entity) throws DbException {

        List<KeyValue> keyValueList = entity2KeyValueList(table, entity);
        if (keyValueList.size() == 0) return null;

        SqlInfo result = new SqlInfo();
        String sql = INSERT_SQL_CACHE.get(table);
        if (sql == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("INSERT INTO ");
            builder.append("\"").append(table.getName()).append("\"");
            builder.append(" (");
            for (KeyValue kv : keyValueList) {
                builder.append("\"").append(kv.key).append("\"").append(',');
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(") VALUES (");

            int length = keyValueList.size();
            for (int i = 0; i < length; i++) {
                builder.append("?,");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(")");

            sql = builder.toString();
            result.setSql(sql);
            result.addBindArgs(keyValueList);
            INSERT_SQL_CACHE.put(table, sql);
        } else {
            result.setSql(sql);
            result.addBindArgs(keyValueList);
        }

        return result;
    }

    //*********************************************** replace sql ***********************************************

    public static SqlInfo buildReplaceSqlInfo(TableEntity<?> table, Object entity) throws DbException {

        List<KeyValue> keyValueList = entity2KeyValueList(table, entity);
        if (keyValueList.size() == 0) return null;

        SqlInfo result = new SqlInfo();
        String sql = REPLACE_SQL_CACHE.get(table);
        if (sql == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("REPLACE INTO ");
            builder.append("\"").append(table.getName()).append("\"");
            builder.append(" (");
            for (KeyValue kv : keyValueList) {
                builder.append("\"").append(kv.key).append("\"").append(',');
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(") VALUES (");

            int length = keyValueList.size();
            for (int i = 0; i < length; i++) {
                builder.append("?,");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(")");

            sql = builder.toString();
            result.setSql(sql);
            result.addBindArgs(keyValueList);
            REPLACE_SQL_CACHE.put(table, sql);
        } else {
            result.setSql(sql);
            result.addBindArgs(keyValueList);
        }

        return result;
    }

    //*********************************************** delete sql ***********************************************

    public static SqlInfo buildDeleteSqlInfo(TableEntity<?> table, Object entity) throws DbException {
        SqlInfo result = new SqlInfo();

        ColumnEntity id = table.getId();
        Object idValue = id.getColumnValue(entity);

        if (idValue == null) {
            throw new DbException("this entity[" + table.getEntityType() + "]'s id value is null");
        }
        StringBuilder builder = new StringBuilder("DELETE FROM ");
        builder.append("\"").append(table.getName()).append("\"");
        builder.append(" WHERE ").append(WhereBuilder.b(id.getName(), "=", idValue));

        result.setSql(builder.toString());

        return result;
    }

    public static SqlInfo buildDeleteSqlInfoById(TableEntity<?> table, Object idValue) throws DbException {
        SqlInfo result = new SqlInfo();

        ColumnEntity id = table.getId();

        if (idValue == null) {
            throw new DbException("this entity[" + table.getEntityType() + "]'s id value is null");
        }
        StringBuilder builder = new StringBuilder("DELETE FROM ");
        builder.append("\"").append(table.getName()).append("\"");
        builder.append(" WHERE ").append(WhereBuilder.b(id.getName(), "=", idValue));

        result.setSql(builder.toString());

        return result;
    }

    public static SqlInfo buildDeleteSqlInfo(TableEntity<?> table, WhereBuilder whereBuilder) throws DbException {
        StringBuilder builder = new StringBuilder("DELETE FROM ");
        builder.append("\"").append(table.getName()).append("\"");

        if (whereBuilder != null && whereBuilder.getWhereItemSize() > 0) {
            builder.append(" WHERE ").append(whereBuilder.toString());
        }

        return new SqlInfo(builder.toString());
    }

    //*********************************************** update sql ***********************************************

    public static SqlInfo buildUpdateSqlInfo(TableEntity<?> table, Object entity, String... updateColumnNames) throws DbException {

        List<KeyValue> keyValueList = entity2KeyValueList(table, entity);
        if (keyValueList.size() == 0) return null;

        HashSet<String> updateColumnNameSet = null;
        if (updateColumnNames != null && updateColumnNames.length > 0) {
            updateColumnNameSet = new HashSet<String>(updateColumnNames.length);
            Collections.addAll(updateColumnNameSet, updateColumnNames);
        }

        ColumnEntity id = table.getId();
        Object idValue = id.getColumnValue(entity);

        if (idValue == null) {
            throw new DbException("this entity[" + table.getEntityType() + "]'s id value is null");
        }

        SqlInfo result = new SqlInfo();
        StringBuilder builder = new StringBuilder("UPDATE ");
        builder.append("\"").append(table.getName()).append("\"");
        builder.append(" SET ");
        for (KeyValue kv : keyValueList) {
            if (updateColumnNameSet == null || updateColumnNameSet.contains(kv.key)) {
                builder.append("\"").append(kv.key).append("\"").append("=?,");
                result.addBindArg(kv);
            }
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(" WHERE ").append(WhereBuilder.b(id.getName(), "=", idValue));

        result.setSql(builder.toString());
        return result;
    }

    public static SqlInfo buildUpdateSqlInfo(TableEntity<?> table, WhereBuilder whereBuilder, KeyValue... nameValuePairs) throws DbException {

        if (nameValuePairs == null || nameValuePairs.length == 0) return null;

        SqlInfo result = new SqlInfo();
        StringBuilder builder = new StringBuilder("UPDATE ");
        builder.append("\"").append(table.getName()).append("\"");
        builder.append(" SET ");
        for (KeyValue kv : nameValuePairs) {
            builder.append("\"").append(kv.key).append("\"").append("=?,");
            result.addBindArg(kv);
        }
        builder.deleteCharAt(builder.length() - 1);
        if (whereBuilder != null && whereBuilder.getWhereItemSize() > 0) {
            builder.append(" WHERE ").append(whereBuilder.toString());
        }

        result.setSql(builder.toString());
        return result;
    }

    //*********************************************** others ***********************************************

    public static SqlInfo buildCreateTableSqlInfo(TableEntity<?> table) throws DbException {
        ColumnEntity id = table.getId();

        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE IF NOT EXISTS ");
        builder.append("\"").append(table.getName()).append("\"");
        builder.append(" ( ");

        if (id.isAutoId()) {
            builder.append("\"").append(id.getName()).append("\"").append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        } else {
            builder.append("\"").append(id.getName()).append("\"").append(id.getColumnDbType()).append(" PRIMARY KEY, ");
        }

        Collection<ColumnEntity> columns = table.getColumnMap().values();
        for (ColumnEntity column : columns) {
            if (column.isId()) continue;
            builder.append("\"").append(column.getName()).append("\"");
            builder.append(' ').append(column.getColumnDbType());
            builder.append(' ').append(column.getProperty());
            builder.append(',');
        }

        builder.deleteCharAt(builder.length() - 1);
        builder.append(" )");
        return new SqlInfo(builder.toString());
    }

    public static List<KeyValue> entity2KeyValueList(TableEntity<?> table, Object entity) {

        List<KeyValue> keyValueList = new ArrayList<KeyValue>();

        Collection<ColumnEntity> columns = table.getColumnMap().values();
        for (ColumnEntity column : columns) {
            KeyValue kv = column2KeyValue(entity, column);
            if (kv != null) {
                keyValueList.add(kv);
            }
        }

        return keyValueList;
    }

    private static KeyValue column2KeyValue(Object entity, ColumnEntity column) {
        if (column.isAutoId()) {
            return null;
        }

        String key = column.getName();
        Object value = column.getFieldValue(entity);
        return new KeyValue(key, value);
    }
}
