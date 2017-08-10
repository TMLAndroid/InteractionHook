package com.rexy.hook.record;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-10 13:46
 */
public class PageRecord {
    int mOperateLength;
    PageOperate mOperaters;

    void record(int optionCode) {
        mOperateLength++;
        mOperaters = PageOperate.obtain(optionCode, mOperaters);
        if (mOperateLength > PageOperate.sMaxOperateLength) {
            mOperateLength -= mOperaters.trim(PageOperate.sMaxOperateLength);
        }
    }

    void destroy() {
        while (mOperaters != null) {
            mOperaters = mOperaters.recycle();
        }
    }
}
