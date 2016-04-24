## xUtils3简介
* xUtils 包含了很多实用的android工具.
* xUtils 支持超大文件(超过2G)上传，更全面的http请求协议支持(11种谓词)，拥有更加灵活的ORM，更多的事件注解支持且不受混淆影响...
* xUtils 最低兼容Android 4.0 (api level 14). ([Android 2.3?](https://github.com/wyouflf/xUtils3/issues/8))
* xUtils3变化较多所以建立了新的项目不在旧版(github.com/wyouflf/xUtils)上继续维护, 相对于旧版本:
    1. HTTP实现替换HttpClient为UrlConnection, 自动解析回调泛型, 更安全的断点续传策略.
    2. 支持标准的Cookie策略, 区分domain, path...
    3. 事件注解去除不常用的功能, 提高性能.
    4. 数据库api简化提高性能, 达到和greenDao一致的性能.
    5. 图片绑定支持gif(受系统兼容性影响, 部分gif文件只能静态显示), webp; 支持圆角, 圆形, 方形等裁剪, 支持自动旋转...

#### 使用Gradle构建时添加一下依赖即可:
```javascript
compile 'org.xutils:xutils:3.3.34'
```
##### 如果使用eclipse可以 [点击这里下载aar文件](http://dl.bintray.com/wyouflf/maven/org/xutils/xutils/), 然后用zip解压, 取出jar包和so文件.
##### 混淆配置参考示例项目sample的配置


#### 常见问题:
1. 更好的管理图片缓存: https://github.com/wyouflf/xUtils3/issues/149
2. Cookie的使用: https://github.com/wyouflf/xUtils3/issues/125
3. 关于query参数? http请求可以通过 header, url, body(请求体)传参; query参数是url中问号(?)后面的参数.
4. 关于body参数? body参数只有PUT, POST, PATCH, DELETE(老版本RFC2616文档没有明确指出它是否支持, 所以暂时支持)请求支持.
5. 自定义Http参数对象和结果解析: https://github.com/wyouflf/xUtils3/issues/191

#### 使用前配置
##### 需要的权限
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```
##### 初始化
```java
// 在application的onCreate中初始化
@Override
public void onCreate() {
    super.onCreate();
    x.Ext.init(this);
    x.Ext.setDebug(BuildConfig.DEBUG); // 是否输出debug日志, 开启debug会影响性能.
    ...
}
```

### 使用@Event事件注解(@ContentView, @ViewInject等更多示例参考sample项目)
```java
/**
 * 1. 方法必须私有限定,
 * 2. 方法参数形式必须和type对应的Listener接口一致.
 * 3. 注解参数value支持数组: value={id1, id2, id3}
 * 4. 其它参数说明见{@link org.xutils.event.annotation.Event}类的说明.
 **/
@Event(value = R.id.btn_test_baidu1,
        type = View.OnClickListener.class/*可选参数, 默认是View.OnClickListener.class*/)
private void onTestBaidu1Click(View view) {
...
}
```

### 访问网络(更多示例参考sample项目)
```java
/**
 * 自定义实体参数类请参考:
 * 请求注解 {@link org.xutils.http.annotation.HttpRequest}
 * 请求注解处理模板接口 {@link org.xutils.http.app.ParamsBuilder}
 *
 * 需要自定义类型作为callback的泛型时, 参考:
 * 响应注解 {@link org.xutils.http.annotation.HttpResponse}
 * 响应注解处理模板接口 {@link org.xutils.http.app.ResponseParser}
 *
 * 示例: 查看 org.xutils.sample.http 包里的代码
 */
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
        * 例如: 指定泛型为File则可实现文件下载, 使用params.setSaveFilePath(path)指定文件保存的全路径.
        * 默认支持断点续传(采用了文件锁和尾端校验续传文件的一致性).
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
        * ...(可参考{@link org.xutils.image.ImageLoader}
        * 或 示例代码中的 {@link org.xutils.sample.download.DownloadCallback})
        *
        * 3. 请求过程拦截或记录日志: 参考 {@link org.xutils.http.app.RequestTracker}
        *
        * 4. 请求Header获取: 参考 {@link org.xutils.http.app.RequestInterceptListener}
        *
        * 5. 其他(线程池, 超时, 重定向, 重试, 代理等): 参考 {@link org.xutils.http.RequestParams}
        *
        **/
       new Callback.CommonCallback<String>() {
           @Override
           public void onSuccess(String result) {
               Toast.makeText(x.app(), result, Toast.LENGTH_LONG).show();
           }

           @Override
           public void onError(Throwable ex, boolean isOnCallback) {
               //Toast.makeText(x.app(), ex.getMessage(), Toast.LENGTH_LONG).show();
               if (ex instanceof HttpException) { // 网络错误
                   HttpException httpEx = (HttpException) ex;
                   int responseCode = httpEx.getCode();
                   String responseMsg = httpEx.getMessage();
                   String errorResult = httpEx.getResult();
                   // ...
               } else { // 其他错误
                   // ...
               }
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

// cancelable.cancel(); // 取消请求
```
#### 如果你只需要一个简单的版本:
```java
@Event(value = R.id.btn_test_baidu2)
private void onTestBaidu2Click(View view) {
    RequestParams params = new RequestParams("https://www.baidu.com/s");
    params.setSslSocketFactory(...); // 设置ssl
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
#### 带有缓存的请求示例:
```java
BaiduParams params = new BaiduParams();
params.wd = "xUtils";
// 默认缓存存活时间, 单位:毫秒.(如果服务没有返回有效的max-age或Expires)
params.setCacheMaxAge(1000 * 60);
Callback.Cancelable cancelable
		// 使用CacheCallback, xUtils将为该请求缓存数据.
		= x.http().get(params, new Callback.CacheCallback<String>() {

	private boolean hasError = false;
	private String result = null;

	@Override
	public boolean onCache(String result) {
		// 得到缓存数据, 缓存过期后不会进入这个方法.
		// 如果服务端没有返回过期时间, 参考params.setCacheMaxAge(maxAge)方法.
        //
        // * 客户端会根据服务端返回的 header 中 max-age 或 expires 来确定本地缓存是否给 onCache 方法.
        //   如果服务端没有返回 max-age 或 expires, 那么缓存将一直保存, 除非这里自己定义了返回false的
        //   逻辑, 那么xUtils将请求新数据, 来覆盖它.
        //
        // * 如果信任该缓存返回 true, 将不再请求网络;
        //   返回 false 继续请求网络, 但会在请求头中加上ETag, Last-Modified等信息,
        //   如果服务端返回304, 则表示数据没有更新, 不继续加载数据.
        //
        this.result = result;
        return false; // true: 信任缓存数据, 不在发起网络请求; false不信任缓存数据.
	}

	@Override
	public void onSuccess(String result) {
		// 注意: 如果服务返回304 或 onCache 选择了信任缓存, 这时result为null.
        if (result != null) {
		    this.result = result;
		}
	}

	@Override
	public void onError(Throwable ex, boolean isOnCallback) {
		hasError = true;
		Toast.makeText(x.app(), ex.getMessage(), Toast.LENGTH_LONG).show();
		if (ex instanceof HttpException) { // 网络错误
			HttpException httpEx = (HttpException) ex;
			int responseCode = httpEx.getCode();
			String responseMsg = httpEx.getMessage();
			String errorResult = httpEx.getResult();
			// ...
		} else { // 其他错误
			// ...
		}
	}

	@Override
	public void onCancelled(CancelledException cex) {
		Toast.makeText(x.app(), "cancelled", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onFinished() {
		if (!hasError && result != null) {
			// 成功获取数据
			Toast.makeText(x.app(), result, Toast.LENGTH_LONG).show();
		}
	}
});
```

### 使用数据库(更多示例参考sample项目)
```java
Parent test = db.selector(Parent.class).where("id", "in", new int[]{1, 3, 6}).findFirst();
long count = db.selector(Parent.class).where("name", "LIKE", "w%").and("age", ">", 32).count();
List<Parent> testList = db.selector(Parent.class).where("id", "between", new String[]{"1", "5"}).findAll();
```

### 绑定图片(更多示例参考sample项目)
```java
x.image().bind(imageView, url, imageOptions);

// assets file
x.image().bind(imageView, "assets://test.gif", imageOptions);

// local file
x.image().bind(imageView, new File("/sdcard/test.gif").toURI().toString(), imageOptions);
x.image().bind(imageView, "/sdcard/test.gif", imageOptions);
x.image().bind(imageView, "file:///sdcard/test.gif", imageOptions);
x.image().bind(imageView, "file:/sdcard/test.gif", imageOptions);

x.image().bind(imageView, url, imageOptions, new Callback.CommonCallback<Drawable>() {...});
x.image().loadDrawable(url, imageOptions, new Callback.CommonCallback<Drawable>() {...});
x.image().loadFile(url, imageOptions, new Callback.CommonCallback<File>() {...});
```

____
### 关于libwebpbackport
* 部分4.x的机型对webp格式的支持仍然有问题, 需要借助webp.
* webp来自:https://github.com/webmproject/libwebp
* webpbackport来自:https://github.com/alexey-pelykh/webp-android-backport
* xUtils在使用webpbackport时为其添加了nativeDecodeFile的实现, 并修复其在Android 5.0及以上系统存在bug:
jni代码见: https://github.com/wyouflf/webp-android-backport/commits/master

----
### 关于作者
* Email： <wyouflf@qq.com>, <wyouflf@gmail.com>
* 有任何建议或者使用中遇到问题都可以给我发邮件, 你也可以加入QQ群：330445659(已满), 275967695, 257323060,
384426013, 176778777, 169852490, 261053948, 330108003, 技术交流，idea分享 *_*
