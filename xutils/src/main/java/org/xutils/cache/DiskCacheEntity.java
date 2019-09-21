package org.xutils.cache;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.util.Date;

/**
 * Created by wyouflf on 15/8/2.
 * 磁盘缓存对象
 */
@Table(name = "disk_cache")
public final class DiskCacheEntity {

    @Column(name = "id", isId = true)
    private long id;

    @Column(name = "key", property = "UNIQUE")
    private String key;

    @Column(name = "path")
    private String path;

    @Column(name = "textContent")
    private String textContent;

    @Column(name = "bytesContent")
    private byte[] bytesContent;

    // from "max-age" (since http 1.1)
    @Column(name = "expires")
    private long expires = Long.MAX_VALUE;

    @Column(name = "etag")
    private String etag;

    @Column(name = "hits")
    private long hits;

    @Column(name = "lastModify")
    private Date lastModify;

    @Column(name = "lastAccess")
    private long lastAccess;


    public DiskCacheEntity() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /*package*/ String getPath() {
        return path;
    }

    /*package*/ void setPath(String path) {
        this.path = path;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public byte[] getBytesContent() {
        return bytesContent;
    }

    public void setBytesContent(byte[] bytesContent) {
        this.bytesContent = bytesContent;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public long getLastAccess() {
        return lastAccess == 0 ? System.currentTimeMillis() : lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }
}
