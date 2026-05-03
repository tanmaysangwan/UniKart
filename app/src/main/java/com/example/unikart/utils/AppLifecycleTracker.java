package com.example.unikart.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Tracks whether the app is in the foreground or background.
 * Register this in Application.onCreate() via registerActivityLifecycleCallbacks().
 */
public class AppLifecycleTracker implements Application.ActivityLifecycleCallbacks {

    private static int activeActivityCount = 0;
    private static Activity currentActivity = null;

    public static boolean isAppInForeground() {
        return activeActivityCount > 0;
    }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        activeActivityCount++;
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // Keep current activity reference
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        activeActivityCount--;
        if (activeActivityCount == 0) {
            currentActivity = null;
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // Not needed
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
