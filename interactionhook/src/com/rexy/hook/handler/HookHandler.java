package com.rexy.hook.handler;

import android.app.Activity;

import com.rexy.hook.HandlerManager;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.hook.interfaces.IHookHandler;

import java.util.Map;


/**
 * <p>
 * this is a base abstract class implements {@link IHookHandler} ,do any hook task should create a Handler class that must extends this base class.
 *
 * @author: rexy
 * @date: 2017-08-02 13:46
 */
public abstract class HookHandler implements IHookHandler {
    /**
     * whether this handler is enable
     */
    private boolean mHandlerEnable = true;

    /**
     * hook handler tag ,used to distinguish the other handler
     */
    private String mTag;

    /**
     * a hook manager owns the handler supplying context and observing result or error happened while deal with task
     */
    protected HandlerManager mHandlerManager;

    public HookHandler(String tag) {
        mTag = tag;
    }

    @Override
    public void setHandlerEnable(boolean handlerEnable) {
        mHandlerEnable = handlerEnable;
    }

    @Override
    public boolean supportHandle() {
        return mHandlerEnable;
    }

    protected String getTag() {
        return mTag;
    }

    @Override
    public void init(HandlerManager caller, Activity activity) {
        mHandlerManager = caller;
    }

    @Override
    public void reportError(Throwable error, String category) {
        reportResult(new ResultError(error, category));
    }

    @Override
    public boolean reportResult(IHandleResult result) {
        if (mHandlerManager != null && result != null) {
            return mHandlerManager.onReceiveHandleResult(this, result);
        }
        return false;
    }

    @Override
    public void destroy() {
        mHandlerManager = null;
    }

    public static class ResultError extends HandleResult {
        /**
         * error a caught exception while handle some task or do some initialization
         */
        Throwable mError;
        /**
         * category a identify group option tag .
         */
        String mCategory;

        protected ResultError(Throwable error, String category) {
            super(null, "error");
            mError = error;
            mCategory = category;
        }

        public Throwable getError() {
            return mError;
        }

        public String getCategory() {
            return mCategory;
        }

        @Override
        protected void toShortStringImpl(StringBuilder receiver) {
            receiver.append("error=").append(formatError(getError())).append(',');
            receiver.append("time=").append(formatTime(getTimestamp(), null)).append(',');
            receiver.deleteCharAt(receiver.length() - 1);
        }

        @Override
        protected void dumpResultImpl(Map<String, Object> receiver) {
            receiver.put("error", getError());
            receiver.put("category", getCategory());
            receiver.put("time", getTimestamp());
        }
    }
}
