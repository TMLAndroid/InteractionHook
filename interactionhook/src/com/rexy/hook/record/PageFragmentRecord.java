package com.rexy.hook.record;

import java.lang.ref.WeakReference;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-10 13:45
 */
public class PageFragmentRecord extends PageRecord<PageFragmentRecord,Object> {

    PageFragmentRecord mChild;

    PageFragmentRecord(Object fragment, PageFragmentRecord next) {
        mRecorder = new WeakReference(fragment);
        mNext = next;
    }

    @Override
    void destroy() {
        super.destroy();
        if (mRecorder != null) {
            mRecorder.clear();
            mRecorder = null;
        }
        mNext = null;
        if (mChild != null) {
            mChild.destroyAll();
        }
    }

    void  destroyAll() {
        if (mNext != null) {
            mNext.destroyAll();
        }
        destroy();
    }
}
