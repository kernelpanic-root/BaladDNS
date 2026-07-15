package com.kernelpanic.baladdns.ui.components.refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdnsPullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            val indicatorSize = 48.dp
            val targetRestingPosition = statusBarHeight + 80.dp + 32.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY =
                            (targetRestingPosition.toPx() + indicatorSize.toPx()) * state.distanceFraction - indicatorSize.toPx()
                    }
                    .size(indicatorSize)
            ) {
                if (refreshing) {
                    CircularWavyProgressIndicator(modifier = Modifier.fillMaxSize())
                } else {
                    ContainedLoadingIndicator(
                        progress = { state.distanceFraction },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
        content = content,
    )
}