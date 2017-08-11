package com.rexy.example.model;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.view.View;

import com.rexy.example.FragmentPageTab;

/**
 * @author: rexy
 */
public class TestViewPagerFragment extends PagerAdapter {
    Class<? extends FragmentPageTab>[] mClsArr;
    FragmentTransaction mCurTransaction = null;
    FragmentManager mFragmentManager = null;
    Fragment mCurrentPrimaryItem = null;
    Fragment mTarget;


    public TestViewPagerFragment(Fragment target, Class<? extends FragmentPageTab>[] testFragmentClass) {
        this.mFragmentManager = target.getChildFragmentManager();
        mClsArr = testFragmentClass;
        mTarget=target;
    }

    @Override
    public Object instantiateItem(View container, int position) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        // Do we already have this fragment?
        String name = makeFragmentName(position);
        Fragment fragment = mFragmentManager.findFragmentByTag(name);
        if (fragment != null) {
            mCurTransaction.show(fragment);
        } else {
            fragment = getItem(position);
            mCurTransaction.add(container.getId(), fragment,
                    makeFragmentName(position));
        }

        if (fragment != mCurrentPrimaryItem) {
            fragment.setMenuVisibility(false);
        }
        return fragment;
    }

    @Override
    public void finishUpdate(View container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    @Override
    public void setPrimaryItem(View container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        mCurTransaction.hide((Fragment) object);
    }

    @Override
    public int getCount() {
        return mClsArr == null ? 0 : mClsArr.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "title" + position;
    }

    @Override
    public int getItemPosition(Object object) {
        return super.getItemPosition(object);
    }

    public Fragment getItem(int i) {
        Bundle arg=new Bundle();
        arg.putInt(FragmentPageTab.TAB_INDEX,i);
        return Fragment.instantiate(mTarget.getContext(),mClsArr[i].getName(),arg);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }

    private String makeFragmentName(int index) {
        return "android:switcher:" + index;
    }

}
