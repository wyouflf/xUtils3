/*
 * Copyright (c) 2013. wyouflf (wyouflf@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xutils.view;

import android.text.TextUtils;
import android.view.View;

import org.xutils.common.util.DoubleKeyValueMap;
import org.xutils.common.util.LogUtil;
import org.xutils.view.annotation.Event;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/*package*/ final class EventListenerManager {

    private final static long QUICK_EVENT_TIME_SPAN = 300;
    private final static HashSet<String> AVOID_QUICK_EVENT_SET = new HashSet<String>(2);

    static {
        AVOID_QUICK_EVENT_SET.add("onClick");
        AVOID_QUICK_EVENT_SET.add("onItemClick");
    }

    private EventListenerManager() {
    }

    /**
     * k1: viewInjectInfo
     * k2: interface Type
     * value: listener
     */
    private final static DoubleKeyValueMap<ViewInfo, Class<?>, Object>
            listenerCache = new DoubleKeyValueMap<ViewInfo, Class<?>, Object>();


    public static void addEventMethod(
            //根据页面或view holder生成的ViewFinder
            ViewFinder finder,
            //根据当前注解ID生成的ViewInfo
            ViewInfo info,
            //注解对象
            Event event,
            //页面或view holder对象
            Object handler,
            //当前注解方法
            Method method) {
        try {
            View view = finder.findViewByInfo(info);

            if (view != null) {
                // 注解中定义的接口，比如Event注解默认的接口为View.OnClickListener
                Class<?> listenerType = event.type();
                // 默认为空，注解接口对应的Set方法，比如setOnClickListener方法
                String listenerSetter = event.setter();
                if (TextUtils.isEmpty(listenerSetter)) {
                    listenerSetter = "set" + listenerType.getSimpleName();
                }


                String methodName = event.method();

                boolean addNewMethod = false;
                /*
                    根据View的ID和当前的接口类型获取已经缓存的接口实例对象，
                    比如根据View.id和View.OnClickListener.class两个键获取这个View的OnClickListener对象
                 */
                Object listener = listenerCache.get(info, listenerType);
                DynamicHandler dynamicHandler = null;
                /*
                    如果接口实例对象不为空
                    获取接口对象对应的动态代理对象
                    如果动态代理对象的handler和当前handler相同
                    则为动态代理对象添加代理方法
                 */
                if (listener != null) {
                    dynamicHandler = (DynamicHandler) Proxy.getInvocationHandler(listener);
                    addNewMethod = handler.equals(dynamicHandler.getHandler());
                    if (addNewMethod) {
                        dynamicHandler.addMethod(methodName, method);
                    }
                }

                // 如果还没有注册此代理
                if (!addNewMethod) {

                    dynamicHandler = new DynamicHandler(handler);

                    dynamicHandler.addMethod(methodName, method);

                    // 生成的代理对象实例，比如View.OnClickListener的实例对象
                    listener = Proxy.newProxyInstance(
                            listenerType.getClassLoader(),
                            new Class<?>[]{listenerType},
                            dynamicHandler);

                    listenerCache.put(info, listenerType, listener);
                }

                Method setEventListenerMethod = view.getClass().getMethod(listenerSetter, listenerType);
                setEventListenerMethod.invoke(view, listener);
            }
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    public static class DynamicHandler implements InvocationHandler {
        // 存放代理对象，比如Fragment或view holder
        private WeakReference<Object> handlerRef;
        // 存放代理方法
        private final HashMap<String, Method> methodMap = new HashMap<String, Method>(1);

        private static long lastClickTime = 0;

        public DynamicHandler(Object handler) {
            this.handlerRef = new WeakReference<Object>(handler);
        }

        public void addMethod(String name, Method method) {
            methodMap.put(name, method);
        }

        public Object getHandler() {
            return handlerRef.get();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object handler = handlerRef.get();
            if (handler != null) {

                String eventMethod = method.getName();
                if ("toString".equals(eventMethod)) {
                    return DynamicHandler.class.getSimpleName();
                }

                method = methodMap.get(eventMethod);
                if (method == null && methodMap.size() == 1) {
                    for (Map.Entry<String, Method> entry : methodMap.entrySet()) {
                        if (TextUtils.isEmpty(entry.getKey())) {
                            method = entry.getValue();
                        }
                        break;
                    }
                }

                if (method != null) {

                    if (AVOID_QUICK_EVENT_SET.contains(eventMethod)) {
                        long timeSpan = System.currentTimeMillis() - lastClickTime;
                        if (timeSpan < QUICK_EVENT_TIME_SPAN) {
                            LogUtil.d("onClick cancelled: " + timeSpan);
                            return null;
                        }
                        lastClickTime = System.currentTimeMillis();
                    }

                    try {
                        return method.invoke(handler, args);
                    } catch (Throwable ex) {
                        throw new RuntimeException("invoke method error:" +
                                handler.getClass().getName() + "#" + method.getName(), ex);
                    }
                } else {
                    LogUtil.w("method not impl: " + eventMethod + "(" + handler.getClass().getSimpleName() + ")");
                }
            }
            return null;
        }
    }
}
