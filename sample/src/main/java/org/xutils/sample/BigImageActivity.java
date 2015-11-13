package org.xutils.sample;

import android.os.Bundle;
import android.widget.ImageView;

import org.xutils.image.ImageOptions;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

@ContentView(R.layout.activity_big_image)
public class BigImageActivity extends BaseActivity {

    @ViewInject(R.id.iv_big_img)
    private ImageView iv_big_img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageOptions imageOptions = new ImageOptions.Builder()
                // 加载中或错误图片的ScaleType
                //.setPlaceholderScaleType(ImageView.ScaleType.MATRIX)
                // 默认自动适应大小
                // .setSize(...)
                .setIgnoreGif(false)
                .setImageScaleType(ImageView.ScaleType.MATRIX).build();

        //x.image().bind(iv_big_img, getIntent().getStringExtra("url"), imageOptions);

        // assets file
        //x.image().bind(iv_big_img, "assets://test.gif", imageOptions);

        // local file
        //x.image().bind(iv_big_img, new File("/sdcard/test.gif").toURI().toString(), imageOptions);
        //x.image().bind(iv_big_img, "/sdcard/test.gif", imageOptions);
        //x.image().bind(iv_big_img, "file:///sdcard/test.gif", imageOptions);
        //x.image().bind(iv_big_img, "file:/sdcard/test.gif", imageOptions);
    }
}
