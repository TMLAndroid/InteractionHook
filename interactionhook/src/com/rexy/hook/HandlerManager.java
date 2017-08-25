package com.rexy.hook;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.rexy.hook.handler.HandleResult;
import com.rexy.hook.handler.HandlerFocus;
import com.rexy.hook.handler.HandlerGesture;
import com.rexy.hook.handler.HandlerInput;
import com.rexy.hook.handler.HandlerPreventFastClick;
import com.rexy.hook.handler.HandlerProxyClick;
import com.rexy.hook.interfaces.IHandleListener;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.hook.interfaces.IHookHandler;
import com.rexy.hook.record.TouchRecord;
import com.rexy.hook.record.TouchTracker;

/**
 * <p>
 * this class manages a variety of handler {@link com.rexy.hook.handler.HookHandler},{@link IHookHandler} in order to monitor and collect global interaction event.
 * each handler focus on handling just on kind of interactions such as keyboard input,touch gesture,view click,focus change and so on.
 * </p>
 * <p>
 * <p>
 * here is a simple example how to use to collect and monitor the use's interaction .
 * </p>
 * <p>
 * before entry process, we need register a global HandlerListener
 * </p>
 * <p>
 * public class BaseActivity extends FragmentActivity {
 * private InteractionHook mInteractionHook;
 *
 * @Override protected void onCreate(Bundle savedInstanceState) {
 * requestWindowFeature(Window.FEATURE_NO_TITLE);
 * super.onCreate(savedInstanceState);
 * mInteractionHook = new InteractionHook(this, true, "rexy_interaction");
 * }
 * @Override protected void onDestroy() {
 * super.onDestroy();
 * if (mInteractionHook != null) {
 * mInteractionHook.destroy();
 * }
 * }
 * @Override public boolean dispatchTouchEvent(MotionEvent ev) {
 * boolean handled = mInteractionHook == null ? false : mInteractionHook.onTouch(ev);
 * if (handled) {
 * final long now = SystemClock.uptimeMillis();
 * MotionEvent cancelEvent = MotionEvent.obtain(now, now,
 * MotionEvent.ACTION_CANCEL, ev.getX(), ev.getY(), 0);
 * cancelEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
 * if (mInteractionHook != null) {
 * mInteractionHook.onTouch(cancelEvent);
 * }
 * super.dispatchTouchEvent(cancelEvent);
 * }
 * return handled || super.dispatchTouchEvent(ev);
 * }
 * }
 * </p>
 * @author: rexy
 * @date: 2017-07-31 14:11
 */
public class HandlerManager {
    private Activity mActivity = null;
    private TouchTracker mTouchTracker;

    /**
     * a fast click handler to intercept any continuous twice click event.
     */
    private HandlerPreventFastClick mHandlerPreventClick;

    /**
     * set a handler who is interest in proxy and monitor any View click event ,{@link  android.view.View.OnClickListener}
     */
    private HandlerProxyClick mHandlerProxyClick;
    /**
     * gesture handler to analyze any move gesture.
     */
    private HandlerGesture mHandlerGesture;

    /**
     * handler to handle any input by the user,the input event include keyboard code, device menu and so on .
     */
    private HandlerInput mHandlerInput;

    /**
     * handler to handle any focus change event,
     */
    private HandlerFocus mHandlerFocus;

    private IHandleListener mHandleListener;

    private boolean mTouchTracking;

    /**
     * create a hook instance for monitor user interaction
     *
     * @param activity current context activity .
     * @param config   each handler config for installing and enable or disable  {@link InteractionConfig}
     */
    HandlerManager(Activity activity, InteractionConfig config) {
        mActivity = activity;
        ViewGroup rootView = null;
        if (mActivity.getWindow().getDecorView() instanceof ViewGroup) {
            rootView = (ViewGroup) mActivity.getWindow().getDecorView();
        }
        mTouchTracker = new TouchTracker(rootView);
        updateConfig(config);
    }

