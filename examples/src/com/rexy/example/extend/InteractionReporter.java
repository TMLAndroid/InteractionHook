package com.rexy.example.extend;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.rexy.hook.InteractionConfig;
import com.rexy.hook.InteractionHook;
import com.rexy.hook.handler.HandleResult;
import com.rexy.hook.handler.HandlerGesture;
import com.rexy.hook.handler.HandlerInput;
import com.rexy.hook.handler.HandlerPreventFastClick;
import com.rexy.hook.handler.HandlerProxyClick;
import com.rexy.hook.interfaces.IHandleListener;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.interactionhook.example.BuildConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO:功能说明
 *
 * @author: rexy
 * @date: 2017-08-14 10:07
 */
public class InteractionReporter implements IHandleListener {

    private static InteractionReporter sReporter;

    public static InteractionReporter getInstance() {
        if (sReporter == null) {
            synchronized (InteractionReporter.class) {
                if (sReporter == null) {
                    sReporter = new InteractionReporter();
                }
            }
        }
        return sReporter;
    }

    public static void setInteractionAccess(boolean installInteractionTrack) {
        if (InteractionConfig.isDevMode) {
            installInteractionTrack = true;
        }
        if (InteractionConfig.isHandleAccess != installInteractionTrack) {
            InteractionConfig.isHandleAccess = installInteractionTrack;
            InteractionHook.updateConfig(InteractionHook.getConfig());
        }
    }

    private int mFastClickTimes = 0;

    private void log(CharSequence msg) {
        Log.d("rexy_hook", String.valueOf(msg));
    }

