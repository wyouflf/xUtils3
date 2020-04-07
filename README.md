## xUtils3简介

xUtils 包含了orm, http(s), image, view注解, 但依然很轻量级(251K), 并且特性强大, 方便扩展.

#### 1. `orm`: 高效稳定的orm工具, 使得http接口实现时更方便的支持cookie和缓存.
* 灵活的, 类似linq表达式的接口.
* 和greenDao一致的性能.

#### 2. `http(s)`: 基于UrlConnection, Android4.4以后底层为okHttp实现.
* 请求协议支持11种谓词: GET,POST,PUT,PATCH,HEAD,MOVE,COPY,DELETE,OPTIONS,TRACE,CONNECT
* 支持超大文件(超过2G)上传
* 支持断点下载(如果服务端支持Range参数,客户端自动处理断点下载)
* 支持cookie(实现了domain, path, expiry等特性)
* 支持缓存(实现了Cache-Control, Last-Modified, ETag等特性, 缓存内容过多时使用过期时间+LRU双重机制清理)
* 支持异步和同步(可结合RxJava使用)调用

#### 3. `image`: 有了`http(s)`及其下载缓存的支持, `image`模块的实现相当的简洁.
* 支持内存缓存, 磁盘缓存(缩略图和原图), 并且支持回收被view持有, 但被MemCache移除的图片, 减少页面回退时的闪烁.
* 支持在ListView滑动时, 自动停止被回收复用的item对应的下载任务(再次下载时断点续传)
* 支持webp, gif(部分比较老的系统只展示静态图)
* 支持圆角, 圆形, 方形等裁剪, 支持自动旋转...

#### 4. `view注解`: view注解模块仅仅400多行代码却灵活的支持了各种View注入和事件绑定.
* 事件注解支持且不受混淆影响...(参考sample的混淆配置)
* 支持绑定拥有多个方法的listener

#### 使用Gradle构建时添加以下依赖即可:
```javascript
implementation 'org.xutils:xutils:3.8.8'
```

