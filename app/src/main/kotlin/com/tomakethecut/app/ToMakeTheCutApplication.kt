package com.tomakethecut.app

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

/** Hilt's root component lives here; every @Singleton binding is scoped to this object. */
@HiltAndroidApp
class ToMakeTheCutApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Any disk or network access on the main thread is logged (tag "StrictMode"), so
            // a regression of the threading model shows up in Logcat the moment it happens.
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
        }
    }
}
