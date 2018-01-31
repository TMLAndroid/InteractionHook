package com.rexy.example.extend;

import android.util.Log;

import com.rexy.hook.handler.HandlerProxyClick;
import com.rexy.hook.interfaces.IHandleResult;

import java.util.LinkedList;

/**
 * Created by liuzhenhui on 2018/1/30.
 */

public class UtilClickApm {
    private static long threshSeconds = 5 * 1000;//5s = 5000ms
    private static LinkedList<HandlerProxyClick.ResultProxyClick> clickList = new LinkedList<>();

    public static final void checkClickApm(IHandleResult click) {
        if (click instanceof HandlerProxyClick.ResultProxyClick) {
            HandlerProxyClick.ResultProxyClick currClick = (HandlerProxyClick.ResultProxyClick)click;
            int times = 0;
            //遍历寻找相同的点击
            for (int i = clickList.size() - 1; i >= 0; i--) {
                HandlerProxyClick.ResultProxyClick tempHistoryClick = clickList.get(i);
                if (currClick.equals(tempHistoryClick)) {
                    long duration = currClick.getDownTime() - tempHistoryClick.getDownTime();
                    if (duration < threshSeconds) {
                        times++;
                        log("时间差小于阈值次数"+times);
                    }else {
                        log("时间差过大："+duration);
                        break;
                    }
                    //重复点击超过3次（算上本次）
                    if (times >= 2) {
                        Log.d("lzh", "==================================5s内3次以上"+currClick.dumpStringResult());
                        break;
                    }
                }
            }
            //加入队列
            clickList.addLast(currClick);
            //不需要保留过多
            while (clickList.size() > 5) {
                clickList.removeFirst();
            }
        }
    }

    private static void log(String msg) {
        Log.d("lzh", msg);
    }
}
