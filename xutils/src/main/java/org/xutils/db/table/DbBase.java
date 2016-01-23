package org.xutils.db.table;

import android.database.Cursor;
import android.text.TextUtils;

import org.xutils.DbManager;
import org.xutils.common.util.IOUtil;
import org.xutils.common.util.LogUtil;
import org.xutils.db.sqlite.SqlInfo;
import org.xutils.db.sqlite.SqlInfoBuilder;
import org.xutils.ex.DbException;

import java.util.HashMap;

/**
 * DbManager基类, 包含表结构的基本操作.
 * Created by wyouflf on 16/1/22.
 */
public abstract class DbBase implements DbManager {

    private final HashMap<Class<?>, TableEntity<?>> tableMap = new HashMap<Class<?>, TableEntity<?>>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> TableEntity<T> getTable(Class<T> entityType) throws DbException {
        synchronized (tableMap) {
            TableEntity<T> table = (TableEntity<T>) tableMap.get(entityType);
            if (table == null) {
                try {
                    table = new TableEntity<T>(this, entityType);
                } catch (Throwable ex) {
                    throw new DbException(ex);
                }
                tableMap.put(entityType, table);
            }

            return table;
        }
    }

    @Override
    public void dropTable(Class<?> entityType) throws DbException {
        TableEntity<?> table = this.getTable(entityType);
        if (!table.tableIsExist()) return;
        execNonQuery("DROP TABLE \"" + table.getName() + "\"");
        table.setCheckedDatabase(false);
        this.removeTable(entityType);
    }

    @Override
    public void dropDb() throws DbException {
        Cursor cursor = execQuery("SELECT name FROM sqlite_master WHERE type='table' AND name<>'sqlite_sequence'");
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    try {
                        String tableName = cursor.getString(0);
                        execNonQuery("DROP TABLE " + tableName);
                    } catch (Throwable e) {
                        LogUtil.e(e.getMessage(), e);
                    }
                }

                synchronized (tableMap) {
                    for (TableEntity<?> table : tableMap.values()) {
                        table.setCheckedDatabase(false);
                    }
                    tableMap.clear();
                }
            } catch (Throwable e) {
                throw new DbException(e);
            } finally {
                IOUtil.closeQuietly(cursor);
            }
        }
    }

    @Override
    public void addColumn(Class<?> entityType, String column) throws DbException {
        TableEntity<?> table = this.getTable(entityType);
        ColumnEntity col = table.getColumnMap().get(column);
        if (col != null) {
            StringBuilder builder = new StringBuilder();
            builder.append("ALTER TABLE ").append("\"").append(table.getName()).append("\"").
                    append(" ADD COLUMN ").append("\"").append(col.getName()).append("\"").
                    append(" ").append(col.getColumnDbType()).
                    append(" ").append(col.getProperty());
            execNonQuery(builder.toString());
        }
    }

    protected void createTableIfNotExist(TableEntity<?> table) throws DbException {
        if (!table.tableIsExist()) {
            synchronized (table.getClass()) {
                if (!table.tableIsExist()) {
                    SqlInfo sqlInfo = SqlInfoBuilder.buildCreateTableSqlInfo(table);
                    execNonQuery(sqlInfo);
                    String execAfterTableCreated = table.getOnCreated();
                    if (!TextUtils.isEmpty(execAfterTableCreated)) {
                        execNonQuery(execAfterTableCreated);
                    }
                    table.setCheckedDatabase(true);
                    TableCreateListener listener = this.getDaoConfig().getTableCreateListener();
                    if (listener != null) {
                        listener.onTableCreated(this, table);
                    }
                }
            }
        }
    }

    protected void removeTable(Class<?> entityType) {
        synchronized (tableMap) {
            tableMap.remove(entityType);
        }
    }
}
