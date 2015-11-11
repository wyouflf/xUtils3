package org.xutils.sample;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.xutils.common.Callback;
import org.xutils.ex.DbException;
import org.xutils.http.RequestParams;
import org.xutils.sample.download.DownloadService;
import org.xutils.sample.http.BaiduParams;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by wyouflf on 15/11/4.
 */
@ContentView(R.layout.fragment_http)
public class HttpFragment extends BaseFragment {


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
     * 4. 注解参数value支持数组: value={id1, id2, id3}
     * 5. 其它参数说明见{@link org.xutils.view.annotation.Event}类的说明.
     **/
    @Event(value = R.id.btn_test1,
            type = View.OnClickListener.class/*可选参数, 默认是View.OnClickListener.class*/)
    private void onTest1Click(View view) {
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
    }

    // 如果你只需要一个简单的版本.
    @Event(value = R.id.btn_test2)
    private void onTest2Click(View view) throws FileNotFoundException {
        RequestParams params = new RequestParams("http://192.168.199.160:8080/upload");
        params.addQueryStringParameter("wd", "xUtils");
        params.addBodyParameter(
                "file",
                new File("/sdcard/test.jpg"),
                null); // 如果文件没有扩展名, 最好设置contentType参数.
        params.addBodyParameter(
                "file2",
                new FileInputStream(new File("/sdcard/test2.jpg")),
                "image/jpeg",
                "test2.jpg"); // InputStream参数获取不到文件名, 最好设置, 除非服务端不关心这个参数.
        x.http().post(params, new Callback.CommonCallback<String>() {
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

    @ViewInject(R.id.et_url)
    private EditText et_url;

    // 添加到下载列表
    @Event(value = R.id.btn_test3)
    private void onTest3Click(View view) throws DbException {
        for (int i = 0; i < 20; i++) {
            String url = et_url.getText().toString();
            String label = i + "xUtils_" + System.nanoTime();
            DownloadService.getDownloadManager().startDownload(
                    url, label,
                    "/sdcard/xUtils/" + label + ".aar", true, false, null);
        }
    }

    // 添加到下载列表
    @Event(value = R.id.btn_test4)
    private void onTest4Click(View view) throws DbException {
        getActivity().startActivity(new Intent(getActivity(), DownloadActivity.class));
    }

}
