package com.rexy.hook.handler;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import com.rexy.hook.HandlerManager;
import com.rexy.hook.interfaces.IProxyClickListener;
import com.rexy.hook.record.TouchRecord;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * this class use to hook click event of any view .
 * every time the user make a touch down event,it will find and install a proxy click listener to all clickable view.
 *
 * @author: rexy
 * @date: 2017-03-08 16:56
 */
public class HandlerProxyClick extends HookHandler {
    /**
     * a reflect method to get getListenerInfo from a View.
     */
    private static Method sHookMethod;

    /**
     * a reflect field to get mOnClickListener object from ListenerInfo object.
     */
    private static Field sHookFiled;

    /**
     * touch down position x
     */
    private float mDownX;
    /**
     * touch down position y
     */
    private float mDownY;

    /**
     * touch down timestamp
     */
    private long mDownTime;

    /**
     * a proxy click listener that can be rewritten the base onClickListener.do you business here .
     * return true to intercept the original clickListener.
     */
    IProxyClickListener mInnerClickProxy = new IProxyClickListener() {
        @Override
        public boolean onProxyClick(WrapClickListener wrap, View v) {
            int dataPosition = findRecycleViewPosition(v, v.getTag(mPrivateTagKey));
            return reportResult(new ResultProxyClick(v, getTag(), mDownX, mDownY, mDownTime, dataPosition));
        }
    };

    private int mPrivateTagKey = System.identityHashCode(this);

    public HandlerProxyClick(String tag) {
        super(tag);
        mPrivateTagKey = mPrivateTagKey | ((0xFFFF) << 24);
    }

