package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNavBarHeight

/** One measured, unobscured viewport shared by every navigation destination. */
@Composable
internal fun FloatingNavigationScaffold(
    modifier: Modifier = Modifier,
    showBottomBar: Boolean = true,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    imeInsets: WindowInsets = WindowInsets.ime,
    bottomBar: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val keyboardVisible = imeInsets.getBottom(LocalDensity.current) > 0
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = contentWindowInsets,
        bottomBar = {
            if (showBottomBar && !keyboardVisible) {
                Box(Modifier.fillMaxWidth().testTag("app-bottom-navigation")) { bottomBar() }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(imeInsets)
                .clipToBounds()
                .testTag("app-content-viewport")
        ) {
            // Legacy screen helpers now add only their own desired spacing.
            // The measured island and system insets have already been reserved.
            CompositionLocalProvider(LocalNavBarHeight provides 0.dp) { content() }
        }
    }
}
