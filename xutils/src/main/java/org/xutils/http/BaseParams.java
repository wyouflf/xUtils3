package org.xutils.http;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.common.util.KeyValue;
import org.xutils.common.util.LogUtil;
import org.xutils.http.body.BodyItemWrapper;
import org.xutils.http.body.FileBody;
import org.xutils.http.body.InputStreamBody;
import org.xutils.http.body.MultipartBody;
import org.xutils.http.body.RequestBody;
import org.xutils.http.body.StringBody;
import org.xutils.http.body.UrlEncodedParamsBody;

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
/*package*/ abstract class BaseParams {

    private String charset = "UTF-8";
    private HttpMethod method;
    private String bodyContent;
    private boolean multipart = false; // 是否强制使用multipart表单
    private boolean asJsonContent = false; // 用json形式的bodyParams上传
    private RequestBody requestBody; // 生成的表单

    private final List<Header> headers = new ArrayList<Header>();
    private final List<KeyValue> queryStringParams = new ArrayList<KeyValue>();
    private final List<KeyValue> bodyParams = new ArrayList<KeyValue>();
    private final List<KeyValue> fileParams = new ArrayList<KeyValue>();

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
     *
     * @return
     */
    public boolean isAsJsonContent() {
        return asJsonContent;
    }

    /**
     * 以json形式提交body参数
     *
     * @param asJsonContent
     */
    public void setAsJsonContent(boolean asJsonContent) {
        this.asJsonContent = asJsonContent;
    }

    /**
     * 覆盖header
     *
     * @param name
     * @param value
     */
    public void setHeader(String name, String value) {
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
     * @param name
     * @param value
     */
    public void addHeader(String name, String value) {
        this.headers.add(new Header(name, value, false));
    }

    /**
     * 添加请求参数(根据请求谓词, 将参数加入QueryString或Body.)
     *
     * @param name  参数名
     * @param value 可以是String, File, InputStream 或 byte[]
     */
    public void addParameter(String name, Object value) {
        if (value == null) return;

        if (method == null || HttpMethod.permitsRequestBody(method)) {
            if (!TextUtils.isEmpty(name)) {
                if (value instanceof File
                        || value instanceof InputStream
                        || value instanceof byte[]) {
                    this.fileParams.add(new KeyValue(name, value));
                } else {
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
                    } else if (value.getClass().isArray()) {
                        int len = Array.getLength(value);
                        for (int i = 0; i < len; i++) {
                            this.bodyParams.add(new ArrayItem(name, Array.get(value, i)));
                        }
                    } else {
                        this.bodyParams.add(new KeyValue(name, value));
                    }
                }
            } else {
                this.bodyContent = value.toString();
            }
        } else {
            if (!TextUtils.isEmpty(name)) {
                if (value instanceof Iterable) {
                    for (Object item : (Iterable) value) {
                        this.queryStringParams.add(new ArrayItem(name, item));
                    }
                } else if (value.getClass().isArray()) {
                    int len = Array.getLength(value);
                    for (int i = 0; i < len; i++) {
                        this.queryStringParams.add(new ArrayItem(name, Array.get(value, i)));
                    }
                } else {
                    this.queryStringParams.add(new KeyValue(name, value));
                }
            }
        }
    }

    /**
     * 添加参数至Query String
     *
     * @param name
     * @param value
     */
    public void addQueryStringParameter(String name, String value) {
        if (!TextUtils.isEmpty(name)) {
            this.queryStringParams.add(new KeyValue(name, value));
        }
    }

    /**
     * 添加参数至Body
     *
     * @param name
     * @param value
     */
    public void addBodyParameter(String name, String value) {
        if (!TextUtils.isEmpty(name)) {
            this.bodyParams.add(new KeyValue(name, value));
        } else {
            this.bodyContent = value;
        }
    }

    /**
     * 添加body参数
     */
    public void addBodyParameter(String name, File value) {
        addBodyParameter(name, value, null, null);
    }

    /**
     * 添加body参数
     *
     * @param name        参数名
     * @param value       可以是String, File, InputStream 或 byte[]
     * @param contentType 可为null
     */
    public void addBodyParameter(String name, Object value, String contentType) {
        addBodyParameter(name, value, contentType, null);
    }

    /**
     * 添加body参数
     *
     * @param name        参数名
     * @param value       可以是String, File, InputStream 或 byte[]
     * @param contentType 可为null
     * @param fileName    服务端看到的文件名
     */
    public void addBodyParameter(String name, Object value, String contentType, String fileName) {
        if (TextUtils.isEmpty(contentType) && TextUtils.isEmpty(fileName)) {
            this.fileParams.add(new KeyValue(name, value));
        } else {
            this.fileParams.add(new KeyValue(name, new BodyItemWrapper(value, contentType, fileName)));
        }
    }

    public void setBodyContent(String content) {
        this.bodyContent = content;
    }

    public String getBodyContent() {
        checkBodyParams();
        return bodyContent;
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

    public List<KeyValue> getFileParams() {
        checkBodyParams();
        return new ArrayList<KeyValue>(fileParams);
    }

    public List<KeyValue> getStringParams() {
        List<KeyValue> result = new ArrayList<KeyValue>(
                queryStringParams.size() + bodyParams.size());
        result.addAll(queryStringParams);
        result.addAll(bodyParams);
        return result;
    }

    public String getStringParameter(String name) {
        for (KeyValue kv : queryStringParams) {
            if (name == null && kv.key == null) {
                return kv.getValueStr();
            } else if (name != null && name.equals(kv.key)) {
                return kv.getValueStr();
            }
        }
        for (KeyValue kv : bodyParams) {
            if (name == null && kv.key == null) {
                return kv.getValueStr();
            } else if (name != null && name.equals(kv.key)) {
                return kv.getValueStr();
            }
        }
        return null;
    }

    public List<KeyValue> getParams(String name) {
        List<KeyValue> result = new ArrayList<KeyValue>();
        for (KeyValue kv : queryStringParams) {
            if (name == null && kv.key == null) {
                result.add(kv);
            } else if (name != null && name.equals(kv.key)) {
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
        for (KeyValue kv : fileParams) {
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
        fileParams.clear();
        bodyContent = null;
        requestBody = null;
    }

    public void removeParameter(String name) {
        if (!TextUtils.isEmpty(name)) {
            Iterator<KeyValue> it = queryStringParams.iterator();
            while (it.hasNext()) {
                KeyValue kv = it.next();
                if (name.equals(kv.key)) {
                    it.remove();
                }
            }

            it = bodyParams.iterator();
            while (it.hasNext()) {
                KeyValue kv = it.next();
                if (name.equals(kv.key)) {
                    it.remove();
                }
            }

            it = fileParams.iterator();
            while (it.hasNext()) {
                KeyValue kv = it.next();
                if (name.equals(kv.key)) {
                    it.remove();
                }
            }
        } else {
            bodyContent = null;
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
        } else if (multipart || fileParams.size() > 0) {
            if (!multipart && fileParams.size() == 1) {
                for (KeyValue kv : fileParams) {
                    String contentType = null;
                    Object value = kv.value;
                    if (value instanceof BodyItemWrapper) {
                        BodyItemWrapper wrapper = (BodyItemWrapper) value;
                        value = wrapper.getValue();
                        contentType = wrapper.getContentType();
                    }
                    if (value instanceof File) {
                        result = new FileBody((File) value, contentType);
                    } else if (value instanceof InputStream) {
                        result = new InputStreamBody((InputStream) value, contentType);
                    } else if (value instanceof byte[]) {
                        result = new InputStreamBody(new ByteArrayInputStream((byte[]) value), contentType);
                    } else if (value instanceof String) {
                        // invoke addBodyParameter(key, stringValue, contentType)
                        result = new StringBody((String) value, charset);
                        result.setContentType(contentType);
                    } else {
                        LogUtil.w("Some params will be ignored for: " + this.toString());
                    }
                    break;
                }
            } else {
                multipart = true;
                result = new MultipartBody(fileParams, charset);
            }
        } else if (bodyParams.size() > 0) {
            result = new UrlEncodedParamsBody(bodyParams, charset);
        }

        return result;
    }

    public String toJSONString() {
        List<KeyValue> list = new ArrayList<KeyValue>(queryStringParams.size() + bodyParams.size());
        list.addAll(queryStringParams);
        list.addAll(bodyParams);
        try {
            JSONObject jsonObject = null;
            if (!TextUtils.isEmpty(bodyContent)) {
                jsonObject = new JSONObject(bodyContent);
            } else {
                jsonObject = new JSONObject();
            }
            params2Json(jsonObject, list);
            return jsonObject.toString();
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String toString() {
        checkBodyParams();
        final StringBuilder sb = new StringBuilder();
        if (!queryStringParams.isEmpty()) {
            for (KeyValue kv : queryStringParams) {
                sb.append(kv.key).append("=").append(kv.value).append("&");
            }
            sb.deleteCharAt(sb.length() - 1);
        }

        if (HttpMethod.permitsRequestBody(this.method)) {
            sb.append("<");
            if (!TextUtils.isEmpty(bodyContent)) {
                sb.append(bodyContent);
            } else {
                if (!bodyParams.isEmpty()) {
                    for (KeyValue kv : bodyParams) {
                        sb.append(kv.key).append("=").append(kv.value).append("&");
                    }
                    sb.deleteCharAt(sb.length() - 1);
                }
            }
            sb.append(">");
        }
        return sb.toString();
    }

    private synchronized void checkBodyParams() {
        if (bodyParams.isEmpty()) return;

        if (!HttpMethod.permitsRequestBody(method)
                || !TextUtils.isEmpty(bodyContent)
                || requestBody != null) {
            queryStringParams.addAll(bodyParams);
            bodyParams.clear();
        }

        if (!bodyParams.isEmpty() && (multipart || fileParams.size() > 0)) {
            fileParams.addAll(bodyParams);
            bodyParams.clear();
        }

        if (asJsonContent && !bodyParams.isEmpty()) {
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
                throw new RuntimeException(ex);
            }
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
}
