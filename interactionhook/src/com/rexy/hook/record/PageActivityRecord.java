package com.rexy.hook.record;

import android.app.Activity;

import java.lang.ref.WeakReference;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-10 13:46
 */
public class PageActivityRecord extends PageRecord {
    PageFragmentRecord mFragmentRecords;
    PageActivityRecord mNext;
    WeakReference<Activity> mActivityRef;

    PageActivityRecord(Activity activity, PageActivityRecord next) {
        mActivityRef = new WeakReference(activity);
        mNext = next;
    }

    PageFragmentRecord findFragmentRecordNoRecursive(Object fragment, PageFragmentRecord fromRecord) {
        PageFragmentRecord recorder = fromRecord;
        while (recorder != null) {
            if (recorder.mFragment != null && recorder.mFragment.get() == fragment) {
                return recorder;
            } else {
                recorder = recorder.mNext;
            }
        }
        return null;
    }

    PageFragmentRecord findFragmentRecordRecursive(Object fragment, PageFragmentRecord fromRecord) {
        PageFragmentRecord find = null;
        if (fromRecord != null) {
            if (fromRecord.mFragment != null && fromRecord.mFragment.get() == fragment) {
                find = fromRecord;
            } else {
                if (fromRecord.mNext != null) {
                    find = findFragmentRecordRecursive(fragment, fromRecord.mNext);
                }
                if (fromRecord.mChild != null && find == null) {
                    find = findFragmentRecordRecursive(fragment, fromRecord.mChild);
                }
            }
        }
        return find;
    }

    PageFragmentRecord findPreviousFragmentRecord(PageFragmentRecord cur, PageFragmentRecord from) {
        PageFragmentRecord peek = from;
        if (!(from == cur || from == null || cur == null)) {
            while (peek.mNext != null) {
                if (peek.mNext == cur) {
                    return peek;
                } else {
                    peek = peek.mNext;
                }
            }
        }
        return null;
    }

    void record(Object fragment, Object parentFragment, int optionCode) {
        PageFragmentRecord fragmentRecord = null;
        if (mFragmentRecords == null) {
            fragmentRecord = new PageFragmentRecord(fragment, mFragmentRecords);
            mFragmentRecords = fragmentRecord;
        } else {
            if (parentFragment == null) {
                fragmentRecord = findFragmentRecordNoRecursive(fragment, mFragmentRecords);
                if (fragmentRecord == null) {
                    fragmentRecord = new PageFragmentRecord(fragment, mFragmentRecords);
                    mFragmentRecords = fragmentRecord;
                } else {
                    if (optionCode == PageOperate.OPERATE_DESTROY) {
                        destroyFragment(fragmentRecord, null);
                        return;
                    }
                }
            } else {
                PageFragmentRecord parentRecord = findFragmentRecordRecursive(parentFragment, mFragmentRecords);
                if (parentRecord != null) {
                    fragmentRecord = findFragmentRecordNoRecursive(fragment, parentRecord.mChild);
                    if (fragmentRecord == null) {
                        fragmentRecord = new PageFragmentRecord(fragment, parentRecord.mChild);
                        parentRecord.mChild = fragmentRecord;
                    } else {
                        if (optionCode == PageOperate.OPERATE_DESTROY) {
                            destroyFragment(fragmentRecord, parentRecord);
                            return;
                        }
                    }
                }
            }
        }
        if (fragmentRecord != null) {
            fragmentRecord.record(optionCode);
        }
    }

    /**
     * destroy target fragment inner
     *
     * @param target current fragment just execute destroy
     * @param targetParent mab be a nested parent fragment record.
     */
    private void destroyFragment(PageFragmentRecord target, PageFragmentRecord targetParent) {
        PageFragmentRecord previous;
        if (targetParent == null) {
            previous = findPreviousFragmentRecord(target, mFragmentRecords);
            if (previous == null) {
                mFragmentRecords = target.mNext;
            } else {
                previous.mNext = target.mNext;
            }
        } else {
            previous = findPreviousFragmentRecord(target, targetParent.mChild);
            if (previous == null) {
                targetParent.mChild = target.mNext;
            } else {
                previous.mNext = target.mNext;
            }
        }
        target.destroy();
    }

    /**
     * destroy self and all its fragment record.
     */
    void destroy() {
        super.destroy();
        if (mActivityRef != null) {
            mActivityRef.clear();
            mActivityRef = null;
        }
        mNext = null;
        if (mFragmentRecords != null) {
            mFragmentRecords.destroyAll();
            mFragmentRecords = null;
        }
    }
}