#### 混淆配置参考示例项目sample的配置
[这里可以下载aar文件](http://dl.bintray.com/wyouflf/maven/org/xutils/xutils/)


### 常见问题:
1. 更好的管理图片缓存: https://github.com/wyouflf/xUtils3/issues/149
2. Cookie的使用: https://github.com/wyouflf/xUtils3/issues/125
3. 关于query参数? http请求可以通过 header, url, body(请求体)传参; query参数是url中问号(?)后面的参数.
4. 关于body参数? body参数只有PUT, POST, PATCH, DELETE(老版本RFC2616文档没有明确指出它是否支持, 所以暂时支持)请求支持.
5. 自定义Http参数对象和结果解析: https://github.com/wyouflf/xUtils3/issues/191
6. 设置了http超时时间为5s但任然等待15s左右: GET请求失败后默认会重试2次, 可以通过setMaxRetryCount(0)来防止请求自动重试.
7. @Event注解同一个id子类的事件会覆盖父类, onClickListener和onItemClickListener默认屏蔽了双击这种手机上不常用操作, 如需要双击支持可以自己setOnClickListener.

#### 使用前配置
##### 需要的权限
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /><!-- 可选 -->
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
@Event(value = R.id.btn_test1,
        type = View.OnClickListener.class/*可选参数, 默认是View.OnClickListener.class*/)
private void onTest1Click(View view) {
...
}
```

### 使用数据库(更多示例参考sample项目)
```java
Parent test = db.selector(Parent.class)
                    .where("id", "in", new int[]{1, 3, 6})
                    .or("age", "<", 29)
                    .findFirst();
long count = db.selector(Parent.class)
                    .where("name", "LIKE", "w%")
                    .and("age", ">", 32)
                    .count();
List<Parent> testList = db.selector(Parent.class)
                    .where("id", "between", new String[]{"1", "5"})
                    .findAll();
List<DbModel> list = db.selector(Child.class)
                    .where("age", "<", 18)
                    .groupBy("parentId")
                    .having(WhereBuilder.b("COUNT(parentId)", ">", 1))
                    .select("parentId, COUNT(parentId) as childNum")
                    .findAll();
```

### 访问网络(更多示例参考sample项目)
#### 如果你只需要一个简单的网络请求:
```java
@Event(value = R.id.btn_test2)
private void onTest2Click(View view) {
    RequestParams params = new RequestParams("https://www.baidu.com/s");
    // params.setSslSocketFactory(...); // 如果需要自定义SSL
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
#### json或protobuf类型请求的处理
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
JsonDemoParams params = new JsonDemoParams();
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
       * 例如: 指定泛型为File则可实现文件下载, 使用params.setSaveFilePath(path)指定文件保存的全路径, 默认支持断点续传(采用了文件锁防止多线程/进程修改文件,及文件末端校验续传文件的一致性).
       * 
       * 自定义callback的泛型支持方案1, 自定义某一Class的转换(不够灵活): 
       * 结合PrepareCallback的两个泛型参数, 第一个泛型参数类型使用LoaderFactory已经支持的, 第二个泛型参数作为最终输出, 需要在prepare方法中自己实现.
       * 一个稍复杂的例子可以参考{@link org.xutils.image.ImageLoader}
       *
       * 自定义callback的泛型支持方案2, 自定义一类数据的自动转化: 
       * 将注解@HttpResponse加到自定义返回值类型上, 实现自定义ResponseParser接口来统一转换.
       * 如果返回值是json/xml/protobuf等数据格式, 那么利用第三方的json/xml/protobuf等工具将十分容易定义自己的ResponseParser.
       * 如示例代码{@link org.xutils.sample.http.JsonDemoResponse}, 可直接使用JsonDemoResponse作为callback的泛型.
       *
       * 2. callback的组合:
       * 可以用基类或接口组合个种类的Callback, 见{@link org.xutils.common.Callback}.
       * 例如:
       * a. 组合使用CacheCallback将使请求检测缓存或将结果存入缓存(仅GET和POST请求生效).
       * b. 组合使用PrepareCallback的prepare方法将为callback提供一次后台执行耗时任务的机会, 然后将结果给onCache或onSuccess.
       * c. 组合使用ProgressCallback将提供进度回调.
       * 可参考{@link org.xutils.image.ImageLoader} 或 示例代码中的 {@link org.xutils.sample.download.DownloadCallback}
       *
       * 3. 请求过程拦截或记录日志: 参考 {@link org.xutils.http.app.RequestTracker}
       *
       * 4. 请求Header获取: 参考 {@link org.xutils.sample.http.JsonResponseParser} 或 {@link org.xutils.http.app.RequestInterceptListener}
       *
       * 5. 其他(线程池, 超时, 重定向, 重试, 代理等): 参考 {@link org.xutils.http.RequestParams}
       *
       **/
       new Callback.CommonCallback<JsonDemoResponse>() {
           @Override
           public void onSuccess(JsonDemoResponse result) {
               Toast.makeText(x.app(), result.toString(), Toast.LENGTH_LONG).show();
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
#### 带有缓存的请求示例:
```java
JsonDemoParams params = new JsonDemoParams();
params.wd = "xUtils";
// 默认缓存存活时间, 单位:毫秒.(如果服务没有返回有效的max-age或Expires)
params.setCacheMaxAge(1000 * 60);
Callback.Cancelable cancelable
    	// 使用CacheCallback, xUtils将为该请求缓存数据.
		= x.http().get(params, new Callback.CacheCallback<JsonDemoResponse>() {

	private boolean hasError = false;
	private String result = null;

	@Override
	public boolean onCache(JsonDemoResponse result) {
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
	public void onSuccess(JsonDemoResponse result) {
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

### 绑定图片(更多示例参考sample项目)
```java
x.image().bind(imageView, url, imageOptions);

// assets file
x.image().bind(imageView, "assets://test.gif", imageOptions);

// resources file
x.image().bind(imageView, "res://" + R.minimap.test, imageOptions);

// local file
x.image().bind(imageView, new File("/sdcard/test.gif").toURI().toString(), imageOptions);
x.image().bind(imageView, "/sdcard/test.gif", imageOptions);
x.image().bind(imageView, "file:///sdcard/test.gif", imageOptions);
x.image().bind(imageView, "file:/sdcard/test.gif", imageOptions);

x.image().bind(imageView, url, imageOptions, new Callback.CommonCallback<Drawable>() {...});
x.image().loadDrawable(url, imageOptions, new Callback.CommonCallback<Drawable>() {...});
// 用来获取缓存文件
x.image().loadFile(url, imageOptions, new Callback.CommonCallback<File>() {...});
```

----
### 关于作者
* Email： <wyouflf@qq.com>, <wyouflf@gmail.com>
* 有任何建议或者使用中遇到问题都可以给我发邮件, 你也可以加入QQ群：330445659(已满), 275967695, 257323060,
384426013, 176778777, 169852490, 261053948, 330108003, 技术交流，idea分享 *_*