    public void setHandleListener(IHandleListener l) {
        mHandleListener = l;
    }

    /**
     * get current context activity
     */
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * get the root view ,in most situation it will deservedly be DecorView
     */
    public ViewGroup getRootView() {
        return mTouchTracker.getRootView();
    }

    /**
     * get current TouchRecord information,which record a series of touch information from ACTION_DOWN to ACTION_UP.
     *
     * @return return latest {@link TouchRecord}
     */
    public TouchRecord getTouchRecord() {
        return mTouchTracker.getTouchRecord();
    }

    /**
     * get the previous TouchRecord information
     */
    public TouchRecord getLastTouchRecord() {
        return mTouchTracker.getLastTouchRecord();
    }

    public <T extends IHookHandler> T getHandler(Class<T> cls) {
        return getHandler(cls, false);
    }

    protected <T extends IHookHandler> T getHandler(Class<T> cls, boolean createIfNull) {
        if (cls != null) {
            if (HandlerPreventFastClick.class.isAssignableFrom(cls)) {
                if (mHandlerPreventClick == null && createIfNull) {
                    mHandlerPreventClick = new HandlerPreventFastClick("prevent-click");
                    dispatchInit(mHandlerPreventClick);
                }
                return (T) mHandlerPreventClick;
            }
            if (HandlerProxyClick.class.isAssignableFrom(cls)) {
                if (mHandlerProxyClick == null && createIfNull) {
                    mHandlerProxyClick = new HandlerProxyClick("click");
                    dispatchInit(mHandlerProxyClick);
                }
                return (T) mHandlerProxyClick;
            }
            if (HandlerInput.class.isAssignableFrom(cls)) {
                if (mHandlerInput == null && createIfNull) {
                    mHandlerInput = new HandlerInput("input");
                    dispatchInit(mHandlerInput);
                }
                return (T) mHandlerInput;
            }
            if (HandlerGesture.class.isAssignableFrom(cls)) {
                if (mHandlerGesture == null && createIfNull) {
                    mHandlerGesture = new HandlerGesture("gesture");
                    mTouchTracker.setHandleDragEnable(true);
                    dispatchInit(mHandlerGesture);
                }
                return (T) mHandlerGesture;
            }
            if (HandlerFocus.class.isAssignableFrom(cls)) {
                if (mHandlerFocus == null && createIfNull) {
                    mHandlerFocus = new HandlerFocus("focus");
                    dispatchInit(mHandlerFocus);
                }
                return (T) mHandlerFocus;
            }
        }
        return null;
    }

    public <T extends IHookHandler> T removeHandler(T handler) {
        T result = null;
        if (handler == mHandlerFocus) {
            result = (T) mHandlerFocus;
            mHandlerFocus = dispatchDestroy(mHandlerFocus);
        }
        if (handler == mHandlerInput) {
            result = (T) mHandlerInput;
            mHandlerInput = dispatchDestroy(mHandlerInput);
        }
        if (handler == mHandlerGesture) {
            result = (T) mHandlerGesture;
            mHandlerGesture = dispatchDestroy(mHandlerGesture);
            mTouchTracker.setHandleDragEnable(false);
        }
        if (handler == mHandlerProxyClick) {
            result = (T) mHandlerProxyClick;
            mHandlerProxyClick = dispatchDestroy(mHandlerProxyClick);
        }
        if (handler == mHandlerPreventClick) {
            result = (T) mHandlerPreventClick;
            mHandlerPreventClick = dispatchDestroy(mHandlerPreventClick);
        }
        return result;
    }

    private <T extends IHookHandler> T dispatchDestroy(IHookHandler handler) {
        if (handler != null) {
            handler.destroy();
        }
        return null;
    }

    private boolean dispatchHandle(IHookHandler handler) {
        return handler != null && handler.supportHandle() && handler.handle(this);
    }

