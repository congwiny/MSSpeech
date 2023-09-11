package com.msspeech.demo.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.msspeech.demo.App;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Logger {
    private static final String KEY_DEBUG = "com.zhaomi.sj.common.utils.Logger.enabled";

    private static final SharedPreferences sp = App.Companion.getCONTEXT().getSharedPreferences("common", Context.MODE_PRIVATE);
    private static boolean DEBUG = false;

    public static void setDebug(boolean isDebug) {
        sp.edit().putBoolean(KEY_DEBUG, isDebug).apply();
        DEBUG = isDebug;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    //    private static boolean DEBUG = true;
    private String mTag = "Logger";

    private long mStartTime = 0;
    private long mLastTime = 0;
    private int mStackLevel = 0;
    private boolean mShowClass = false;


    public Logger(String tag) {
        mTag = tag;
        restart();
    }

    public void setTag(String tag) {
        if (tag != null) {
            mTag = tag;
        }
    }

    public Logger(Class clazz) {
        this(clazz == null ? null : clazz.getSimpleName());
    }

    /**
     * 设置获取调用栈的层级
     */
    public Logger setStackLevel(int level) {
        mStackLevel = level;
        return this;
    }

    public Logger setShowClass(boolean isShown) {
        mShowClass = isShown;
        return this;
    }

    private List<String> mCommonLog = new ArrayList<>();
    private String mCommonLogStr = "";

    public Logger addCommon(Object param) {
        if (param != null) {
            mCommonLog.add(param.toString());
            mCommonLogStr = null;
        }

        return this;
    }

    public Logger clearCommon() {
        mCommonLog.clear();
        mCommonLogStr = null;
        return this;
    }

    private String getCommonLog() {
        if (!DEBUG) {
            return "RELEASE";
        }
        if (mCommonLogStr == null) {
            StringBuilder builder = new StringBuilder("");
            for (String log : mCommonLog) {
                builder.append(log).append(", ");
            }
            mCommonLogStr = builder.toString();
        }
        return mCommonLogStr;
    }

    public void i(Object... param) {
        _i(mTag, parseWithCommon(param));
    }

    public void i2(Object... param) {
        try {
            String msg = parseWithCommon(param);
            int p = 2048;
            long length = msg.length();
            if (length < p || length == p)
                _i(mTag, msg);
            else {
                while (msg.length() > p) {
                    String logContent = msg.substring(0, p);
                    msg = msg.replace(logContent, "");
                    _i(mTag, logContent);
                }
                _i(mTag, "续上方-->" + msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void ci(Object... param) {
        _i(mTag, getSimpleCaller() + ": " + parseWithCommon(param));
    }

    public void cd(Object... param) {
        _d(mTag, getSimpleCaller() + ": " + parseWithCommon(param));
    }

    public void throwable(Throwable t) {
        printStackTrace0(mTag, t);
    }

    public static void throwable0(String tag, Throwable t) {
        printStackTrace0(tag, t);
    }

    public void printStackTrace() {
        printStackTrace0(mTag, new Throwable());
    }

    public static void printStackTrace0(String tag, Throwable t) {
        StringWriter writer = new StringWriter();
        PrintWriter print = new PrintWriter(writer);
        t.printStackTrace(print);
        i0(tag, writer.toString());
        close(print);
        close(writer);
    }

    private String parseWithCommon(Object[] params) {
        if (!DEBUG) {
            return "RELEASE";
        }
        String out = parse(params);
        return getCommonLog() + out;
    }

    private static String parse(Object[] params) {
        if (!DEBUG) {
            return "RELEASE";
        }
        StringBuilder builder = new StringBuilder();
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                builder.append(param == null ? "null" : param);
                if (i != params.length - 1) {
                    builder.append(", ");
                }
            }
        }
        return builder.toString();
    }


    public static String getSimpleCaller() {
        if (!DEBUG) {
            return "RELEASE";
        }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (trace == null && trace.length == 0) {
            return "null";
        } else if (trace.length < 5) {
            return getMethod(trace[trace.length - 1]);
        } else {
            return getMethod(trace[4]);
        }
    }

    public static String getCaller() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (trace == null && trace.length == 0) {
            return "null";
        } else if (trace.length < 5) {
            return getSignature(trace[trace.length - 1]);
        } else {
            return getSignature(trace[4]);
        }
    }

    private static String getSignature(StackTraceElement element) {
        if (element == null) {
            return "null";
        } else {
            return element.getClassName() + "." + element.getMethodName() + "()";
        }
    }


    private static String getMethod(StackTraceElement element) {
        if (element == null) {
            return "null";
        } else {
            return element.getMethodName() + "()";
        }
    }

    private static int sPid = -1;

    private static int getPid() {
        return sPid == -1 ? (sPid = android.os.Process.myPid()) : sPid;
    }

    private static String mProcessName = null;

    public static String getProcessName() {
        if (mProcessName != null) {
            return mProcessName;
        }
        int pid = getPid();
        try {
            Context context = App.Companion.getCONTEXT();
            if (context != null) {
                ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
                    if (appProcess.pid == pid) {
                        return mProcessName = appProcess.processName;
                    }
                }
            }
        } catch (Throwable t) {
        }

        return "null";
    }

    public void in() {
        i("[_in]", getProcessName(), getCaller());
    }

    public void out() {
        i("[out]", getProcessName(), getCaller());
    }

    public static void in(String tag) {
        _i(tag, "[in], " + getProcessName() + ", " + getCaller());
    }

    public static void out(String tag) {
        _i(tag, "[out], " + getProcessName() + ", " + getCaller());
    }

    public void d(Object... param) {
        _d(mTag, parseWithCommon(param));
    }

    public void e(Object... param) {
        _i(mTag, parseWithCommon(param));
    }

    public void v(Object... param) {
        _v(mTag, parseWithCommon(param));
    }

    public void w(Object... param) {
        _w(mTag, parseWithCommon(param));
    }

    public void restart() {
        mStartTime = mLastTime = System.currentTimeMillis();
    }

    public void tick(String... message) {
        long now = System.currentTimeMillis();
        long total = now - mStartTime;
        long step = now - mLastTime;
        mLastTime = now;
        String method = "";
        String clazz = "";
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            if (stack != null && stack.length > 4 + mStackLevel) {
                StackTraceElement element = stack[3 + mStackLevel];
                method = element.getMethodName();
                if (mShowClass) {
                    clazz = element.getClassName();
                    clazz = clazz.substring(clazz.lastIndexOf('.') + 1);
                }
            }
        } catch (Throwable e) {
        }
        i(String.format(Locale.CHINA, "tick[%s.%s](%d,%d): %s", clazz, method, total, step, parseWithCommon(message)));
    }

    public static void i0(Class<?> clazz, Object... param) {
        i0(clazz.getSimpleName(), param);
    }

    public static void i0(String tag, Object... param) {
        _i(tag, parse(param));
    }

    public static void ci0(String tag, Object... param) {
        _i(tag, getSimpleCaller() + ": " + parse(param));
    }

    public static void ci0(Class clazz, Object... param) {
        _i(clazz.getSimpleName(), getSimpleCaller() + ": " + parse(param));
    }

    public static void e0(String tag, Object... param) {
        _e(tag, parse(param));
    }

    public static void v0(String tag, Object... param) {
        _v(tag, parse(param));
    }

    public static void w0(String tag, Object... param) {
        _w(tag, parse(param));
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final int LINE_LENGTH = 2000;

    private static List<String> splitByLength(String content) {
        List<String> list = new LinkedList<>();
        do {
            int end = Math.min(LINE_LENGTH, content.length());
            list.add(content.substring(0, end));
            content = content.substring(end);
        } while (content.length() > 0);
        return list;
    }

    private static void _i(String tag, String content) {
        if (!DEBUG) return;
        for (String c : splitByLength(content)) {
            Log.i(tag, c);
        }
    }

    private static void _v(String tag, String content) {
        if (!DEBUG) return;
        for (String c : splitByLength(content)) {
            Log.v(tag, c);
        }
    }

    private static void _w(String tag, String content) {
        if (!DEBUG) return;
        for (String c : splitByLength(content)) {
            Log.w(tag, c);
        }
    }

    private static void _d(String tag, String content) {
        if (!DEBUG) return;
        for (String c : splitByLength(content)) {
            Log.d(tag, c);
        }
    }

    private static void _e(String tag, String content) {
        if (!DEBUG) return;
        for (String c : splitByLength(content)) {
            Log.e(tag, c);
        }
    }
}
