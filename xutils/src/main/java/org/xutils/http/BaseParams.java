package org.xutils.http;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.util.KeyValue;
import org.xutils.http.body.FileBody;
import org.xutils.http.body.InputStreamBody;
import org.xutils.http.body.MultipartBody;
import org.xutils.http.body.RequestBody;
import org.xutils.http.body.StringBody;
import org.xutils.http.body.UrlEncodedBody;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 请求的基础参数
 * Created by wyouflf on 16/1/23.
 */
public abstract class BaseParams {

    private String charset = "UTF-8";
    private HttpMethod method;
    private String bodyContent;
    private String bodyContentType;
    private boolean multipart = false; // 是否使用multipart表单
    private boolean asJsonContent = false; // 用json形式的bodyParams上传
    private RequestBody requestBody; // 生成的表单

    private final List<Header> headers = new ArrayList<Header>();
    private final List<KeyValue> queryStringParams = new ArrayList<KeyValue>();
    private final List<KeyValue> bodyParams = new ArrayList<KeyValue>();

    public void setCharset(String charset) {
        if (!TextUtils.isEmpty(charset)) {
            this.charset = charset;
        }
    }

    public String getCharset() {
        return charset;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public void setMultipart(boolean multipart) {
        this.multipart = multipart;
    }

    /**
     * 以json形式提交body参数
     */
    public boolean isAsJsonContent() {
        return asJsonContent;
    }

    /**
     * 以json形式提交body参数
     */
    public void setAsJsonContent(boolean asJsonContent) {
        this.asJsonContent = asJsonContent;
    }

    /**
     * 覆盖header
     *
     * @param name 为空时不添加该参数
     */
    public void setHeader(String name, String value) {
        if (TextUtils.isEmpty(name)) return;
        Header header = new Header(name, value, true);
        Iterator<Header> it = headers.iterator();
        while (it.hasNext()) {
            KeyValue kv = it.next();
            if (name.equals(kv.key)) {
                it.remove();
            }
        }
        this.headers.add(header);
    }

    /**
     * 添加header
     *
     * @param name 为空时不添加该参数
     */
    public void addHeader(String name, String value) {
        if (TextUtils.isEmpty(name)) return;
        this.headers.add(new Header(name, value, false));
    }

    /**
     * 添加请求参数(根据请求谓词, 将参数加入QueryString或Body.)
     *
     * @param name  参数名(单个File/InputStream/byte[]数据表单允许name为空)
     * @param value 可以是String, File, InputStream 或 byte[]
     */
    public void addParameter(String name, Object value) {
        if (HttpMethod.permitsRequestBody(method)) {
            addBodyParameter(name, value, null, null);
        } else {
            addQueryStringParameter(name, value);
        }
    }

    /**
     * 添加参数至Query String
     *
     * @param name  参数名, 为空时不添加该参数
     * @param value 字符串值, 也可以是String集合或数组
     */
    public void addQueryStringParameter(String name, Object value) {
        if (TextUtils.isEmpty(name)) return;
        if (value instanceof Iterable) {
            for (Object item : (Iterable) value) {
                this.queryStringParams.add(new ArrayItem(name, item));
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            int len = array.length();
            for (int i = 0; i < len; i++) {
                this.queryStringParams.add(new ArrayItem(name, array.opt(i)));
            }
        } else if (value != null && value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                this.queryStringParams.add(new ArrayItem(name, Array.get(value, i)));
            }
        } else {
            this.queryStringParams.add(new KeyValue(name, value));
        }
    }

    /**
     * 添加body参数
     *
     * @param name  参数名(单个File/InputStream/byte[]数据表单允许name为空)
     * @param value 可以是String, File, InputStream 或 byte[]
     */
    public void addBodyParameter(String name, Object value) {
        addBodyParameter(name, value, null, null);
    }

    /**
     * 添加body参数
     *
     * @param name        参数名(单个File/InputStream/byte[]数据表单允许name为空)
     * @param value       可以是String, File, InputStream 或 byte[]
     * @param contentType 可为空
     */
    public void addBodyParameter(String name, Object value, String contentType) {
        addBodyParameter(name, value, contentType, null);
    }

    /**
     * 添加body参数
     *
     * @param name        参数名(单个File/InputStream/byte[]数据表单允许name为空)
     * @param value       可以是String, File, InputStream 或 byte[]
     * @param contentType 可为空
     * @param fileName    服务端看到的文件名
     */
    public void addBodyParameter(String name, Object value, String contentType, String fileName) {
        if (TextUtils.isEmpty(name) && value == null) return;
        if (TextUtils.isEmpty(contentType) && TextUtils.isEmpty(fileName)) {
            if (value instanceof Iterable) {
                for (Object item : (Iterable) value) {
                    this.bodyParams.add(new ArrayItem(name, item));
                }
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                int len = array.length();
                for (int i = 0; i < len; i++) {
                    this.bodyParams.add(new ArrayItem(name, array.opt(i)));
                }
            } else if (value instanceof byte[]) {
                this.bodyParams.add(new KeyValue(name, value));
            } else if (value != null && value.getClass().isArray()) {
                int len = Array.getLength(value);
                for (int i = 0; i < len; i++) {
                    this.bodyParams.add(new ArrayItem(name, Array.get(value, i)));
                }
            } else {
                this.bodyParams.add(new KeyValue(name, value));
            }
        } else {
            this.bodyParams.add(new BodyItemWrapper(name, value, contentType, fileName));
        }
    }

    public void setBodyContent(String content) {
        this.bodyContent = content;
    }

    public String getBodyContent() {
        checkBodyParams();
        return bodyContent;
    }

    /**
     * 设置POST等请求的 Content-Type
     *
     * @param bodyContentType multipart表单仅设置subType(例如:"form-data"(默认) or "related");
     *                        kv结构自定义设置会被忽略, 默认使用:"application/x-www-form-urlencoded;charset=" + charset;
     *                        字符串内容表单默认使用: "application/json;charset=" + charset;
     *                        File表单默认尝试使用文件名匹配Content-Type, 匹配失败使用:"application/octet-stream";
     *                        InputStream表单默认使用: "application/octet-stream";
     */
    public void setBodyContentType(String bodyContentType) {
        this.bodyContentType = bodyContentType;
    }

    public List<Header> getHeaders() {
        return new ArrayList<Header>(headers);
    }

    public List<KeyValue> getQueryStringParams() {
        checkBodyParams();
        return new ArrayList<KeyValue>(queryStringParams);
    }

    public List<KeyValue> getBodyParams() {
        checkBodyParams();
        return new ArrayList<KeyValue>(bodyParams);
    }

    public List<KeyValue> getParams(String name) {
        List<KeyValue> result = new ArrayList<KeyValue>();
        for (KeyValue kv : queryStringParams) {
            if (name != null && name.equals(kv.key)) {
                result.add(kv);
            }
        }
        for (KeyValue kv : bodyParams) {
            if (name == null && kv.key == null) {
                result.add(kv);
            } else if (name != null && name.equals(kv.key)) {
                result.add(kv);
            }
        }
        return result;
    }

    public void clearParams() {
        queryStringParams.clear();
        bodyParams.clear();
        bodyContent = null;
        bodyContentType = null;
        requestBody = null;
    }

    public void removeParameter(String name) {
        if (TextUtils.isEmpty(name)) {
            bodyContent = null;
            bodyContentType = null;
        } else {
            Iterator<KeyValue> it = queryStringParams.iterator();
            while (it.hasNext()) {
                KeyValue kv = it.next();
                if (name.equals(kv.key)) {
                    it.remove();
                }
            }
        }

        Iterator<KeyValue> it = bodyParams.iterator();
        while (it.hasNext()) {
            KeyValue kv = it.next();
            if (name == null && kv.key == null) {
                it.remove();
            } else if (name != null && name.equals(kv.key)) {
                it.remove();
            }
        }
    }

    public void setRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
    }

