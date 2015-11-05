## xUtils3简介
* xUtils 包含了很多实用的android工具。
* xUtils 支持大文件上传，更全面的http请求协议支持(11种谓词)，拥有更加灵活的ORM，更多的事件注解支持且不受混淆影响...
* xUitls 最低兼容android 4.0 (api level 14), (源码最低兼容至2.3.3, 可以自己修改最低兼容设置).
* xUtils3变化较多所以建立了新的项目不在旧版(github.com/wyouflf/xUtils)上继续维护, 相对于旧版本:
    1. HTTP实现替换HttpClient为UrlConnection, 自动解析回调泛型, 更安全的断点续传策略.
    2. 支持标准的Cookie策略, 区分domain, path...
    3. 事件和数据库注解去除不常用的功能, 提高性能.
    4. 图片绑定支持gif, webp; 支持圆角, 圆形, 方形等裁剪, 支持自动旋转...

#### 使用Gradle构建时添加一下依赖即可:
```javascript
compile 'org.xutils:xutils:3.0'
```

#### 使用前配置
需要的权限
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
初始化
```java
// 在application的onCreate中初始化
@Override
public void onCreate() {
    super.onCreate();
    x.Ext.init(this);
    ...
}
```

### 使用@Event事件注解(@ContentView, @ViewInject等更多示例参考sample项目)
```java
/**
 * 1. 方法必须私有限定,
 * 2. 方法以Click或Event结尾, 方便配置混淆编译参数 :
 * -keepattributes *Annotation*
 * -keepclassmembers class * {
 * void *(android.view.View);
 * *** *Click(...);
 * *** *Event(...);
 * }
 * 3. 方法参数形式必须和type对应的Listener接口一致.
 * 4. 其他见{@link org.xutils.event.annotation.Event}类的说明.
 **/
@Event(value = R.id.btn_test_baidu1,
        type = View.OnClickListener.class/*可选参数, 默认是View.OnClickListener.class*/)
private void onTestBaidu1Click(View view) {
...
}
```

### 访问网络(更多示例参考sample项目)
```java
BaiduParams params = new BaiduParams();
params.wd = "xUtils";
// 有上传文件时使用multipart表单, 否则上传原始文件流.
// params.setMultipart(true);
// 上传文件方式 1
// params.uploadFile = new File("/sdcard/test.txt");
// 上传文件方式 2
// params.addBodyParameter("uploadFile", new File("/sdcard/test.txt"));
Callback.Cancelable cancelable
       = x.http().get(params,
       /**
        * 1. callback的泛型:
        * callback参数默认支持的泛型类型参见{@link org.xutils.http.loader.LoaderFactory},
        * 例如指定泛型为File则可实现文件下载. 默认支持断点续传(采用了文件锁和尾端校验续传文件的一致性).
        * 其他常用类型可以自己在LoaderFactory中注册,
        * 也可以使用{@link org.xutils.http.annotation.HttpResponse}
        * 将注解HttpResponse加到自定义返回值类型上, 实现自定义ResponseParser接口来统一转换.
        * 如果返回值是json形式, 那么利用第三方的json工具将十分容易定义自己的ResponseParser.
        * 如示例代码{@link org.xutils.sample.http.BaiduResponse}, 可直接使用BaiduResponse作为
        * callback的泛型.
        *
        * 2. callback的组合:
        * 可以用基类或接口组合个种类的Callback, 见{@link org.xutils.common.Callback}.
        * 例如:
        * a. 组合使用CacheCallback将使请求检测缓存或将结果存入缓存(仅GET请求生效).
        * b. 组合使用PrepareCallback的prepare方法将为callback提供一次后台执行耗时任务的机会,
        * 然后将结果给onCache或onSuccess.
        * c. 组合使用ProgressCallback将提供进度回调.
        * ...(可参考{@link org.xutils.image.ImageLoader})
        *
        **/
       new Callback.CommonCallback<String>() {
           @Override
           public void onSuccess(String result) {
               Toast.makeText(x.app(), result, Toast.LENGTH_LONG).show();
           }

           @Override
           public void onError(Throwable ex, boolean isOnCallback) {
               Toast.makeText(x.app(), ex.getMessage(), Toast.LENGTH_LONG).show();
           }

           @Override
           public void onCancelled(CancelledException cex) {
               Toast.makeText(x.app(), "cancelled", Toast.LENGTH_LONG).show();
           }

           @Override
           public void onFinished() {

           }
       });

// cancelable.cancel(); // 取消
// 如果需要记录请求的日志, 可使用RequestTracker接口(优先级依次降低, 找到一个实现后会忽略后面的):
// 1. 自定义Callback同时实现RequestTracker接口;
// 2. 自定义ResponseParser同时实现RequestTracker接口;
// 3. 在LoaderFactory注册.
```
#### 如果你只需要一个简单的版本:
```java
@Event(value = R.id.btn_test_baidu2)
private void onTestBaidu2Click(View view) {
    RequestParams params = new RequestParams("https://www.baidu.com/s");
    params.addQueryStringParameter("wd", "xUtils");
    x.http().get(params, new Callback.CommonCallback<String>() {
        @Override
        public void onSuccess(String result) {
            Toast.makeText(x.app(), result, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(Throwable ex, boolean isOnCallback) {
            Toast.makeText(x.app(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCancelled(CancelledException cex) {
            Toast.makeText(x.app(), "cancelled", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFinished() {

        }
    });
}
````

### 使用数据库(更多示例参考sample项目)
```java
Parent test = db.selector(Parent.class).where("id", "in", new int[]{1, 3, 6}).findFirst();
long count = db.selector(Parent.class).where("name", "LIKE", "w%").and("age", ">", 32).count();
List<Parent> testList = db.selector(Parent.class).where("id", "between", new String[]{"1", "5"}).findAll();
```

### 绑定图片(更多示例参考sample项目)
```java
x.image().bind(imageView, url, imageOptions);
x.image().bind(imageView, "file:///sdcard/test.gif", imageOptions);
x.image().bind(imageView, "assets://test.gif", imageOptions);
x.image().bind(imageView, url, imageOptions, new Callback.CommonCallback<Drawable>() {...});
x.image().loadDrawable(url, imageOptions, new Callback.CommonCallback<Drawable>() {...});
x.image().loadFile(url, imageOptions, new Callback.CommonCallback<File>() {...});
```

____
### 关于libwebpbackport
* 部分4.x的机型对webp格式的支持仍然有问题, 需要借助webp.
* webp来自:https://github.com/webmproject/libwebp
* webpbackport来自:https://github.com/alexey-pelykh/webp-android-backport
* 其中为webpbackport添加了nativeDecodeFile的实现, 并修复在Android 5.0以上系统存在bug:
```CPP
// android_backport_webp.cpp
// 修改:
jclassRef = jniEnv->FindClass(...);
// 为:
jclass temp = jniEnv->FindClass(...);
jclassRef = (jclass)jniEnv->NewGlobalRef(temp);
jniEnv->DeleteLocalRef(temp);
```

----
### 关于作者
* Email： <wyouflf@qq.com>, <wyouflf@gmail.com>
* 有任何建议或者使用中遇到问题都可以给我发邮件, 你也可以加入QQ群：330445659(已满), 275967695, 257323060, 384426013, 176778777, 169852490, 技术交流，idea分享 *_*
