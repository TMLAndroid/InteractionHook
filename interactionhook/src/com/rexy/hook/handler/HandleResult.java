package com.rexy.hook.handler;

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;

import com.rexy.hook.HandlerManager;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.hook.interfaces.IHookHandler;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * this is a base abstract class implements {@link IHandleResult} ,it's used by {@link HandlerManager#onReceiveHandleResult(IHookHandler, IHandleResult)} after some Handler create a result which should be packaged as a HandleResult
 * </p>
 * <p>
 * <p>
 * Different Handler maybe has a different handle result , this result class must extends this base class .
 * </p>
 *
 * @author: rexy
 * @date: 2017-08-02 13:46
 */
public abstract class HandleResult implements IHandleResult {

    private String mTag;
    private View mTargetView;
    private Activity mActivity;
    private IHookHandler mHandler;
    private long mCreateTime;


    /**
     * @see #HandleResult(View, String, long)
     */
    protected HandleResult(View target, String tag) {
        this(target, tag, System.currentTimeMillis());
    }

    /**
     * @param target     the target View that the handler is observing with .
     * @param tag        hook handler tag ,used to distinguish the other handler
     * @param createTime the timestamp when this result is created
     */
    protected HandleResult(View target, String tag, long createTime) {
        mTag = tag;
        mTargetView = target;
        mCreateTime = createTime;
    }

    public void setHandler(IHookHandler handler) {
        mHandler = handler;
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    @Override
    public String getTag() {
        return mTag;
    }

    @Override
    public Activity getActivity() {
        return mActivity;
    }

    @Override
    public IHookHandler getHandler() {
        return mHandler;
    }

    @Override
    public View getTargetView() {
        return mTargetView;
    }

    @Override
    public long getTimestamp() {
        return mCreateTime;
    }

    @Override
    public StringBuilder toShortString(StringBuilder sb) {
        sb = sb == null ? new StringBuilder(64) : sb;
        toShortStringImpl(sb);
        return sb;
    }

    @Override
    public Map<String, Object> dumpResult(Map<String, Object> result) {
        result = result == null ? new HashMap<String, Object>() : result;
        dumpResultImpl(result);
        return result;
    }

    /**
     * dump result as a short description into a given StringBuilder
     *
     * @param receiver a not null receiver to write result
     */
    protected abstract void toShortStringImpl(StringBuilder receiver);

    /**
     * dump result to a given map .
     *
     * @param receiver not null,use for receive any result . put(key,value);
     */
    protected void dumpResultImpl(Map<String, Object> receiver) {
        receiver.put("actionType", getTag());
        View target = getTargetView();
        if (target != null) {
            String viewName = target.getClass().getSimpleName();
            receiver.put("viewName", viewName);
            int id = target.getId();
            Resources res = (id == View.NO_ID || target.getContext() == null) ? null : target.getContext().getResources();
            if (res != null) {
                try {
                    String viewIdName = res.getResourceEntryName(id);
                    receiver.put("viewIdName", viewIdName);
                } catch (Throwable ignore) {
                }
                String viewIdValue = "0x" + Integer.toHexString(id);
                receiver.put("viewId", viewIdValue);
            }
        }
    }

    @Override
    public String toString() {
        return toShortString(null).toString();
    }

    @Override
    public void destroy() {
        mTag = null;
        mHandler = null;
        mActivity = null;
        mTargetView = null;
    }

    /**
     * format View to a short description String like "EditText[editText1#id/7f0b0051]"
     */
    public static String formatView(View view) {
        if (view == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(view.getClass().getSimpleName());
            int id = view.getId();
            Resources res = (id == View.NO_ID || view.getContext() == null) ? null : view.getContext().getResources();
            if (res != null) {
                sb.append('[');
                try {
                    String entryName = res.getResourceEntryName(id);
                    if (entryName != null) {
                        sb.append(entryName).append("#id/");
                    }
                } catch (Throwable ignore) {
                }
                sb.append(Integer.toHexString(id));
                sb.append(']');
            }
            return sb.toString();
        }
    }

    /**
     * format timestamp {@link SimpleDateFormat}
     */
    public static String formatTime(long time, String format) {
        return new SimpleDateFormat(format == null ? "mm:ss.SSS" : format)
                .format(new java.util.Date(time));
    }

    public static String formatError(Throwable error) {
        return error == null ? "" : error.getCause().getMessage();
    }

}