    public RequestBody getRequestBody() throws IOException {
        checkBodyParams();
        if (this.requestBody != null) {
            return this.requestBody;
        }

        RequestBody result = null;
        if (!TextUtils.isEmpty(bodyContent)) {
            result = new StringBody(bodyContent, charset);
            result.setContentType(bodyContentType);
        } else if (multipart) {
            result = new MultipartBody(bodyParams, charset);
            result.setContentType(bodyContentType);
        } else if (bodyParams.size() == 1) {
            KeyValue kv = bodyParams.get(0);
            String name = kv.key;
            Object value = kv.value;
            String contentType = null;
            if (kv instanceof BodyItemWrapper) {
                BodyItemWrapper wrapper = (BodyItemWrapper) kv;
                contentType = wrapper.contentType;
            }
            if (TextUtils.isEmpty(contentType)) {
                contentType = bodyContentType;
            }
            if (value instanceof File) {
                result = new FileBody((File) value, contentType);
            } else if (value instanceof InputStream) {
                result = new InputStreamBody((InputStream) value, contentType);
            } else if (value instanceof byte[]) {
                result = new InputStreamBody(new ByteArrayInputStream((byte[]) value), contentType);
            } else {
                if (TextUtils.isEmpty(name)) {
                    result = new StringBody(kv.getValueStrOrEmpty(), charset);
                    result.setContentType(contentType);
                } else {
                    result = new UrlEncodedBody(bodyParams, charset);
                    result.setContentType(contentType);
                }
            }
        } else {
            result = new UrlEncodedBody(bodyParams, charset);
            result.setContentType(bodyContentType);
        }

        return result;
    }

