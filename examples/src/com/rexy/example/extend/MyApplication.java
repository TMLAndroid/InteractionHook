package com.rexy.example.extend;

import android.app.Application;
import android.util.Log;

import com.rexy.hook.HandlerConfig;
import com.rexy.hook.InteractionHook;
import com.rexy.hook.interfaces.IHandleListener;
import com.rexy.hook.interfaces.IHandleResult;

/**
 * Created by rexy on 17/8/2.
 */

public class MyApplication extends Application implements IHandleListener {
    private static MyApplication mApp;

    public static MyApplication getApp() {
        return mApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
        InteractionHook.init(this, this);
        HandlerConfig config=new HandlerConfig();
        config.installProxyClickHandler=true;
        config.handleProxyClickEnable=true;

        config.installPreventClickHandler=true;
        config.handlePreventClickEnable=true;

        config.installFocusHandler=true;
        config.handleFocusEnable=true;

        config.installInputHandler=true;
        config.handleInputEnable=true;

        config.installGestureHandler=true;
        config.handleGestureEnable=true;
        InteractionHook.updateConfig(config);
    }

    @Override
    public boolean onHandleResult(IHandleResult result) {
        Log.d("rexy_app", result.toShortString(null).toString());
        return false;
    }
}
