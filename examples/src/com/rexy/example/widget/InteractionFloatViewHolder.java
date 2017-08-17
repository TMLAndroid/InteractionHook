package com.rexy.example.widget;

import android.app.Activity;
import android.content.Context;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.rexy.example.extend.InteractionReporter;
import com.rexy.example.extend.ViewUtils;
import com.rexy.hook.InteractionConfig;
import com.rexy.hook.InteractionHook;
import com.rexy.hook.interfaces.IHandleListener;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.interactionhook.example.R;
import com.rexy.widgets.layout.PageScrollView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-07 09:50
 */
public class InteractionFloatViewHolder extends FloatViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, IHandleListener {

    private static String sEmpty = "";
    private static InteractionFloatViewHolder sHolder;

    public static InteractionFloatViewHolder getInstance(Context context) {
        if (sHolder == null || sHolder.isDestroy()) {
            synchronized (InteractionFloatViewHolder.class) {
                if (sHolder == null || sHolder.isDestroy()) {
                    sHolder = new InteractionFloatViewHolder(LayoutInflater.from(context).inflate(R.layout.float_view_interaction, null, false)
                            , (WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
                }
            }
        }
        return sHolder;
    }

    View mIcon;
    View mLayoutOption;
    TextView mTextTitle;
    TextView mTextMessage;
    PageScrollView mScrollView;
    private StringBuilder mLogger = new StringBuilder(1024);
    boolean mExpanded;
    private HashMap<String, List<Pair<Long, Map<String, Object>>>> mResults = new HashMap();
    private List<String> mAccepts = new ArrayList(8);

    public InteractionFloatViewHolder(View rootView, WindowManager windowManager) {
        super(rootView, windowManager);
        mTextTitle = ViewUtils.view(rootView, R.id.title);
        mTextMessage = ViewUtils.view(rootView, R.id.message);
        mLayoutOption = ViewUtils.view(rootView, R.id.expanded_menu);
        mIcon = ViewUtils.view(rootView, R.id.icon);
        mLayoutOption.setVisibility(View.INVISIBLE);
        mIcon.setOnClickListener(this);
        ViewUtils.view(rootView, R.id.clean).setOnClickListener(this);
        ViewUtils.view(rootView, R.id.close).setOnClickListener(this);
        InteractionConfig config = InteractionHook.getConfig();
        initToggleButton(rootView, R.id.toggleInput, config.installInputHandler && config.isHandleAccess);
        initToggleButton(rootView, R.id.toggleProxyClick, config.installProxyClickHandler && config.isHandleAccess);
        initToggleButton(rootView, R.id.togglePreventClick, config.installPreventClickHandler);
        initToggleButton(rootView, R.id.toggleGesture, config.installGestureHandler && config.isHandleAccess);
        initToggleButton(rootView, R.id.toggleFocus, config.installFocusHandler);
        initToggleButton(rootView, R.id.toggleError, config.installPreventClickHandler || config.isHandleAccess);
        initToggleButton(rootView, R.id.togglePage, config.isHandleAccess);
        mScrollView = ViewUtils.view(rootView, R.id.scrollView);
        mScrollView.setMaxHeight((int) (rootView.getResources().getDisplayMetrics().heightPixels * 0.6f));
        if (rootView.getLayoutParams() == null) {
            rootView.setLayoutParams(new ViewGroup.LayoutParams((int) (rootView.getResources().getDisplayMetrics().widthPixels * 0.85f), -2));
        }
    }

    private void initToggleButton(View rootView, int rid, boolean checked) {
        ToggleButton button = ViewUtils.view(rootView, rid);
        button.setChecked(checked);
        onCheckedChanged(button, checked);
        button.setOnCheckedChangeListener(this);
    }

    @Override
    protected View getTouchDragView() {
        return getRootView() == null ? null : getRootView().findViewById(R.id.titleHeader);
    }

    protected void startAnimWhenStateChanged(boolean toReversed, int duration) {
        int from = toReversed ? 0 : 180;
        int to = toReversed ? 180 : 360;
        RotateAnimation animation = new RotateAnimation(from, to, RotateAnimation.RELATIVE_TO_SELF,
                0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(duration);
        animation.setFillAfter(true);
        mIcon.clearAnimation();
        mIcon.startAnimation(animation);
    }

    public void toggleExpand() {
        int duration = 200;
        int gravity = Gravity.BOTTOM;
        int expandType = mExpanded ? ExpandCollapseAnimation.COLLAPSE : ExpandCollapseAnimation.EXPAND;
        ExpandCollapseAnimation.animateView(mLayoutOption, expandType, duration, gravity, new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mExpanded = !mExpanded;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        startAnimWhenStateChanged(!mExpanded, duration);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.icon) {
            toggleExpand();
        }
        if (id == R.id.close) {
            destroy();
        }
        if (id == R.id.clean) {
            mTextMessage.setText(sEmpty);
            mResults.clear();
            mLogger.delete(0, mLogger.length());
        }
    }

    private void updateLoggerAccept(ToggleButton button, boolean isChecked) {
        if (button.getTag() instanceof String) {
            String tag = (String) button.getTag();
            boolean contained = mAccepts.contains(tag);
            if (isChecked) {
                if (!contained) {
                    mAccepts.add(tag);
                }
            } else {
                if (contained) {
                    mAccepts.remove(tag);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String r : mAccepts) {
            sb.append(r).append(",");
        }
        if (sb.length() == 0) {
            sb.append("All Type");
        } else {
            sb.deleteCharAt(sb.length() - 1);
        }
        mTextTitle.setText(sb);
    }

    private boolean acceptLogger(String tag) {
        if (mAccepts.isEmpty() || tag == null) {
            return true;
        }
        for (String r : mAccepts) {
            if (TextUtils.equals(tag, r)) {
                return true;
            }
        }
        return false;
    }

    private void appendLogger(String tag, Map<String, Object> params) {
        mLogger.insert(0, "\n\n");
        mLogger.insert(0, params.toString());
        mLogger.insert(0, ':');
        mLogger.insert(0, tag);
        mTextMessage.setText(mLogger);
        if (ViewCompat.canScrollVertically(mScrollView, 1)) {
            mScrollView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScrollView.scrollTo(0, 0, -1);
                }
            }, 16);
        }
    }

    @Override
    public boolean onHandleResult(IHandleResult result) {
        String tag = result.getTag();
        if (!mResults.containsKey(tag)) {
            mResults.put(tag, new ArrayList<Pair<Long, Map<String, Object>>>(8));
        }
        List<Pair<Long, Map<String, Object>>> list = mResults.get(tag);
        Map<String, Object> params = InteractionReporter.getInstance().dumpReportParams(result);
        list.add(Pair.create(result.getTimestamp(), params));
        if (acceptLogger(tag)) {
            appendLogger(tag, params);
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView instanceof ToggleButton) {
            updateLoggerAccept((ToggleButton) buttonView, isChecked);
        }
        if (mResults.size() > 0) {
            mTextMessage.setText(sEmpty);
            mLogger.delete(0,mLogger.length());
            ArrayList<Pair<Long, Map<String, Object>>> result = new ArrayList();
            Iterator<Map.Entry<String, List<Pair<Long, Map<String, Object>>>>> its = mResults.entrySet().iterator();
            while (its.hasNext()) {
                Map.Entry<String, List<Pair<Long, Map<String, Object>>>> entry = its.next();
                if (acceptLogger(entry.getKey())) {
                    result.addAll(entry.getValue());
                }
            }
            if (result.size() > 0) {
                Collections.sort(result, new Comparator<Pair<Long, Map<String, Object>>>() {
                    @Override
                    public int compare(Pair<Long, Map<String, Object>> l, Pair<Long, Map<String, Object>> r) {
                        return (int) (l.first - r.first);
                    }
                });
                for (Pair<Long, Map<String, Object>> item : result) {
                    Map<String, Object> map = item.second;
                    appendLogger(String.valueOf(map.get("actionType")), map);
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        mAccepts.clear();
        mResults.clear();
        mLogger.delete(0,mLogger.length());
    }

    @Override
    protected void onAttachChanged(boolean attached) {
        if (attached) {
            InteractionReporter.getInstance().registerHandleListener(this);
        } else {
            InteractionReporter.getInstance().unregisterHandleListener(this);
        }
    }
}
