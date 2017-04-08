package org.xutils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.xutils.common.util.KeyValue;
import org.xutils.db.Selector;
import org.xutils.db.sqlite.SqlInfo;
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.db.table.DbModel;
import org.xutils.db.table.TableEntity;
import org.xutils.ex.DbException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 数据库访问接口
 */
public interface DbManager extends Closeable {

    DaoConfig getDaoConfig();

    SQLiteDatabase getDatabase();
    //-------------------------Save---------------------------
    /**
     * 保存实体类或实体类的List到数据库,
     * 如果该类型的id是自动生成的, 则保存完后会给id赋值.
     *
     * @param entity 实体类或实体类的List
     * @return boolean 保存结果(Note:插入式保存、ID存在保存失败)
     * @throws DbException
     */
    boolean saveBindingId(Object entity) throws DbException;

    /**
     * 保存或更新实体类或实体类的List到数据库, 根据id对应的数据是否存在.
     * 如果该类型的id是自动生成的, 则根据entity的ID是否赋值执行saveBindingId或Update
     * @param entity 实体类或实体类的List
     * @throws DbException
     */
    void saveOrUpdate(Object entity) throws DbException;

    /**
     * 保存实体类或实体类的List到数据库(不带ID约束)
     *
     * @param entity entity 实体类或实体类的List
     * @throws DbException
     */
    void save(Object entity) throws DbException;

    /**
     * 保存或更新实体类或实体类的List到数据库, 根据id和其他唯一索引判断数据是否存在.
     *
     * @param entity entity 实体类或实体类的List
     * @throws DbException
     */
    void replace(Object entity) throws DbException;

    //-------------------------Delete---------------------------
    /**
     * 根据实体类的ID值删除一列
     * @param entityType 实体类(例如 User.class)
     * @param idValue ID值
     * @throws DbException
     */
    void deleteById(Class<?> entityType, Object idValue) throws DbException;

    /**
     * 根据实体类对象删除一列(ID不能为空)
     * @param entity 实体类对象
     * @throws DbException
     */
    void delete(Object entity) throws DbException;

    /**
     * 根据实体类删除所有列
     * @param entityType 实体类(例如:User.class)
     * @throws DbException
     */
    void delete(Class<?> entityType) throws DbException;

    /**
     * 根据实体类删除所有列
     * @param entityType 实体类(例如:User.class)
     * @param whereBuilder where条件
     * @return int 影响行数
     * @throws DbException
     */
    int delete(Class<?> entityType, WhereBuilder whereBuilder) throws DbException;

    //-------------------------update---------------------------
    /**
     * 根据实体类对象更新数据
     * @param entity 实体类对象
     * @param updateColumnNames 需要更新的列(不填代表所有列)
     * @throws DbException
     */
    void update(Object entity, String... updateColumnNames) throws DbException;

    /**
     * 根据where条件更新数据
     * @param entityType 实体类
     * @param whereBuilder where条件
     * @param nameValuePairs 需要更新的列
     * @return int 影响行数
     * @throws DbException
     */
    int update(Class<?> entityType, WhereBuilder whereBuilder, KeyValue... nameValuePairs) throws DbException;

    //-------------------------find---------------------------

    /**
     * 根据ID值查找实体类对象
     * @param entityType 实体类
     * @param idValue id值
     * @return 实体类对象
     * @throws DbException
     */
    <T> T findById(Class<T> entityType, Object idValue) throws DbException;

    /**
     * 找出实体表的第一条数据
     * @param entityType 实体类
     * @return 实体类对象
     * @throws DbException
     */
    <T> T findFirst(Class<T> entityType) throws DbException;

    /**
     * 找出实体表中的所有数据
     * @param entityType 实体类
     * @return 实体类的List
     * @throws DbException
     */
    <T> List<T> findAll(Class<T> entityType) throws DbException;

    /**
     * 根据实体类返回一个{@link Selector}操作对象(用于自定义SQL语句)
     * @param entityType 实体类
     * @return {@link Selector}
     * @throws DbException
     */
    <T> Selector<T> selector(Class<T> entityType) throws DbException;

    /**
     * 根据{@link SqlInfo} 找出第一行数据
     * @param sqlInfo {@link SqlInfo}
     * @return {@link DbModel}
     * @throws DbException
     */
    DbModel findDbModelFirst(SqlInfo sqlInfo) throws DbException;

    /**
     * 根据{@link SqlInfo} 找出所有数据
     * @param sqlInfo {@link SqlInfo}
     * @return List {@link DbModel}
     * @throws DbException
     */
    List<DbModel> findDbModelAll(SqlInfo sqlInfo) throws DbException;

    //-------------------------table---------------------------

    /**
     * 获取表信息
     * @param entityType 实体类
     * @return {@link TableEntity}
     * @throws DbException
     */
    <T> TableEntity<T> getTable(Class<T> entityType) throws DbException;

