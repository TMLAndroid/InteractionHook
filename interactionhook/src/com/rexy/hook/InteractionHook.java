package com.rexy.hook;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;

import com.rexy.hook.interfaces.IHandleListener;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.hook.interfaces.IHookHandler;

import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by rexy on 17/8/13.
 */
public class InteractionHook {
    private static List<IHandleListener> sHandleListeners = new CopyOnWriteArrayList();
    private static WeakHashMap<Activity, HandlerManager> sHandlers = new WeakHashMap();
    private static Application.ActivityLifecycleCallbacks sLifeCycle = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            onCreate(activity);
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            onDestroy(activity);
        }
    };

    private static IHandleListener sListenerInner = new IHandleListener() {
        @Override
        public boolean onHandle(IHookHandler handler, IHandleResult result) {
            return onReceiveHandleResult(handler, result);
        }
    };

    public static void init(Application application, IHandleListener listener) {
        if (application != null) {
            if (Build.VERSION.SDK_INT >= 14) {
                application.registerActivityLifecycleCallbacks(sLifeCycle);
            }
        }
        if (!sHandleListeners.contains(listener)) {
            sHandleListeners.add(listener);
        }
    }

    public static void onCreate(Activity activity) {
        if (!sHandlers.containsKey(activity)) {
            HandlerManager handler=new HandlerManager(activity, true);
            handler.setHandleListener(sListenerInner);
            sHandlers.put(activity, handler);
        }
    }

    public static void onDestroy(Activity activity) {
        if (sHandlers.containsKey(activity)) {
            HandlerManager manager = sHandlers.get(activity);
            if (manager != null) {
                manager.destroy();
            }
            sHandlers.remove(activity);
        }
    }

    public static boolean onTouch(Activity activity, MotionEvent ev) {
        boolean result = false;
        HandlerManager manager = sHandlers.get(activity);
        if (manager != null) {
            result = manager.onTouch(ev);
        }
        return result;
    }

    public static void registerHandleListener(IHandleListener l) {
        sHandleListeners.add(l);
    }

    public static void unregisterHandleListener(IHandleListener l) {
        sHandleListeners.remove(l);
    }

    private static boolean onReceiveHandleResult(IHookHandler handler, IHandleResult result) {
        boolean handled=false;
        for(IHandleListener l :sHandleListeners){
            if(l.onHandle(handler,result)){
                handled=true;
                break;
            }
        }
        return handled;
    }
}
