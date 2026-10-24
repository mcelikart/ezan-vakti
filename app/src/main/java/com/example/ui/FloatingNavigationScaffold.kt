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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNavBarHeight

/** Shared full-height scrolling viewport with the measured navigation island floating above it. */
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
    val navigationVisible = showBottomBar && !keyboardVisible
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarHeight = with(LocalDensity.current) { bottomBarHeightPx.toDp() }
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = contentWindowInsets,
        bottomBar = {
            if (navigationVisible) {
                Box(
                    Modifier.fillMaxWidth()
                        .onSizeChanged { bottomBarHeightPx = it.height }
                        .testTag("app-bottom-navigation")
                ) { bottomBar() }
            }
        }
    ) { innerPadding ->
        // Let content pass behind the floating island. Scrollable screens add
        // navBarBottomPadding() to their trailing content so their final item
        // can stop at the island's upper edge.
        val visibleBottomBarHeight = if (navigationVisible) bottomBarHeight else 0.dp
        val contentBottomPadding = (innerPadding.calculateBottomPadding() - visibleBottomBarHeight)
            .coerceAtLeast(0.dp)
        Box(
            Modifier.fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), bottom = contentBottomPadding)
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(imeInsets)
                .clipToBounds()
                .testTag("app-content-viewport")
        ) {
            CompositionLocalProvider(LocalNavBarHeight provides visibleBottomBarHeight) { content() }
        }
    }
}
