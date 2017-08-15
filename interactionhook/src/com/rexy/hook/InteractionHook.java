package com.rexy.hook;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;

import com.rexy.hook.interfaces.IHandleListener;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.hook.interfaces.IHookHandler;

import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by rexy on 17/8/13.
 */
public abstract class InteractionHook {
    /**
     * use for logger debug info
     */
    private static final List<IHandleListener> sHandleListeners = new CopyOnWriteArrayList();
    private static final WeakHashMap<Activity, HandlerManager> sHandlers = new WeakHashMap();
    private static final Application.ActivityLifecycleCallbacks sLifeCycle = new Application.ActivityLifecycleCallbacks() {
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
    private static InteractionConfig sConfig;

    private static IHandleListener sListenerInner = new IHandleListener() {
        @Override
        public boolean onHandleResult(IHandleResult result) {
            return onReceiveHandleResult(result);
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

    public static void updateConfig(InteractionConfig config) {
        sConfig = config;
        if (config != null && sHandlers.size() > 0) {
            Collection<HandlerManager> collection = sHandlers.values();
            if (collection != null) {
                for (HandlerManager handler : collection) {
                    handler.updateConfig(config);
                }
            }
        }
    }

    public static InteractionConfig getConfig() {
        if (sConfig == null) {
            synchronized (InteractionConfig.class) {
                if (sConfig == null) {
                    sConfig = new InteractionConfig();
                    sConfig.handleFocusEnable = true;
                    sConfig.handleInputEnable = true;
                    sConfig.handleGestureEnable= true;
                    sConfig.handleProxyClickEnable = true;
                    sConfig.handlePreventClickEnable = true;

                    sConfig.installFocusHandler=true;
                    sConfig.installInputHandler=true;
                    sConfig.installGestureHandler=true;
                    sConfig.installProxyClickHandler=true;
                    sConfig.installPreventClickHandler=true;
                }
            }
        }
        return sConfig;
    }

    public static HandlerManager getHandlerManager(Activity activity) {
        return sHandlers.get(activity);
    }

    public static <T extends IHookHandler> T getHandler(Activity activity, Class<T> cls) {
        HandlerManager handlerManager = getHandlerManager(activity);
        return handlerManager == null ? null : handlerManager.getHandler(cls);
    }

    public static void onCreate(Activity activity) {
        if (!sHandlers.containsKey(activity)) {
            HandlerManager handler = new HandlerManager(activity, sConfig);
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

    private static boolean onReceiveHandleResult(IHandleResult result) {
        boolean handled = false;
        for (IHandleListener l : sHandleListeners) {
            if (l.onHandleResult(result)) {
                handled = true;
                break;
            }
        }
        result.destroy();
        return handled;
    }

    public static void notify(IHandleResult result) {
        if (result != null) {
            onReceiveHandleResult(result);
        }
    }
}
