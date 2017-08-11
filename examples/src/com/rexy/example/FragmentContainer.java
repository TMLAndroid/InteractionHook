package com.rexy.example;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rexy.example.extend.BaseFragment;
import com.rexy.example.extend.ViewUtils;
import com.rexy.example.model.TestViewPagerFragment;
import com.rexy.interactionhook.example.R;
import com.rexy.widgets.layout.PageScrollTab;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-07 17:52
 */
public class FragmentContainer extends BaseFragment {
    PageScrollTab mPagerTab;
    ViewPager mViewPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_container, container, false);
        mPagerTab = ViewUtils.view(root, R.id.fragmentTab);
        mViewPager = ViewUtils.view(root, R.id.fragmentViewPager);
        mViewPager.setAdapter(new TestViewPagerFragment(FragmentContainer.this, new Class[]{
                FragmentTabOne.class,
                FragmentTabTwo.class,
                FragmentTabThird.class,
                FragmentTabForth.class,
        }));
        mPagerTab.setViewPager(mViewPager);
        mPagerTab.setTabClickListener(new PageScrollTab.ITabClickEvent() {
            @Override
            public boolean onTabClicked(PageScrollTab parent, View cur, int curPos, View pre, int prePos) {
                mViewPager.setCurrentItem(curPos, true);
                return false;
            }
        });
        return root;
    }
}
