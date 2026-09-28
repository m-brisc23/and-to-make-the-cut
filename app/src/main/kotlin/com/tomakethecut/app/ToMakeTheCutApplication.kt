package com.tomakethecut.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt's root component lives here; every @Singleton binding is scoped to this object. */
@HiltAndroidApp
class ToMakeTheCutApplication : Application()
