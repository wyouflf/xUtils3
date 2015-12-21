package org.xutils.sample;

import android.view.View;
import android.widget.TextView;

import org.xutils.DbManager;
import org.xutils.db.table.DbModel;
import org.xutils.sample.db.Child;
import org.xutils.sample.db.Parent;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by wyouflf on 15/11/4.
 */
@ContentView(R.layout.fragment_db)
public class DbFragment extends BaseFragment {

    DbManager.DaoConfig daoConfig = new DbManager.DaoConfig()
            .setDbName("test")
            .setDbDir(new File("/sdcard"))
            .setDbVersion(2)
            .setDbUpgradeListener(new DbManager.DbUpgradeListener() {
                @Override
                public void onUpgrade(DbManager db, int oldVersion, int newVersion) {
                    // TODO: ...
                    // db.addColumn(...);
                    // db.dropTable(...);
                    // ...
                }
            });

    @ViewInject(R.id.tv_db_result)
    private TextView tv_db_result;

    @Event(R.id.btn_test_db)
    private void onTestDbClick(View view) {

        // 一对多: (本示例的代码)
        // 自己在多的一方(child)保存另一方的(parentId), 查找的时候用parentId查parent或child.
        // 一对一:
        // 在任何一边保存另一边的Id并加上唯一属性: @Column(name = "parentId", property = "UNIQUE")
        // 多对多:
        // 再建一个关联表, 保存两边的id. 查询分两步: 先查关联表得到id, 再查对应表的属性.

        String temp = "";

        try {

            DbManager db = x.getDb(daoConfig);

            Child child = new Child();
            child.setName("child's name");

            Parent test = db.selector(Parent.class).where("id", "in", new int[]{1, 3, 6}).findFirst();
            // long count = db.selector(Parent.class).where("id", "in", new int[]{1, 3, 6}).count();
            // Parent test = db.selector(Parent.class).where("id", "between", new String[]{"1", "5"}).findFirst();
            if (test != null) {
                child.setParentId(test.getId());
                temp += "first parent:" + test + "\n";
                tv_db_result.setText(temp);
            }

            Parent parent = new Parent();
            parent.name = "测试" + System.currentTimeMillis();
            parent.setAdmin(true);
            parent.setEmail("wyouflf@qq.com");
            parent.setTime(new Date());
            parent.setDate(new java.sql.Date(new Date().getTime()));
            db.save(parent);

            db.saveBindingId(child);//保存对象关联数据库生成的id

            List<Child> children = db.selector(Child.class).findAll();
            temp += "children size:" + children.size() + "\n";
            tv_db_result.setText(temp);
            if (children.size() > 0) {
                temp += "last children:" + children.get(children.size() - 1) + "\n";
                tv_db_result.setText(temp);
            }

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);
            calendar.add(Calendar.HOUR, 3);

            List<Parent> list = db.selector(Parent.class)
                    .where("id", "<", 54)
                    .and("time", ">", calendar.getTime())
                    .orderBy("id")
                    .limit(10).findAll();
            temp += "find parent size:" + list.size() + "\n";
            tv_db_result.setText(temp);
            if (list.size() > 0) {
                temp += "last parent:" + list.get(list.size() - 1) + "\n";
                tv_db_result.setText(temp);
            }

            //parent.name = "hahaha123";
            //db.update(parent);

            Parent entity = child.getParent(db);
            temp += "find by id:" + entity.toString() + "\n";
            tv_db_result.setText(temp);

            List<DbModel> dbModels = db.selector(Parent.class)
                    .groupBy("name")
                    .select("name", "count(name) as count").findAll();
            temp += "group by result:" + dbModels.get(0).getDataMap() + "\n";
            tv_db_result.setText(temp);

        } catch (Throwable e) {
            temp += "error :" + e.getMessage() + "\n";
            tv_db_result.setText(temp);
        }
    }

}
