package com.rexy.hook.record;

import android.support.v4.util.Pools;

/**
 * this class is used to record page switch option , we just interest in action onResume ,onPause , onDestroy ,onHiddenChanged .
 * just record the option type .
 */
class PageOperate {
    static final int sMaxOperateLength = 4;
    /**
     * code for onResume
     */
    static final int OPERATE_RESUME = 1;
    /**
     * code for onPause
     */
    static final int OPERATE_PAUSE = 2;
    /**
     * code for fragment onHiddenChanged hide
     */
    static final int OPERATE_HIDE = 3;
    /**
     * code for fragment onHiddenChanged show
     */
    static final int OPERATE_SHOW = 4;
    /**
     * code for onDestroy
     */
    static final int OPERATE_DESTROY = 5;


    private static Pools.Pool<PageOperate> sPageOptionPool = new Pools.SimplePool(8);


    public static PageOperate obtain(int option, PageOperate next) {
        PageOperate operate = sPageOptionPool.acquire();
        if (operate == null) {
            operate = new PageOperate();
        }
        operate.mOption = option;
        operate.mTimestamp = System.currentTimeMillis();
        operate.mNext = next;
        if (next != null) {
            operate.mDeltaTime = (int) (operate.mTimestamp - next.mTimestamp);
        }
        return operate;
    }

    int mOption;
    int mDeltaTime;
    long mTimestamp;
    PageOperate mNext;

    private PageOperate() {
    }

    public int trim(int count) {
        PageOperate parent = null;
        PageOperate peek = this;
        while (peek != null && count > 0) {
            parent = peek;
            peek = peek.mNext;
            count--;
        }
        if (peek != null) {
            while (peek != null && (peek.mOption != OPERATE_RESUME && peek.mOption != OPERATE_SHOW)) {
                parent = peek;
                peek = peek.mNext;
            }
        }
        count = 0;
        while (peek != null) {
            peek = peek.recycle();
            count++;
        }
        if (count != 0) {
            parent.mNext = null;
        }
        return count;
    }

    public PageOperate recycle() {
        PageOperate next = mNext;
        mNext = null;
        mTimestamp = 0;
        mDeltaTime = 0;
        sPageOptionPool.release(this);
        return next;
    }

    /**
     * get option name ,enum in {"resume","pause","hide","show","destroy"}
     */
    public String getOptionName() {
        if (mOption == OPERATE_RESUME) {
            return "resume";
        }
        if (mOption == OPERATE_PAUSE) {
            return "pause";
        }
        if (mOption == OPERATE_HIDE) {
            return "hide";
        }
        if (mOption == OPERATE_SHOW) {
            return "show";
        }
        if (mOption == OPERATE_DESTROY) {
            return "destroy";
        }
        return "unknown";
    }
}
