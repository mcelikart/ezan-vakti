package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Ensures that the device screen stays on and does not lock or turn off while this Composable is in composition.
 * Used for reading screens such as the Quran reading module.
 */
@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    val currentView = LocalView.current

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window

        // WindowManager flag on Activity window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // View-level flag on Compose root view
        val previousKeepScreenOn = currentView.keepScreenOn
        currentView.keepScreenOn = true

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            currentView.keepScreenOn = previousKeepScreenOn
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
