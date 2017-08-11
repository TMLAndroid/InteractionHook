package com.rexy.hook.record;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.rexy.hook.InteractionHook;
import com.rexy.hook.handler.HandleResult;
import com.rexy.hook.handler.HookHandler;
import com.rexy.hook.interfaces.IHandleListener;

import java.util.Map;

/**
 * record every activity and fragment of its lifecycle,track and analyze the page change .
 *
 * @author: rexy
 * @date: 2017-08-09 15:52
 */
public class PageTracker extends HookHandler {
    /**
     * current activity that is resumed.
     */
    Activity mCurrentActivity;
    /**
     * current fragment that is visible
     */
    Object mCurrentFragment;
    /**
     * Activity record include Fragment record inner.
     */
    PageActivityRecord mRecords;

    public PageTracker(String tag) {
        super(tag);
    }

    public static void testPrint(CharSequence msg) {
        Log.d("rexy_page", msg.toString());
    }

    public void setHandleListener(IHandleListener listener) {
        mHandleListener = listener;
    }

    public void onResume(Activity activity, Object fragment, Object parentFragment) {
        record(activity, fragment, parentFragment, PageOperate.OPERATE_RESUME);
        if (fragment == null) {
            setCurrentActivity(activity);
        } else {
            setCurrentFragment(fragment);
        }
    }

    public void onPause(Activity activity, Object fragment, Object parentFragment) {
        record(activity, fragment, parentFragment, PageOperate.OPERATE_PAUSE);
    }

    public void onHiddenChanged(Activity activity, Object fragment, Object parentFragment, boolean hidden) {
        record(activity, fragment, parentFragment, hidden ? PageOperate.OPERATE_HIDE : PageOperate.OPERATE_SHOW);
        if (!hidden) {
            setCurrentFragment(fragment);
        }
    }

    public void onDestroy(Activity activity, Object fragment, Object parentFragment) {
        record(activity, fragment, parentFragment, PageOperate.OPERATE_DESTROY);
    }

    private PageActivityRecord findActivityRecord(Activity activity) {
        PageActivityRecord peek = mRecords;
        while (peek != null) {
            if (peek.mRecorder != null && peek.mRecorder.get() == activity) {
                return peek;
            } else {
                peek = peek.mNext;
            }
        }
        return null;
    }

    private PageActivityRecord findPreviousActivityRecord(PageActivityRecord record) {
        if (!(mRecords == record || mRecords == null)) {
            PageActivityRecord peek = mRecords;
            while (peek.mNext != null) {
                if (peek.mNext == record) {
                    return peek;
                } else {
                    peek = peek.mNext;
                }
            }
        }
        return null;
    }

    /**
     * record option for current activity or fragment .
     *
     * @param activity       current activity not null
     * @param fragment       current operated fragment maybe null if there is no fragment interaction,
     * @param parentFragment parent fragment of current operated fragment if it exists
     * @param optionCode     operate code
     */
    private PageRecord record(Activity activity, Object fragment, Object parentFragment, int optionCode) {
        PageRecord result = null;
        PageActivityRecord activityRecord = findActivityRecord(activity);
        if (fragment == null) {
            if (activityRecord == null) {
                activityRecord = new PageActivityRecord(activity, mRecords);
                mRecords = activityRecord;
            }
            result = activityRecord;
            if (optionCode == PageOperate.OPERATE_DESTROY) {
                //change link and destroy target activity record.
                PageActivityRecord parent = findPreviousActivityRecord(activityRecord);
                if (parent == null) {
                    mRecords = activityRecord.mNext;
                } else {
                    parent.mNext = activityRecord.mNext;
                }
                activityRecord.destroy();
            } else {
                activityRecord.record(optionCode);
            }
        } else {
            if (activityRecord != null) {
                result = activityRecord.record(fragment, parentFragment, optionCode);
            }
        }
        if(result!=null&&optionCode!=PageOperate.OPERATE_DESTROY){
            testPrint("----------->"+result.toString());
        }
        return result;
    }

    private void setCurrentActivity(Activity activity) {
        if (mCurrentActivity != activity) {
            mCurrentActivity = activity;
        }
        testPrint(dumpRecord(new StringBuilder()).toString());
    }

    private void setCurrentFragment(Object fragment) {
        if (mCurrentFragment != fragment) {
            mCurrentFragment = fragment;
            testPrint(dumpRecord(new StringBuilder()).toString());
        }
    }

    public StringBuilder dumpRecord(StringBuilder sb) {
        sb = sb == null ? new StringBuilder() : sb;
        PageActivityRecord peekActivity = mRecords;
        while (peekActivity != null) {
            dumpRecordActivity(peekActivity, sb);
            peekActivity = peekActivity.mNext;
        }
        return sb.append("\n\n");
    }

    void dumpRecordActivity(PageActivityRecord target, StringBuilder sb) {
        Activity activity = target.mRecorder == null ? null : target.mRecorder.get();
        if (activity != null) {
            sb.append(mCurrentActivity == activity ? "*|-" : " |-");
            sb.append(activity.getClass().getSimpleName());
            sb.append('@').append(activity.hashCode());
            if (target.mOperator != null) {
                target.dumpRecordOption(sb);
            }
            sb.append('\n');
        }
        if (target.mFragmentRecords != null) {
            dumpRecordFragment(target.mFragmentRecords, sb, "   ");
        }
    }

    void dumpRecordFragment(PageFragmentRecord target, StringBuilder sb, String space) {
        Object fragment = target.mRecorder == null ? null : target.mRecorder.get();
        if (fragment != null) {
            sb.append(space);
            sb.append(fragment == mCurrentFragment ? "*|-" : " |-");
            sb.append(fragment.getClass().getSimpleName());
            sb.append('@').append(fragment.hashCode());
            if (target.mOperator != null) {
                target.dumpRecordOption(sb);
            }
            sb.append('\n');
        }
        if (target.mChild != null) {
            dumpRecordFragment(target.mChild, sb, space + "    ");
        }
        if (target.mNext != null) {
            dumpRecordFragment(target.mNext, sb, space);
        }
    }

    @Override
    public boolean handle(InteractionHook caller) {
        return false;
    }


    public static class ResultPage extends HandleResult {

        protected ResultPage(View target, String tag) {
            super(target, tag);
        }

        @Override
        protected void toShortStringImpl(StringBuilder receiver) {

        }

        @Override
        protected void dumpResultImpl(Map<String, Object> receiver) {

        }
    }
}