    private boolean dispatchInit(IHookHandler handler) {
        if (handler != null) {
            handler.init(this, mActivity);
        }
        return handler != null && handler.supportHandle();
    }

    public void updateConfig(InteractionConfig config) {
        if (config != null) {
            updateHandler(HandlerProxyClick.class, config.installProxyClickHandler && InteractionConfig.isHandleAccess, config.handleProxyClickEnable);
            updateHandler(HandlerPreventFastClick.class, config.installPreventClickHandler, config.handlePreventClickEnable);
            updateHandler(HandlerInput.class, config.installInputHandler && InteractionConfig.isHandleAccess, config.handleInputEnable);
            updateHandler(HandlerGesture.class, config.installGestureHandler && InteractionConfig.isHandleAccess, config.handleGestureEnable);
            updateHandler(HandlerFocus.class, config.installFocusHandler && InteractionConfig.isHandleAccess, config.handleFocusEnable);
        }
    }

    public void updateHandler(Class<? extends IHookHandler> cls, boolean install, boolean enable) {
        IHookHandler hookHandler = getHandler(cls, install);
        if (hookHandler != null) {
            if (install) {
                hookHandler.setHandlerEnable(enable);
            } else {
                removeHandler(hookHandler);
            }
        }
    }

    /**
     * dispatch the top touch event,give a chance to any handler to handle ,called by the {@link Activity#dispatchTouchEvent(MotionEvent)}
     *
     * @param ev original touch event
     * @return true will intercept the touch sequence until the next down event happened.
     */
    public boolean onTouch(MotionEvent ev) {
        boolean intercept = false;
        if (mHandlerPreventClick != null || InteractionConfig.isHandleAccess) {
            int action = ev.getActionMasked();
            boolean actionDown = MotionEvent.ACTION_DOWN == action;
            //prevent the user have touched down and not leave
            // while the condition (mHandlerPreventClick != null || InteractionConfig.isHandleAccess) is false,
            //and suddenly the condition changed to true ,in this case the mTouchRecord in mTouchTracker would be null.
            if (mTouchTracking || actionDown) {
                mTouchTracker.onTouch(ev, action);
                if (actionDown) {
                    mTouchTracking = true;
                    dispatchHandle(mHandlerProxyClick);
                    intercept = dispatchHandle(mHandlerPreventClick);
                } else {
                    if ((MotionEvent.ACTION_UP == action || MotionEvent.ACTION_CANCEL == action)) {
                        mTouchTracking = false;
                        dispatchHandle(mHandlerGesture);
                    }
                }
            }
        }
        return intercept;
    }

    /**
     * do some recycle and destroy task,
     * called in the activity lifecycle method destroy,{@link Activity#onDestroy()}
     */
    public void destroy() {
        mHandlerProxyClick = dispatchDestroy(mHandlerProxyClick);
        mHandlerPreventClick = dispatchDestroy(mHandlerPreventClick);
        mHandlerGesture = dispatchDestroy(mHandlerGesture);
        mHandlerInput = dispatchDestroy(mHandlerInput);
        mHandlerFocus = dispatchDestroy(mHandlerFocus);
        if (mTouchTracker != null) {
            mTouchTracker.destroy();
            mTouchTracker = null;
        }
        mActivity = null;
    }

    /**
     * called when a handler success handled something and created a result .
     *
     * @param handler handler who create this result ,{@link IHookHandler},{@link IHookHandler}.
     * @param result  handle result , a data struct see {@link com.rexy.hook.handler.HandleResult}
     * @return return true to intercept original workflow .
     */
    public boolean onReceiveHandleResult(IHookHandler handler, IHandleResult result) {
        if (result instanceof HandleResult) {
            HandleResult hr = (HandleResult) result;
            hr.setActivity(mActivity);
            hr.setHandler(handler);
        }
        boolean handled = mHandleListener == null ? false : mHandleListener.onHandleResult(result);
        result.destroy();
        return handled;
    }
}
