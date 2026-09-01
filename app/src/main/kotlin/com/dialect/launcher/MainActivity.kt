package com.dialect.launcher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.dialect.launcher.crashsafety.SafeModeExceptionHandler
import com.dialect.launcher.crashsafety.SafeModeScreen
import com.dialect.launcher.home.HomeScreen
import com.dialect.launcher.home.HomeViewModel
import com.dialect.launcher.settings.SettingsScreen
import com.dialect.launcher.ui.theme.DialectTheme

private const val STABLE_FOREGROUND_RESET_DELAY_MILLIS = 5_000L

class MainActivity : ComponentActivity() {
    private var viewModel: HomeViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as DialectApplication

        if (app.isSafeMode) {
            setContent {
                DialectTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        SafeModeScreen()
                    }
                }
            }
            return
        }

        val container = requireNotNull(app.container)
        val vm = ViewModelProvider(this, HomeViewModel.Factory(container))[HomeViewModel::class.java]
        viewModel = vm

        setContent {
            DialectTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var showSettings by remember { mutableStateOf(false) }
                    if (showSettings) {
                        SettingsScreen(
                            settingsRepository = container.settingsRepository,
                            onBack = { showSettings = false },
                        )
                    } else {
                        HomeScreen(vm, onOpenSettings = { showSettings = true })
                    }
                }
            }
        }

        // NFR-4: only clear the crash counter once we've run stably for a while, not immediately on launch.
        Handler(Looper.getMainLooper()).postDelayed(
            { SafeModeExceptionHandler.resetAfterStableForeground(this) },
            STABLE_FOREGROUND_RESET_DELAY_MILLIS,
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // T-4/FR-10: repeated Home presses (re-entering the launcher after being backgrounded) reset the buffer.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel?.onClearBuffer()
        }
    }
}