    public String toJSONString() throws JSONException {
        JSONObject jsonObject = null;
        if (!TextUtils.isEmpty(bodyContent)) {
            jsonObject = new JSONObject(bodyContent);
        } else {
            jsonObject = new JSONObject();
        }
        List<KeyValue> list = new ArrayList<KeyValue>(
                queryStringParams.size() + bodyParams.size());
        list.addAll(queryStringParams);
        list.addAll(bodyParams);
        params2Json(jsonObject, list);
        return jsonObject.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (!queryStringParams.isEmpty()) {
            for (KeyValue kv : queryStringParams) {
                sb.append(kv.key).append("=").append(kv.value).append("&");
            }
            sb.deleteCharAt(sb.length() - 1);
        }

        if (!TextUtils.isEmpty(bodyContent)) {
            sb.append("<").append(bodyContent).append(">");
        } else if (!bodyParams.isEmpty()) {
            sb.append("<");
            for (KeyValue kv : bodyParams) {
                sb.append(kv.key).append("=").append(kv.value).append("&");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(">");
        }
        return sb.toString();
    }

    private synchronized void checkBodyParams() {
        if (bodyParams.isEmpty()) return;

        if (requestBody != null || !HttpMethod.permitsRequestBody(method)) {
            queryStringParams.addAll(bodyParams);
            bodyParams.clear();
            return;
        }

        if (asJsonContent) {
            try {
                JSONObject jsonObject = null;
                if (!TextUtils.isEmpty(bodyContent)) {
                    jsonObject = new JSONObject(bodyContent);
                } else {
                    jsonObject = new JSONObject();
                }
                params2Json(jsonObject, bodyParams);
                bodyContent = jsonObject.toString();
                bodyParams.clear();
            } catch (JSONException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        } else if (!TextUtils.isEmpty(bodyContent)) {
            queryStringParams.addAll(bodyParams);
            bodyParams.clear();
        }
    }

    private void params2Json(final JSONObject jsonObject, final List<KeyValue> paramList) throws JSONException {
        HashSet<String> arraySet = new HashSet<String>(paramList.size());
        LinkedHashMap<String, JSONArray> tempData = new LinkedHashMap<String, JSONArray>(paramList.size());
        for (int i = 0; i < paramList.size(); i++) {
            KeyValue kv = paramList.get(i);
            final String key = kv.key;
            if (TextUtils.isEmpty(key)) continue;

            JSONArray ja = null;
            if (tempData.containsKey(key)) {
                ja = tempData.get(key);
            } else {
                ja = new JSONArray();
                tempData.put(key, ja);
            }

            ja.put(RequestParamsHelper.parseJSONObject(kv.value));

            if (kv instanceof ArrayItem) {
                arraySet.add(key);
            }
        }

        for (Map.Entry<String, JSONArray> entry : tempData.entrySet()) {
            String key = entry.getKey();
            JSONArray ja = entry.getValue();
            if (ja.length() > 1 || arraySet.contains(key)) {
                jsonObject.put(key, ja);
            } else {
                Object value = ja.get(0);
                jsonObject.put(key, value);
            }
        }
    }

    public final static class ArrayItem extends KeyValue {
        public ArrayItem(String key, Object value) {
            super(key, value);
        }
    }

    public final static class Header extends KeyValue {

        public final boolean setHeader;

        public Header(String key, String value, boolean setHeader) {
            super(key, value);
            this.setHeader = setHeader;
        }
    }

    public final class BodyItemWrapper extends KeyValue {

        public final String fileName;
        public final String contentType;

        public BodyItemWrapper(String key, Object value, String contentType, String fileName) {
            super(key, value);
            if (TextUtils.isEmpty(contentType)) {
                this.contentType = "application/octet-stream";
            } else {
                this.contentType = contentType;
            }
            this.fileName = fileName;
        }
    }
}
