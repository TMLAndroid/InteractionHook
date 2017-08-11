package com.rexy.hook.record;

import com.rexy.hook.handler.HandleResult;

import java.lang.ref.WeakReference;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-10 13:46
 */
public class PageRecord<PAGERECORD extends PageRecord, RECORDER> {
    int mOperateLength;
    PageOperate mOperator;
    WeakReference<RECORDER> mRecorder;
    PAGERECORD mNext;

    void record(int optionCode) {
        mOperateLength++;
        mOperator = PageOperate.obtain(optionCode, mOperator);
        if (mOperateLength > PageOperate.sMaxOperateLength) {
            mOperateLength -= mOperator.trim(PageOperate.sMaxOperateLength);
        }
    }

    void destroy() {
        while (mOperator != null) {
            mOperator = mOperator.recycle();
        }
    }

    void dumpRecordOption(StringBuilder sb) {
        int beforeLength = sb.length();
        PageOperate peek = mOperator;
        while (peek != null) {
            sb.append(peek.getOptionName()).append('(');
            sb.append(HandleResult.formatTime(peek.mTimestamp, null)).append(',').append(peek.mDeltaTime);
            sb.append(')').append(" < ");
            peek = peek.mNext;
        }
        int afterLength = sb.length();
        if (afterLength > beforeLength) {
            sb.delete(afterLength - 3, afterLength);
            sb.insert(beforeLength, " => { ");
            sb.insert(sb.length(), " }");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        RECORDER recorder = mRecorder == null ? null : mRecorder.get();
        if (recorder != null) {
            sb.append(recorder.getClass().getSimpleName()).append('@').append(recorder.hashCode());
            if (mOperator != null) {
                dumpRecordOption(sb);
            }
        }
        return sb.toString();
    }
}
