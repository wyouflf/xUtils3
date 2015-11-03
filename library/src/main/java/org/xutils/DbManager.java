package org.xutils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.xutils.db.Selector;
import org.xutils.db.sqlite.SqlInfo;
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.db.table.DbModel;
import org.xutils.ex.DbException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author: wyouflf
 * @date: 2014/11/29
 */
public interface DbManager extends Closeable {

    DaoConfig getDaoConfig();

    SQLiteDatabase getDatabase();

    ///////////// save
    boolean saveBindingId(Object entity) throws DbException;

    void saveOrUpdate(Object entity) throws DbException;

    void save(Object entity) throws DbException;

    ///////////// replace
    void replace(Object entity) throws DbException;

    ///////////// delete
    void deleteById(Class<?> entityType, Object idValue) throws DbException;

    void delete(Object entity) throws DbException;

    void delete(Class<?> entityType) throws DbException;

    void delete(Class<?> entityType, WhereBuilder whereBuilder) throws DbException;

    ///////////// update
    void update(Object entity, String... updateColumnNames) throws DbException;

    void update(Object entity, WhereBuilder whereBuilder, String... updateColumnNames) throws DbException;

    ///////////// find
    <T> T findById(Class<T> entityType, Object idValue) throws DbException;

    <T> T findFirst(Class<T> entityType) throws DbException;

    <T> List<T> findAll(Class<T> entityType) throws DbException;

    <T> Selector<T> selector(Class<T> entityType) throws DbException;

    DbModel findDbModelFirst(SqlInfo sqlInfo) throws DbException;

    List<DbModel> findDbModelAll(SqlInfo sqlInfo) throws DbException;

    ///////////// table
    void dropTable(Class<?> entityType) throws DbException;

    void addColumn(Class<?> entityType, String column) throws DbException;

    ///////////// db
    void dropDb() throws DbException;

    void close() throws IOException;

    ///////////// custom
    void execNonQuery(SqlInfo sqlInfo) throws DbException;

    void execNonQuery(String sql) throws DbException;

    Cursor execQuery(SqlInfo sqlInfo) throws DbException;

    Cursor execQuery(String sql) throws DbException;

    public interface DbUpgradeListener {
        public void onUpgrade(DbManager db, int oldVersion, int newVersion);
    }

    public static class DaoConfig {
        private String dbName = "xUtils.db"; // default db name
        private int dbVersion = 1;
        private boolean allowTransaction = true;
        private DbUpgradeListener dbUpgradeListener;
        private File dbDir;

        public DaoConfig() {
        }

        public DaoConfig setDbVersion(int dbVersion) {
            this.dbVersion = dbVersion;
            return this;
        }

        public DaoConfig setDbName(String dbName) {
            if (!TextUtils.isEmpty(dbName)) {
                this.dbName = dbName;
            }
            return this;
        }

        public DaoConfig setDbUpgradeListener(DbUpgradeListener dbUpgradeListener) {
            this.dbUpgradeListener = dbUpgradeListener;
            return this;
        }

        public DaoConfig setDbDir(File dbDir) {
            this.dbDir = dbDir;
            return this;
        }

        public DaoConfig setAllowTransaction(boolean allowTransaction) {
            this.allowTransaction = allowTransaction;
            return this;
        }

        public int getDbVersion() {
            return dbVersion;
        }

        public boolean isAllowTransaction() {
            return allowTransaction;
        }

        public String getDbName() {
            return dbName;
        }

        public DbUpgradeListener getDbUpgradeListener() {
            return dbUpgradeListener;
        }

        public File getDbDir() {
            return dbDir;
        }

    }
}
