package com.rexy.example.extend;

import android.app.Application;

/**
 * Created by rexy on 17/8/2.
 */

public class MyApplication extends Application {

    private static MyApplication mApp;

    public static MyApplication getApp() {
        return mApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApp = this;
        InteractionReporter.getInstance().init(this);
    }
}