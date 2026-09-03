package com.example.uos_lms.core.common;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * App-wide thread pools, used for work that doesn't already run on a Firebase/Play Services
 * callback thread (e.g. Cloudinary's OkHttp calls, or chaining several Task-based calls).
 * Standard Google Architecture Components sample utility.
 */
public final class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService diskIO;
    private final Executor mainThread;

    private AppExecutors() {
        diskIO = Executors.newFixedThreadPool(4);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainThread = mainHandler::post;
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public ExecutorService diskIO() {
        return diskIO;
    }

    public Executor mainThread() {
        return mainThread;
    }
}