    /**
     * install proxy click listener in a recursive function
     *
     * @param view                  root view .
     * @param recycledContainerDeep view hierarchy level
     */
    private void hookViews(View view, int recycledContainerDeep) {
        if (view.getVisibility() == View.VISIBLE) {
            boolean forceHook = recycledContainerDeep == 1;
            if (view instanceof ViewGroup) {
                boolean existAncestorRecycle = recycledContainerDeep > 0;
                ViewGroup p = (ViewGroup) view;
                if (!(p instanceof AbsListView || p instanceof RecyclerView) || existAncestorRecycle) {
                    hookClickListener(view, recycledContainerDeep, forceHook);
                    if (existAncestorRecycle) {
                        recycledContainerDeep++;
                    }
                } else {
                    recycledContainerDeep = 1;
                }
                int childCount = p.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View child = p.getChildAt(i);
                    hookViews(child, recycledContainerDeep);
                }
            } else {
                hookClickListener(view, recycledContainerDeep, forceHook);
            }
        }
    }

    /**
     * hook original click listener of a single view.
     *
     * @param forceHook force hook  View if it is direct child of some AbsListView 。
     */
    private void hookClickListener(View view, int recycledContainerDeep, boolean forceHook) {
        boolean needHook = forceHook;
        if (!needHook) {
            needHook = view.isClickable();
            if (needHook && recycledContainerDeep == 0) {
                needHook = view.getTag(mPrivateTagKey) == null;
            }
        }
        if (needHook) {
            try {
                Object getListenerInfo = sHookMethod.invoke(view);
                View.OnClickListener baseClickListener = getListenerInfo == null ? null : (View.OnClickListener) sHookFiled.get(getListenerInfo);//获取已设置过的监听器
                if ((baseClickListener != null && !(baseClickListener instanceof IProxyClickListener.WrapClickListener))) {
                    sHookFiled.set(getListenerInfo, new IProxyClickListener.WrapClickListener(baseClickListener, mInnerClickProxy));
                    view.setTag(mPrivateTagKey, recycledContainerDeep);
                }
            } catch (Exception e) {
                reportError(e, "hook");
            }
        }
    }

    private int findRecycleViewPosition(View host, Object deep) {
        int deepInt = (deep instanceof Integer) ? (Integer) deep : 0;
        if (deepInt > 0) {
            View child = host;
            ViewGroup parent = (ViewGroup) child.getParent();
            while (deepInt-- > 1 && (child.getParent() instanceof ViewGroup)) {
                child = parent;
                parent = (ViewGroup) child.getParent();
            }
            if (parent instanceof AbsListView) {
                AbsListView listView = (AbsListView) parent;
                int firstPosition = listView.getFirstVisiblePosition();
                if (firstPosition >= 0) {
                    return firstPosition + listView.indexOfChild(child);
                }
            }
            if (parent instanceof RecyclerView) {
                RecyclerView recyclerView = (RecyclerView) parent;
                return recyclerView.getChildLayoutPosition(child);
            }
        }
        return -1;
    }

    /**
     * ensure all the hook method and field is available.
     *
     * @param caller   the InteractionHook who has context and hook data .
     * @param activity context of a current Activity.
     */
    @Override
    public void init(HandlerManager caller, Activity activity) {
        super.init(caller, activity);
        if (sHookMethod == null) {
            try {
                Class viewClass = Class.forName("android.view.View");
                if (viewClass != null) {
                    sHookMethod = viewClass.getDeclaredMethod("getListenerInfo");
                    if (sHookMethod != null) {
                        sHookMethod.setAccessible(true);
                    }
                }
            } catch (Exception e) {
                reportError(e, "init");
            }
        }
        if (sHookFiled == null) {
            try {
                Class listenerInfoClass = Class.forName("android.view.View$ListenerInfo");
                if (listenerInfoClass != null) {
                    sHookFiled = listenerInfoClass.getDeclaredField("mOnClickListener");
                    if (sHookFiled != null) {
                        sHookFiled.setAccessible(true);
                    }
                }
            } catch (Exception e) {
                reportError(e, "init");
            }
        }
    }

    /**
     * if init success and everything is ready to handle any task .
     */
    @Override
    public boolean supportHandle() {
        return super.supportHandle() && sHookMethod != null && sHookFiled != null;
    }

    @Override
    public boolean handle(HandlerManager caller) {
        View rootView = caller.getRootView();
        if (rootView != null) {
            TouchRecord down = caller.getTouchRecord();
            mDownX = down.getDownX();
            mDownY = down.getDownY();
            mDownTime = down.getDownTime();
            hookViews(rootView, 0);
            return true;
        }
        return false;
    }

    @Override
    public void destroy() {
        mInnerClickProxy = null;
        super.destroy();
    }

    public static class ResultProxyClick extends HandleResult {
        private int mClickX;
        private int mClickY;
        private int mDataPosition = -1;
        private long mDownTime;
        private int mClickedViewId;

        private ResultProxyClick(View target, String tag, float clickX, float clickY, long downTime, int dataPosition) {
            super(target, tag);
            mClickX = (int) clickX;
            mClickY = (int) clickY;
            mDownTime = downTime;
            mDataPosition = dataPosition;
            mClickedViewId = System.identityHashCode(target);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof ResultProxyClick) {
                return mClickedViewId == ((ResultProxyClick) obj).getClickedViewId();
            }
            return false;
        }

        /**
         * get maybe click x in window
         */
        public int getClickX() {
            return mClickX;
        }

        /**
         * get maybe click y in window
         */
        public int getClickY() {
            return mClickY;
        }

        /**
         * get click touch down timestamp;
         */
        public long getDownTime() {
            return mDownTime;
        }

        public int getDataPosition() {
            return mDataPosition;
        }

        public int getClickedViewId() {
            return mClickedViewId;
        }

        /**
         * get click touch up timestamp;
         */
        public long getUpTime() {
            return getTimestamp();
        }

        @Override
        protected void toShortStringImpl(StringBuilder receiver) {
            receiver.append(formatView(getTargetView())).append("{");
            receiver.append("clickX=").append(getClickX()).append(',');
            receiver.append("clickY=").append(getClickY()).append(',');
            receiver.append("downTime=").append(formatTime(getDownTime(), null)).append(',');
            receiver.append("time=").append(formatTime(getTimestamp(), null)).append(',');
            receiver.setCharAt(receiver.length() - 1, '}');
        }

        @Override
        protected void dumpResultImpl(Map<String, Object> receiver) {
            super.dumpResultImpl(receiver);
            receiver.put("clickId", getClickedViewId());
            receiver.put("dataPosition", getDataPosition());
            receiver.put("clickX", getClickX());
            receiver.put("clickY", getClickY());
            receiver.put("timestamp", getUpTime());
            receiver.put("downTime", getDownTime());
            View target = getTargetView();
            if (target != null) {
                if (target.getParent() instanceof ViewGroup) {
                    receiver.put("viewPosition", ((ViewGroup) target.getParent()).indexOfChild(target));
                }
                int[] position = new int[]{0, 0};
                target.getLocationOnScreen(position);
                StringBuilder sb = new StringBuilder();
                sb.append('(').append(position[0]).append(',').append(position[1]);
                sb.append(',').append(target.getWidth() + position[0]);
                sb.append(',').append(target.getHeight() + position[1]).append(')');
                receiver.put("viewBounds", sb.toString());
                List<TextView> lables = new ArrayList(8);
//                List<TextView> icons = new ArrayList(4);
                findAndFillTextView(target, lables);
/*                Iterator<TextView> its = lables.iterator();
                while (its.hasNext()) {
                    TextView text = its.next();
                    if (text instanceof IconFontTextView || (text.getTypeface() == GlobalApp.iconFont)) {
                        its.remove();
                        icons.add(text);
                    }
                }*/
                Comparator<TextView> comparator = new Comparator<TextView>() {
                    @Override
                    public int compare(TextView a, TextView b) {
                        return (int) (b.getTextSize() - a.getTextSize());
                    }
                };

                sb.delete(0, sb.length());
                dumpText(lables, comparator, sb);
//                dumpText(icons, comparator, sb);
                if (sb.length() > 0) {
                    receiver.put("viewText", sb.deleteCharAt(sb.length() - 1).toString());
                }
                //receiver.put("eventHandler", "");
            }
        }

        private void dumpText(List<TextView> textViews, Comparator<TextView> comparator, StringBuilder sb) {
            int viewCount = textViews == null ? 0 : textViews.size();
            if (viewCount > 0) {
                Collections.sort(textViews, comparator);
                for (TextView text : textViews) {
                    sb.append(text.getText()).append(",");
                }
            }
        }

        private void findAndFillTextView(View host, List<TextView> lables) {
            if (host instanceof TextView) {
                lables.add((TextView) host);
            } else if (host instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) host;
                int count = parent.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = parent.getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        findAndFillTextView(child, lables);
                    }
                }
            }
        }
    }
}