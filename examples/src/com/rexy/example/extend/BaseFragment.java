package com.rexy.example.extend;

import android.support.v4.app.Fragment;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-09 14:34
 */
public class BaseFragment extends Fragment {
    @Override
    public void onPause() {
        super.onPause();
        InteractionReporter.getInstance().onPause(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        InteractionReporter.getInstance().onResume(this);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        InteractionReporter.getInstance().onHiddenChanged(this,hidden);
    }
}
