package org.xutils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by wyouflf on 15/10/29.
 */
public interface ViewInjector {

    void inject(View view);

    void inject(Activity activity);

    void inject(Object handler, View view);

    View inject(Object fragment, LayoutInflater inflater, ViewGroup container);
}
