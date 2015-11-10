package org.xutils.sample.db;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.util.Date;

/**
 * Author: wyouflf
 * Date: 13-7-25
 * Time: 下午7:06
 */
@Table(name = "parent", onCreated = "CREATE UNIQUE INDEX index_name ON parent(name,email)")
public class Parent {

    @Column(name = "id", isId = true)
    private int id;

    @Column(name = "name")
    public String name;

    @Column(name = "email")
    private String email;

    @Column(name = "isAdmin")
    private boolean isAdmin;

    @Column(name = "time")
    private Date time;

    @Column(name = "date")
    private java.sql.Date date;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public java.sql.Date getDate() {
        return date;
    }

    public void setDate(java.sql.Date date) {
        this.date = date;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Parent{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", isAdmin=" + isAdmin +
                ", time=" + time +
                ", date=" + date +
                '}';
    }
}
