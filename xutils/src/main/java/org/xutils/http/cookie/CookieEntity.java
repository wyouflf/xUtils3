package org.xutils.http.cookie;

import android.text.TextUtils;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.net.HttpCookie;
import java.net.URI;

/**
 * Created by wyouflf on 15/8/20.
 * 数据库中的cookie实体
 */
@Table(name = "cookie",
        onCreated = "CREATE UNIQUE INDEX index_cookie_unique ON cookie(\"name\",\"domain\",\"path\")")
/*package*/ final class CookieEntity {

    // ~ 100 year
    private static final long MAX_EXPIRY = System.currentTimeMillis() + 1000L * 60L * 60L * 24L * 30L * 12L * 100L;

    @Column(name = "id", isId = true)
    private long id;

    @Column(name = "uri")
    private String uri; // cookie add by this uri.

    @Column(name = "name")
    private String name;
    @Column(name = "value")
    private String value;
    @Column(name = "comment")
    private String comment;
    @Column(name = "commentURL")
    private String commentURL;
    @Column(name = "discard")
    private boolean discard;
    @Column(name = "domain")
    private String domain;
    @Column(name = "expiry")
    private long expiry = MAX_EXPIRY;
    @Column(name = "path")
    private String path;
    @Column(name = "portList")
    private String portList;
    @Column(name = "secure")
    private boolean secure;
    @Column(name = "version")
    private int version = 1;

    public CookieEntity() {
    }

    public CookieEntity(URI uri, HttpCookie cookie) {
        this.uri = uri == null ? null : uri.toString();
        this.name = cookie.getName();
        this.value = cookie.getValue();
        this.comment = cookie.getComment();
        this.commentURL = cookie.getCommentURL();
        this.discard = cookie.getDiscard();
        this.domain = cookie.getDomain();
        long maxAge = cookie.getMaxAge();
        if (maxAge > 0) {
            this.expiry = (maxAge * 1000L) + System.currentTimeMillis();
            if (this.expiry < 0L) { // 计算溢出?
                this.expiry = MAX_EXPIRY;
            }
        } else {
            this.expiry = -1L;
        }
        this.path = cookie.getPath();
        if (!TextUtils.isEmpty(path) && path.length() > 1 && path.endsWith("/")) {
            this.path = path.substring(0, path.length() - 1);
        }
        this.portList = cookie.getPortlist();
        this.secure = cookie.getSecure();
        this.version = cookie.getVersion();
    }

    public HttpCookie toHttpCookie() {
        HttpCookie cookie = new HttpCookie(name, value);
        cookie.setComment(comment);
        cookie.setCommentURL(commentURL);
        cookie.setDiscard(discard);
        cookie.setDomain(domain);
        if (expiry == -1L) {
            cookie.setMaxAge(-1L);
        } else {
            cookie.setMaxAge((expiry - System.currentTimeMillis()) / 1000L);
        }
        cookie.setPath(path);
        cookie.setPortlist(portList);
        cookie.setSecure(secure);
        cookie.setVersion(version);
        return cookie;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isExpired() {
        return expiry != -1L && expiry < System.currentTimeMillis();
    }
}
