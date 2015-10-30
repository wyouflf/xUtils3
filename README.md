## xUtils3简介
* xUtils 包含了很多实用的android工具。
* xUtils 支持大文件上传，更全面的http请求协议支持(11种谓词)，拥有更加灵活的ORM，更多的事件注解支持且不受混淆影响...
* xUitls 最低兼容android 4.0 (api level 14), (源码最低兼容至2.3.3, 可以自己修改最低兼容设置).
* xUtils3变化较多所以建立了新的项目不在旧版(github.com/wyouflf/xUtils)上继续维护, 相对于旧版本:
    1. HTTP实现替换HttpClient为UrlConnection, 自动解析回调泛型, 更安全的断点续传策略.
    2. 支持标准的Cookie策略, 区分domain, path...
    3. 事件和数据库注解去除不常用的功能, 提高性能.
    4. 图片绑定支持gif, webp; 支持圆角, 圆形, 方形等裁剪, 支持自动旋转...



## 文档和实例正在完善中, 待续...


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
