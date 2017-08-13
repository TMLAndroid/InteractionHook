package com.rexy.example.extend;

import android.app.Application;
import android.util.Log;

import com.rexy.hook.InteractionHook;
import com.rexy.hook.interfaces.IHandleListener;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.hook.interfaces.IHookHandler;

/**
 * Created by rexy on 17/8/2.
 */

public class MyApplication extends Application implements IHandleListener {
   private static MyApplication mApp;
    @Override
    public void onCreate() {
        super.onCreate();
        mApp=this;
        InteractionHook.init(this, this);
    }


    public static MyApplication getApp(){
        return mApp;
    }
    @Override
    public boolean onHandle(IHookHandler handler, IHandleResult result) {
        Log.d("rexy_app", result.toShortString(null).toString());
        return false;
    }
}
