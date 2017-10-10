package com.rexy.hook.handler;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.rexy.hook.HandlerManager;
import com.rexy.hook.record.InputRecord;

import java.util.List;
import java.util.Map;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-01 10:49
 */
public class HandlerInput extends HookHandler {

    private ViewGroup mRootView;
    /**
     * current focus edit text widget
     */
    private EditText mEditText;
    /**
     * current focus view maybe the same with {@link #mEditText}
     */
    private InputRecord mRecordHeader;
    private InputRecord mRecordPointer;
    private InputRecord mRecordTemp;

    private static int sTimeMaxInterval = 1000 * 5;

    /**
     * global focus change listener to observe the current focus View.
     */
    ViewTreeObserver.OnGlobalFocusChangeListener mFocusListener = new ViewTreeObserver.OnGlobalFocusChangeListener() {
        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            onFocusViewChanged(newFocus, oldFocus);
        }
    };

    /**
     * TextWatcher to observe the text change of the current focus EditText
     */
    TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mEditText != null && (before == 0 || count == 0)) {
                int added = before == 0 ? count : -before;
                long time = System.currentTimeMillis();
                if (before > 1 && (s == null || s.length() == 0)) {
                    if (mRecordPointer == null && mRecordTemp == null) {
                        mRecordTemp = InputRecord.obtain(mEditText, time);
                        mRecordTemp.record(time, added, s);
                    }
                    return;
                }
                if (count > 1 && s != null && count == s.length()) {
                    if (mRecordPointer == null && mRecordTemp == null) {
                        mRecordTemp = InputRecord.obtain(mEditText, time);
                        mRecordTemp.record(time, added, s);
                    }
                    return;
                }
                if (mRecordHeader == null || mRecordPointer == null) {
                    mRecordPointer = mRecordHeader = InputRecord.obtain(mEditText, time);
                } else {
                    if (mEditText != mRecordPointer.getTargetView() || !mRecordPointer.support(time)) {
                        mRecordPointer.mNext = InputRecord.obtain(mEditText, time);
                        mRecordPointer = mRecordPointer.mNext;
                    }
                }
                mRecordPointer.record(time, added, s);
                if (mRecordTemp != null) {
                    mRecordTemp.recycle();
                    mRecordTemp = null;
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    public HandlerInput(String tag) {
        super(tag);
    }

    @Override
    public void init(HandlerManager caller, Activity activity) {
        super.init(caller, activity);
        mRootView = caller.getRootView();
        if (mRootView != null) {
            mRootView.getViewTreeObserver().addOnGlobalFocusChangeListener(mFocusListener);
            View focusView = mRootView.findFocus();
            if (focusView != null) {
                onFocusViewChanged(focusView, null);
            }
        }
    }

    /**
     * call when the focus view changed in this window
     *
     * @param focusView new focus View
     * @param oldFocusView old focus View may be null
     */
    private void onFocusViewChanged(View focusView, View oldFocusView) {
        EditText prevEdit = mEditText;
        if (focusView instanceof EditText) {
            EditText edit = (EditText) focusView;
            if (edit != mEditText) {
                inputListenerFor(mEditText, false);
                inputListenerFor(edit, true);
                mEditText = edit;
            }
        }
        InputRecord header = mRecordHeader == null ? mRecordTemp : mRecordHeader;
        boolean recycleTemp = true;
        if (header != null) {
            mRecordHeader = mRecordPointer = null;
            if ((oldFocusView instanceof EditText) || (focusView == null && oldFocusView == mRootView)) {
                recycleTemp = mRecordTemp != header;
                handleInputRecordAfterUnfocused(prevEdit, header);
            }
        }
        if (mRecordTemp != null) {
            if (recycleTemp) {
                mRecordTemp.recycle();
            }
            mRecordTemp = null;
        }
    }

    /**
     * add or remove TextWatcher with a given EditText .
     *
     * @param install true to add TextWatch ,false to remove
     */
    private void inputListenerFor(EditText edit, boolean install) {
        if (edit != null) {
            if (install) {
                edit.addTextChangedListener(mTextWatcher);
            } else {
                edit.removeTextChangedListener(mTextWatcher);
            }
        }
    }

    @Override
    public boolean handle(HandlerManager caller) {
        return false;
    }

    private void handleInputRecordAfterUnfocused(EditText edit, InputRecord header) {
        while (header != null && header.getTargetView() != edit) {
            header = header.recycle();
        }
        InputRecord pointer = header == null ? null : header.mNext, previous = header;
        while (pointer != null) {
            if (pointer.getTargetView() == edit) {
                previous = pointer;
                pointer = pointer.mNext;
            } else {
                previous.mNext = pointer.recycle();
                pointer = previous.mNext;
            }
        }

        if (header != null) {
            ResultInput result = new ResultInput(edit, getTag(), header.getReferTime());
            long lastTime = header.getReferTime();
            while (header != null) {
                lastTime = analyzeInputResult(lastTime, header.getReferTime(), header.getKeyRecord(), result);
                result.mText = header.getText();
                header = header.recycle();
            }
            analyzeInputTypeAndMethod(edit, result);
            reportResult(result);
        }
    }

    private void analyzeInputTypeAndMethod(EditText edit, ResultInput result) {
        int inputType = edit == null ? 0 : edit.getInputType();
        int klass = inputType & InputType.TYPE_MASK_CLASS;
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        result.mInputType = analyzeInputType(inputType,klass, variation);

        Context context = mHandlerManager == null ? null : mHandlerManager.getActivity();
        if (context == null && edit != null) {
            context = edit.getContext();
        }
        if (context != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            result.mInputMethod = analyzeInputMethod(context, imm);
        }
    }

    private CharSequence analyzeInputMethod(Context context, InputMethodManager imm) {
        CharSequence inputMethodName = null;
        String defInputMethodId = null;
        try {
            //com.iflytek.inputmethod/.FlyIME   may throw SecurityException
            defInputMethodId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!TextUtils.isEmpty(defInputMethodId)) {
            InputMethodInfo defaultInputMethod = null;
            List<InputMethodInfo> methodList = imm.getEnabledInputMethodList();
            for (InputMethodInfo mi : methodList) {
                if (defInputMethodId.equals(mi.getId())) {
                    defaultInputMethod = mi;
                    break;
                }
            }
            if (defaultInputMethod != null) {
                PackageManager pm = context.getPackageManager();
                inputMethodName = defaultInputMethod.loadLabel(pm);
            }
            if (TextUtils.isEmpty(inputMethodName)) {
                int pos = defInputMethodId.lastIndexOf('.');
                if (pos == -1) pos = defInputMethodId.lastIndexOf('/');
                inputMethodName = pos == -1 ? defInputMethodId : defInputMethodId.substring(pos + 1);
            }
        }
        return inputMethodName;
    }

    private CharSequence analyzeInputType(int flag,int klass, int variation) {
        StringBuilder sb = new StringBuilder();
        switch (klass) {
            case InputType.TYPE_CLASS_TEXT:  sb.append("CLASS_TEXT"); break;
            case InputType.TYPE_CLASS_PHONE: sb.append("CLASS_PHONE"); break;
            case InputType.TYPE_CLASS_DATETIME:  sb.append("CLASS_DATETIME"); break;
            case InputType.TYPE_CLASS_NUMBER:
                if(EditorInfo.TYPE_NUMBER_FLAG_SIGNED==(flag&EditorInfo.TYPE_NUMBER_FLAG_SIGNED)){
                    sb.append("CLASS_NUMBER_SIGNED");
                }else if(EditorInfo.TYPE_NUMBER_FLAG_DECIMAL==(flag&EditorInfo.TYPE_NUMBER_FLAG_DECIMAL)){
                    sb.append("CLASS_NUMBER_DECIMAL");
                }else {
                    sb.append("CLASS_NUMBER");
                }
                break;
            default: sb.append("CLASS_DEFAULT"); break;
        }
        sb.append('&');
        switch (variation) {
            case InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:  sb.append("VARIATION_EMAIL_ADDRESS"); break;
            case InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:  sb.append("VARIATION_EMAIL_SUBJECT"); break;
            case InputType.TYPE_TEXT_VARIATION_FILTER:  sb.append("VARIATION_FILTER"); break;
            case InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE:  sb.append("VARIATION_LONG_MESSAGE"); break;
            case InputType.TYPE_TEXT_VARIATION_NORMAL:  sb.append("VARIATION_NORMAL"); break;
            case InputType.TYPE_TEXT_VARIATION_PASSWORD:  sb.append("VARIATION_PASSWORD"); break;
            case InputType.TYPE_TEXT_VARIATION_PERSON_NAME:  sb.append("VARIATION_PERSON_NAME"); break;
            case InputType.TYPE_TEXT_VARIATION_PHONETIC:  sb.append("VARIATION_PHONETIC"); break;
            case InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS:  sb.append("VARIATION_POSTAL_ADDRESS"); break;
            case InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE:  sb.append("VARIATION_SHORT_MESSAGE"); break;
            case InputType.TYPE_TEXT_VARIATION_URI:  sb.append("VARIATION_URI"); break;
            case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:  sb.append("VARIATION_VISIBLE_PASSWORD"); break;
            case InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT:  sb.append("VARIATION_WEB_EDIT_TEXT"); break;
            case InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:  sb.append("VARIATION_WEB_EMAIL_ADDRESS"); break;
            case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:  sb.append("VARIATION_WEB_PASSWORD"); break;
            default: sb.append("VARIATION_DEFAULT"); break;
        }
        return sb;
    }

    private long analyzeInputResult(long lastTime, long refer, SparseIntArray array, ResultInput result) {
        int size = array.size();
        for (int i = 0; i < size; i++) {
            int key = array.keyAt(i);
            int count = array.get(key);
            int optCount;
            if (count > 0) {
                optCount = count;
                result.mTypeInputCount += optCount;
            } else {
                optCount = -count;
                result.mTypeDeleteCount += optCount;
            }
            long currentTime = refer + key;
            if (currentTime - lastTime < sTimeMaxInterval) {
                result.mValidTimeCount += optCount;
                result.mValidTimeCost += (currentTime - lastTime);
            }
            lastTime = currentTime;
        }
        return lastTime;
    }

    @Override
    public void destroy() {
        super.destroy();
        inputListenerFor(mEditText, false);
        mRootView.getViewTreeObserver().removeOnGlobalFocusChangeListener(mFocusListener);
        mEditText = null;
        mRootView = null;
        mTextWatcher = null;
        mFocusListener = null;
        mRecordPointer = mRecordHeader;
        while (mRecordPointer != null) {
            mRecordHeader = mRecordPointer.recycle();
            mRecordPointer = mRecordHeader;
        }
    }

    /**
     * input analytics result after a EditText lost its focus.
     */
    public static class ResultInput extends HandleResult {
        /**
         * timestamp of the first time when user give a input value
         */
        private long mStartTime;

        /**
         * the latest text of the EditText
         */
        private CharSequence mText;

        /**
         * input add char count
         */
        private int mTypeInputCount;

        /**
         * input delete char count
         */
        private int mTypeDeleteCount;

        /**
         * input total time cost considerate as valid
         */
        private long mValidTimeCost;

        /**
         * input time that considerate as valid in a max interaction time 5 second
         * mValidTimeCount/mValidTimeCost will be the average input speed .
         */
        private int mValidTimeCount;

        /**
         * the inputType of EditText
         */
        private CharSequence mInputType;

        /**
         * inputMethod name
         */
        private CharSequence mInputMethod;


        private ResultInput(View target, String tag, long startTime) {
            super(target, tag);
            mStartTime = startTime;
        }

        /**
         * get the latest text of the target EditText
         *
         * @return
         */
        public CharSequence getText() {
            return mText;
        }

        public CharSequence getInputType(){
            return mInputType;
        }

        public CharSequence getInputMethod(){
            return mInputMethod;
        }

        public long getStartTime() {
            return mStartTime;
        }

        public long getEndTime() {
            return getTimestamp();
        }

        public int getInputCount() {
            return mTypeInputCount + mTypeDeleteCount;
        }

        public int getDeleteCount() {
            return mTypeDeleteCount;
        }

        /**
         * @param unit every second for 1000,every minute for 1000*60;
         * @return input speed in measure unit
         */
        public float getInputSpeed(int unit) {
            return (float) (mValidTimeCount * unit) / (mValidTimeCost + 1);
        }

        @Override
        protected void toShortStringImpl(StringBuilder receiver) {
            receiver.append(formatView(getTargetView())).append("{");
            receiver.append("text=").append(getText()).append(',');
            receiver.append("input=").append(getInputCount()).append(',');
            receiver.append("delete=").append(getDeleteCount()).append(',');
            receiver.append("speed=").append((int) getInputSpeed(60 * 1000)).append(',');
            receiver.append("inputType=").append(getInputType()).append(',');
            receiver.append("inputMethod=").append(getInputMethod()).append(',');
            receiver.append("startTime=").append(formatTime(getStartTime(), null)).append(',');
            receiver.append("time=").append(formatTime(getTimestamp(), null)).append(',');
            receiver.setCharAt(receiver.length() - 1, '}');
        }

        @Override
        protected void dumpResultImpl(Map<String, Object> receiver) {
            super.dumpResultImpl(receiver);
            receiver.put("text", getText());
            receiver.put("inputStartTime", getStartTime());
            receiver.put("inputEndTime", getEndTime());
            receiver.put("inputCount", getInputCount());
            receiver.put("deleteCount", getDeleteCount());
            receiver.put("inputSpeed", getInputSpeed(60 * 1000));
            receiver.put("inputType",getInputType());
            receiver.put("inputMethod",getInputMethod());
        }

        @Override
        public void destroy() {
            super.destroy();
            mText = null;
        }
    }
}