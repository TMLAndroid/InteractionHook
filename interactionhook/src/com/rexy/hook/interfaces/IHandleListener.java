package com.rexy.hook.interfaces;

/**
 * when a {@link IHookHandler} create a result,it should be packaged in {@link IHandleResult} and send to all observer listeners.
 *
 * Created by rexy on 17/8/13.
 */

public interface  IHandleListener {
    boolean onHandleResult(IHandleResult result);
}