    /**
     * 删除表
     * @param entityType 实体类
     * @throws DbException
     */
    void dropTable(Class<?> entityType) throws DbException;

    /**
     * 添加一列, 新的entityType中必须定义了这个列的属性.
     * @param entityType 实体类(添加列属性后的实体类)
     * @param column 添加的列名
     * @throws DbException
     */
    void addColumn(Class<?> entityType, String column) throws DbException;

    //-------------------------DB---------------------------

    /**
     * 删除库
     * @throws DbException
     */
    void dropDb() throws DbException;

    /**
     * 关闭数据库,
     * xUtils对同一个库的链接是单实例的, 一般不需要关闭它.
     * @throws IOException
     */
    void close() throws IOException;

    //-------------------------Custom---------------------------
    /**
     * 根据{@link SqlInfo}带返回影响数的SQL操作
     * @param sqlInfo {@link SqlInfo}
     * @return 影响数
     * @throws DbException
     */
    int executeUpdateDelete(SqlInfo sqlInfo) throws DbException;

    /**
     * 根据SQL带返回影响数的SQL操作
     * @param sql SQL语句
     * @return 影响数
     * @throws DbException
     */
    int executeUpdateDelete(String sql) throws DbException;

    /**
     * 根据{@link SqlInfo}不带返回影响数的SQL操作
     * @param sqlInfo {@link SqlInfo}
     * @throws DbException
     */
    void execNonQuery(SqlInfo sqlInfo) throws DbException;

    /**
     * 根据SQL带返回影响数的SQL操作
     * @param sql SQL语句
     * @throws DbException
     */
    void execNonQuery(String sql) throws DbException;

    /**
     * 根据{@link SqlInfo}带返回结果集的SQL操作
     * @param sqlInfo {@link SqlInfo}
     * @return {@link Cursor}结果集
     * @throws DbException
     */
    Cursor execQuery(SqlInfo sqlInfo) throws DbException;

    /**
     * 根据SQL语句带返回结果集的SQL操作
     * @param sql SQL语句
     * @return {@link Cursor}结果集
     * @throws DbException
     */
    Cursor execQuery(String sql) throws DbException;

    public interface DbOpenListener {
        void onDbOpened(DbManager db);
    }

    public interface DbUpgradeListener {
        void onUpgrade(DbManager db, int oldVersion, int newVersion);
    }

    public interface TableCreateListener {
        void onTableCreated(DbManager db, TableEntity<?> table);
    }

    /**
     * DB模块设置
     */
    public static class DaoConfig {
        private File dbDir; //DB文件目录
        private String dbName = "xUtils.db"; // default db name
        private int dbVersion = 1; // DB文件版本
        private boolean allowTransaction = true; //是否开启事务
        private DbUpgradeListener dbUpgradeListener;
        private TableCreateListener tableCreateListener;
        private DbOpenListener dbOpenListener;

        public DaoConfig() {
        }

        public DaoConfig setDbDir(File dbDir) {
            this.dbDir = dbDir;
            return this;
        }

        public DaoConfig setDbName(String dbName) {
            if (!TextUtils.isEmpty(dbName)) {
                this.dbName = dbName;
            }
            return this;
        }

        public DaoConfig setDbVersion(int dbVersion) {
            this.dbVersion = dbVersion;
            return this;
        }

        public DaoConfig setAllowTransaction(boolean allowTransaction) {
            this.allowTransaction = allowTransaction;
            return this;
        }

        public DaoConfig setDbOpenListener(DbOpenListener dbOpenListener) {
            this.dbOpenListener = dbOpenListener;
            return this;
        }

        public DaoConfig setDbUpgradeListener(DbUpgradeListener dbUpgradeListener) {
            this.dbUpgradeListener = dbUpgradeListener;
            return this;
        }

        public DaoConfig setTableCreateListener(TableCreateListener tableCreateListener) {
            this.tableCreateListener = tableCreateListener;
            return this;
        }

        public File getDbDir() {
            return dbDir;
        }

        public String getDbName() {
            return dbName;
        }

        public int getDbVersion() {
            return dbVersion;
        }

        public boolean isAllowTransaction() {
            return allowTransaction;
        }

        public DbOpenListener getDbOpenListener() {
            return dbOpenListener;
        }

        public DbUpgradeListener getDbUpgradeListener() {
            return dbUpgradeListener;
        }

        public TableCreateListener getTableCreateListener() {
            return tableCreateListener;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DaoConfig daoConfig = (DaoConfig) o;

            if (!dbName.equals(daoConfig.dbName)) return false;
            return dbDir == null ? daoConfig.dbDir == null : dbDir.equals(daoConfig.dbDir);
        }

        @Override
        public int hashCode() {
            int result = dbName.hashCode();
            result = 31 * result + (dbDir != null ? dbDir.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return String.valueOf(dbDir) + "/" + dbName;
        }
    }
}
