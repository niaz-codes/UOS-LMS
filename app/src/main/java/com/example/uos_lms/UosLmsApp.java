package com.example.uos_lms;

import android.app.Application;

import com.example.uos_lms.core.notifications.NotificationChannels;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class UosLmsApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannels.createAll(this);
    }
}
