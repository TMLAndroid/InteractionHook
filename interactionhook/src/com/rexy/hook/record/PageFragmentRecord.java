package com.rexy.hook.record;

import java.lang.ref.WeakReference;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-10 13:45
 */
public class PageFragmentRecord extends PageRecord {
    PageFragmentRecord mNext;
    PageFragmentRecord mChild;
    WeakReference mFragment;

    PageFragmentRecord(Object fragment, PageFragmentRecord next) {
        mFragment = new WeakReference(fragment);
        mNext = next;
    }

    @Override
    void destroy() {
        super.destroy();
        if (mFragment != null) {
            mFragment.clear();
            mFragment = null;
        }
        mNext = null;
        if (mChild != null) {
            mChild.destroyAll();
        }
    }

    void destroyAll() {
        if (mNext != null) {
            mNext.destroyAll();
        }
        destroy();
    }
}
