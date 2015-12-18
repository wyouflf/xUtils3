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

import android.database.Cursor;
import android.text.TextUtils;

import org.xutils.DbManager;
import org.xutils.common.util.IOUtil;
import org.xutils.db.annotation.Table;
import org.xutils.ex.DbException;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public final class TableEntity<T> {

    private final DbManager db;
    private final String name;
    private final String onCreated;
    private ColumnEntity id;
    private Class<T> entityType;
    private Constructor<T> constructor;

    /**
     * key: columnName
     */
    private final LinkedHashMap<String, ColumnEntity> columnMap;

    /**
     * key: dbName#className
     */
    private static final HashMap<String, TableEntity<?>> tableMap = new HashMap<String, TableEntity<?>>();

    private TableEntity(DbManager db, Class<T> entityType) throws Throwable {
        this.db = db;
        this.entityType = entityType;
        this.constructor = entityType.getConstructor();
        this.constructor.setAccessible(true);
        Table table = entityType.getAnnotation(Table.class);
        this.name = table.name();
        this.onCreated = table.onCreated();
        this.columnMap = TableUtils.findColumnMap(entityType);

        for (ColumnEntity column : columnMap.values()) {
            if (column.isId()) {
                this.id = column;
                break;
            }
        }
    }

    public T createEntity() throws Throwable {
        return this.constructor.newInstance();
    }

    @SuppressWarnings("unchecked")
    public static <T> TableEntity<T> get(DbManager db, Class<T> entityType) throws DbException {
        synchronized (tableMap) {
            String tableKey = generateTableKey(db, entityType);
            TableEntity<T> table = (TableEntity<T>) tableMap.get(tableKey);
            if (table == null) {
                try {
                    table = new TableEntity<T>(db, entityType);
                } catch (Throwable ex) {
                    throw new DbException(ex);
                }
                tableMap.put(tableKey, table);
            }

            return table;
        }
    }

    public static void remove(DbManager db, Class<?> entityType) {
        synchronized (tableMap) {
            String tableKey = generateTableKey(db, entityType);
            tableMap.remove(tableKey);
        }
    }

    public static void remove(DbManager db, String tableName) {
        synchronized (tableMap) {
            if (tableMap.size() > 0) {
                String key = null;
                for (Map.Entry<String, TableEntity<?>> entry : tableMap.entrySet()) {
                    TableEntity table = entry.getValue();
                    if (table != null) {
                        if (table.getName().equals(tableName) && table.getDb() == db) {
                            key = entry.getKey();
                            break;
                        }
                    }
                }
                if (!TextUtils.isEmpty(key)) {
                    tableMap.remove(key);
                }
            }
        }
    }

    public boolean tableIsExist() throws DbException {
        if (this.isCheckedDatabase()) {
            return true;
        }

        Cursor cursor = db.execQuery("SELECT COUNT(*) AS c FROM sqlite_master WHERE type='table' AND name='" + name + "'");
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    int count = cursor.getInt(0);
                    if (count > 0) {
                        this.setCheckedDatabase(true);
                        return true;
                    }
                }
            } catch (Throwable e) {
                throw new DbException(e);
            } finally {
                IOUtil.closeQuietly(cursor);
            }
        }

        return false;
    }

    public DbManager getDb() {
        return db;
    }

    public String getName() {
        return name;
    }

    public Class<T> getEntityType() {
        return entityType;
    }

    public String getOnCreated() {
        return onCreated;
    }

    public ColumnEntity getId() {
        return id;
    }

    public LinkedHashMap<String, ColumnEntity> getColumnMap() {
        return columnMap;
    }

    private volatile boolean checkedDatabase;

    public boolean isCheckedDatabase() {
        return checkedDatabase;
    }

    public void setCheckedDatabase(boolean checkedDatabase) {
        this.checkedDatabase = checkedDatabase;
    }

    private static String generateTableKey(DbManager db, Class<?> entityType) {
        return db.getDaoConfig().toString() + "#" + entityType.getName();
    }

    @Override
    public String toString() {
        return name;
    }
}
