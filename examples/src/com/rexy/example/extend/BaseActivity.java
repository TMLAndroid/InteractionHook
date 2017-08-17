package com.rexy.example.extend;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.PermissionChecker;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Toast;

import com.rexy.example.widget.InteractionFloatViewHolder;

/**
 * @author: rexy
 * @date: 2017-06-05 14:45
 */
public class BaseActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!InteractionFloatViewHolder.getInstance(MyApplication.getApp()).isShown()) {
            boolean showFloatView = false;
            if (PermissionChecker.PERMISSION_GRANTED == PermissionChecker.checkSelfPermission(this, Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                showFloatView = true;
            } else {
                if (Build.VERSION.SDK_INT > 23 && !Settings.canDrawOverlays(BaseActivity.this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1000);
                } else {
                    showFloatView = true;

                }
            }
            if (showFloatView) {
                try {
                    InteractionFloatViewHolder.getInstance(this).show();
                    InteractionFloatViewHolder.getInstance(this).updateViewWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.9f), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "require SYSTEM_ALERT_WINDOW permission", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = InteractionReporter.getInstance().onTouch(this, ev);
        if (handled) {
            final long now = SystemClock.uptimeMillis();
            MotionEvent cancelEvent = MotionEvent.obtain(now, now,
                    MotionEvent.ACTION_CANCEL, ev.getX(), ev.getY(), 0);
            cancelEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            InteractionReporter.getInstance().onTouch(this, cancelEvent);
            super.dispatchTouchEvent(cancelEvent);
        }
        return handled || super.dispatchTouchEvent(ev);
    }
}
