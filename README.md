## xUtils简介
* xUtils 包含了很多实用的android工具。
* xUtils 支持大文件上传，更全面的http请求协议支持(10种谓词)，拥有更加灵活的ORM，更多的事件注解支持且不受混淆影响...
* xUitls 最低兼容android 2.2 (api level 8)



## 还在开发中, 请暂时不要使用, 待续...


____
### 关于libwebpbackport
* webp来自:https://github.com/webmproject/libwebp
* webpbackport来自:https://github.com/alexey-pelykh/webp-android-backport
* 其中webpbackport存在bug:
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
* 有任何建议或者使用中遇到问题都可以给我发邮件, 你也可以加入QQ群：330445659(已满), 275967695, 257323060，技术交流，idea分享 *_*
