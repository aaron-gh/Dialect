package com.dialect.launcher

import android.app.Application
import com.dialect.launcher.crashsafety.SafeModeExceptionHandler

class DialectApplication : Application() {
    var container: AppContainer? = null
        private set

    var isSafeMode = false
        private set

    override fun onCreate() {
        super.onCreate()
        SafeModeExceptionHandler.install(this)
        isSafeMode = SafeModeExceptionHandler.shouldEnterSafeMode(this)
        if (!isSafeMode) {
            container = AppContainer(this).also { it.appIndexRepository.start() }
        }
    }
}