    public void init(Application application) {
        InteractionHook.init(application, this);

        InteractionConfig config = InteractionHook.getConfig();
        config.installFocusHandler = true;
        config.handleFocusEnable = true;
        InteractionConfig.isDevMode = false;
        InteractionConfig.isHandleAccess = false;
        InteractionHook.updateConfig(config);

        if (Build.VERSION.SDK_INT >= 14) {
            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                }

                @Override
                public void onActivityStarted(Activity activity) {
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    if (InteractionConfig.isHandleAccess) {
                        reportActivityLifeCycle(activity, "appear");
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    if (InteractionConfig.isHandleAccess) {
                        reportActivityLifeCycle(activity, "disappear");
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                }
            });
        }
        if (BuildConfig.DEBUG) {
            setInteractionAccess(true);
        }
    }

    public void registerHandleListener(IHandleListener l) {
        InteractionHook.registerHandleListener(l);
    }

    public void unregisterHandleListener(IHandleListener l) {
        InteractionHook.unregisterHandleListener(l);
    }

    public boolean onTouch(Activity activity, MotionEvent ev) {
        return InteractionHook.onTouch(activity, ev);
    }

    public void onResume(Fragment fragment) {
        if (InteractionConfig.isHandleAccess) {
            InteractionHook.notify(new ResultPage(fragment, "appear", false));
        }
    }

    public void onPause(Fragment fragment) {
        if (InteractionConfig.isHandleAccess) {
            InteractionHook.notify(new ResultPage(fragment, "disappear", false));
        }
    }

    public void onHiddenChanged(Fragment fragment, boolean hidden) {
        if (InteractionConfig.isHandleAccess) {
            InteractionHook.notify(new ResultPage(fragment, hidden ? "disappear" : "appear", true));
        }
    }

    private void reportActivityLifeCycle(Activity activity, String eventType) {
        boolean needReport = !(activity instanceof FragmentActivity);
        if (!needReport) {
            FragmentManager manager = ((FragmentActivity) activity).getSupportFragmentManager();
            List<Fragment> fragments = manager == null ? null : manager.getFragments();
            needReport = fragments == null || fragments.size() == 0;
        }
        if (needReport) {
            InteractionHook.notify(new ResultPage(activity, eventType, false));
        }
    }

    @Override
    public boolean onHandleResult(IHandleResult result) {
        if (result instanceof HandlerPreventFastClick.ResultPreventFastClick) {
            mFastClickTimes++;
            if ((BuildConfig.DEBUG) && mFastClickTimes % 3 == 0) {
                Toast.makeText(result.getActivity(), "点这么快，手酸吗", Toast.LENGTH_SHORT).show();
            }
        }
        if (result instanceof ResultPage ||
                result instanceof HandlerInput.ResultInput ||
                result instanceof HandlerProxyClick.ResultProxyClick ||
                result instanceof HandlerGesture.ResultGesture) {
            if (InteractionConfig.isHandleAccess) {
                Map<String, Object> params = dumpReportParams(result);
                if (InteractionConfig.isDevMode) {
                    log(params.toString());
                }
            }
        }
        return false;
    }

    private   Activity findActivity(Context context) {
        while (true) {
            if (context instanceof Activity) {
                return (Activity) context;
            } else if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else {
                return null;
            }
        }
    }

    private Fragment findTopVisibleFragment(FragmentManager fragmentManager) {
        List<Fragment> fragments = fragmentManager == null ? null : fragmentManager.getFragments();
        if (fragments != null && fragments.size() > 0) {
            for(Fragment fragment:fragments){
                if(fragment.isVisible()){
                    return fragment;
                }
            }
        }
        return null;
    }

    private Fragment findCloselyFragmentFromChildView(FragmentManager fragmentManager, View childView, Rect childPosition, Rect fragmentPosition) {
        List<Fragment> fragments = fragmentManager == null ? null : fragmentManager.getFragments();
        if (fragments != null && fragments.size() > 0) {
            int[] position = new int[]{0, 0};
            for (Fragment child : fragments) {
                if (child != null && child.isVisible()) {
                    View fragmentView = child.getView();
                    if (fragmentView != null) {
                        fragmentView.getLocationOnScreen(position);
                        fragmentPosition.left = position[0];
                        fragmentPosition.top = position[1];
                        fragmentPosition.right = fragmentPosition.left + fragmentView.getWidth();
                        fragmentPosition.bottom = fragmentPosition.top + fragmentView.getHeight();
                        if (fragmentPosition.contains(childPosition)) {
                            boolean find = childView == null;
                            if (!find) {
                                do {
                                    if (childView == fragmentView) {
                                        find = true;
                                        break;
                                    }
                                    if (childView.getParent() instanceof View) {
                                        childView = (View) childView.getParent();
                                    } else {
                                        childView = null;
                                    }
                                } while (childView != null);
                            }
                            if (find) {
                                return child;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    //最多找四层 Fragment.
    private void dumpFragmentPathFromActivity(Activity activity, View targetView, List<Fragment> result) {
        if (activity instanceof FragmentActivity) {
            Fragment fragment = findTopVisibleFragment(((FragmentActivity) activity).getSupportFragmentManager());
            if (fragment != null) {
                result.add(fragment);
                if (targetView != null && fragment.getHost() != null) {
                    Rect targetRect = new Rect();
                    Rect tempRect = new Rect();
                    int[] position = new int[]{0, 0};
                    targetView.getLocationOnScreen(position);
                    targetRect.left = position[0];
                    targetRect.top = position[1];
                    targetRect.right = targetRect.left + targetView.getWidth();
                    targetRect.bottom = targetRect.top + targetView.getHeight();
                    int maxTimes = 3;
                    do {
                        fragment = findCloselyFragmentFromChildView(fragment.getChildFragmentManager(), targetView, targetRect, tempRect);
                        if (fragment == null) {
                            break;
                        } else {
                            result.add(fragment);
                        }
                    } while (--maxTimes > 0 && fragment.getHost() != null);
                }
            }
        }
    }

    private void dumpFragmentPathFromFragment(Fragment fragment, List<Fragment> result) {
        if (fragment != null) {
            result.add(0, fragment);
            if (Build.VERSION.SDK_INT >= 14 && fragment.getParentFragment() != null) {
                dumpFragmentPathFromFragment(fragment.getParentFragment(), result);
            }
        }
    }

    public Map<String, Object> dumpReportParams(IHandleResult result) {
        Fragment fragment = null;
        if (result instanceof ResultPage) {
            fragment = ((ResultPage) result).getFragment();
        }
        Map<String, Object> arg = result.dumpResult(null);
        Activity activity = result.getActivity();
        View targetView = result.getTargetView();
        if(activity!=null||targetView!=null){
            if(activity==null){
                activity=findActivity(targetView.getContext());
            }
            StringBuilder screenPathBuilder = new StringBuilder();
            List<Fragment> fragments = new ArrayList(2);
            if (fragment == null) {
                if (targetView != null && activity != null) {
                    dumpFragmentPathFromActivity(activity, targetView, fragments);
                }
            } else {
                dumpFragmentPathFromFragment(fragment, fragments);
            }
            int size = fragments.size();
            if (size > 0) {
                arg.put("screenId", System.identityHashCode(fragments.get(size - 1)));
            } else {
                arg.put("screenId", activity == null ? 0 : System.identityHashCode(activity));
            }
            for (int i = size - 1; i >= 0; i--) {
                fragment = fragments.get(i);
                screenPathBuilder.insert(0, fragment.getClass().getSimpleName());
                screenPathBuilder.insert(0, '>');
            }
            if (activity != null) {
                screenPathBuilder.insert(0, activity.getClass().getSimpleName());
            }
            arg.put("screenPath", screenPathBuilder.toString());
        }
        return arg;
    }

    public static class ResultPage extends HandleResult {
        private Fragment mFragment;
        private String mEventType;
        private boolean mHiddenChanged;

        protected ResultPage(Fragment fragment, String eventType, boolean hiddenChanged) {
            super(null, "page");
            mFragment = fragment;
            mEventType = eventType;
            mHiddenChanged = hiddenChanged;
            if (mFragment != null) {
                setActivity(mFragment.getActivity());
            }
        }

        protected ResultPage(Activity activity, String eventType, boolean hiddenChanged) {
            super(null, "page");
            mEventType = eventType;
            mHiddenChanged = hiddenChanged;
            setActivity(activity);
        }

        public Fragment getFragment() {
            return mFragment;
        }

        public String getEventType() {
            return mEventType;
        }

        public boolean isHiddenChanged() {
            return mHiddenChanged;
        }

        @Override
        protected void toShortStringImpl(StringBuilder receiver) {
            receiver.append(getFragment()).append("{");
            receiver.append("eventType=").append(getEventType()).append(',');
            receiver.append("hiddenChange=").append(isHiddenChanged()).append(',');
            receiver.append("timestamp=").append(formatTime(getTimestamp(), null)).append(',');
            receiver.setCharAt(receiver.length() - 1, '}');
        }

        @Override
        protected void dumpResultImpl(Map<String, Object> receiver) {
            super.dumpResultImpl(receiver);
            receiver.put("eventType", getEventType());
            receiver.put("timestamp", getTimestamp());
        }

        @Override
        public void destroy() {
            super.destroy();
            mFragment = null;
        }
    }
}