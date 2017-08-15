package com.rexy.example.extend;

import android.app.Activity;
import android.app.Application;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.rexy.hook.InteractionConfig;
import com.rexy.hook.InteractionHook;
import com.rexy.hook.handler.HandlerGesture;
import com.rexy.hook.handler.HandlerInput;
import com.rexy.hook.handler.HandlerProxyClick;
import com.rexy.hook.interfaces.IHandleListener;
import com.rexy.hook.interfaces.IHandleResult;
import com.rexy.interactionhook.example.BuildConfig;

import java.util.ArrayList;
import java.util.HashMap;
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

    private void log(CharSequence msg) {
        Log.d("rexy_hook", String.valueOf(msg));
    }

    public void init(Application application) {
        InteractionHook.init(application, this);

        InteractionConfig config = InteractionHook.getConfig();
        config.installFocusHandler = false;
        config.handleFocusEnable = false;
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
                        report(activity, "appear");
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    if (InteractionConfig.isHandleAccess) {
                        report(activity, "disappear");
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
        InteractionConfig.isDevMode = BuildConfig.DEBUG;
        setInteractionAccess(true);
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
            report(fragment, "appear", false);
        }
    }

    public void onPause(Fragment fragment) {
        if (InteractionConfig.isHandleAccess) {
            report(fragment, "disappear", false);
        }
    }

    public void onHiddenChanged(Fragment fragment, boolean hidden) {
        if (InteractionConfig.isHandleAccess) {
            report(fragment, hidden ? "disappear" : "appear", true);
        }
    }

    @Override
    public boolean onHandleResult(IHandleResult result) {
        if (result instanceof HandlerInput.ResultInput ||
                result instanceof HandlerProxyClick.ResultProxyClick ||
                result instanceof HandlerGesture.ResultGesture) {
            if (InteractionConfig.isHandleAccess) {
                report(result.getActivity(), null, result.dumpResult(null), result.getTargetView());
            }
        }
        return false;
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

    private void report(Activity activity, String eventType) {
        boolean needReport = !(activity instanceof FragmentActivity);
        if (!needReport) {
            FragmentManager manager = ((FragmentActivity) activity).getSupportFragmentManager();
            List<Fragment> fragments = manager == null ? null : manager.getFragments();
            needReport = fragments == null || fragments.size() == 0;
        }
        if (needReport) {
            HashMap<String, Object> arg = new HashMap();
            arg.put("actionType", "page");
            arg.put("eventType", eventType);
            arg.put("timestamp", System.currentTimeMillis());
            report(activity, null, arg, null);
        }
    }

    private void report(Fragment fragment, String eventType, boolean fromHiddenChanged) {
        HashMap<String, Object> arg = new HashMap();
        arg.put("actionType", "page");
        arg.put("eventType", eventType);
        arg.put("timestamp", System.currentTimeMillis());
        report(fragment.getActivity(), fragment, arg, fragment.getView());
    }

    private void report(Activity activity, Fragment fragment, Map<String, Object> arg, View targetView) {
        StringBuilder screenPathBuilder = new StringBuilder();
        List<Fragment> fragments = new ArrayList(2);
        if (fragment == null) {
            if (targetView != null) {
                dumpFragmentPathFromActivity(activity, targetView, fragments);
            }
        } else {
            dumpFragmentPathFromFragment(fragment, fragments);
        }
        int size = fragments.size();
        if (size > 0) {
            arg.put("screenId", System.identityHashCode(fragments.get(size - 1)));
        } else {
            arg.put("screenId", System.identityHashCode(activity));
        }
        for (int i = size - 1; i >= 0; i--) {
            fragment = fragments.get(i);
            screenPathBuilder.insert(0, fragment.getClass().getSimpleName());
            screenPathBuilder.insert(0, '>');
        }
        screenPathBuilder.insert(0, activity.getClass().getSimpleName());
        arg.put("screenPath", screenPathBuilder.toString());
        log(arg.toString());
    }
}