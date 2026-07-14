package com.eyalm.adns.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyalm.adns.data.Locales
import com.eyalm.adns.ui.components.refresh.AdnsPullToRefresh
import com.eyalm.adns.ui.theme.pageTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenScaffold(
    onBack: (() -> Unit)?,
    title: String,
    modifier: Modifier = Modifier,
    description: String = "",
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    val scrollState = rememberLazyListState()
    val showAppBarTitle by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    SettingsScreenLayout(
        title = title,
        onBack = onBack,
        showAppBarTitle = showAppBarTitle,
        modifier = modifier,
        refreshing = refreshing,
        onRefresh = onRefresh,
        snackbarHostState = snackbarHostState,
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
                item {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.pageTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (description.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            content()
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenLayout(
    title: String,
    onBack: (() -> Unit)?,
    showAppBarTitle: Boolean,
    modifier: Modifier = Modifier,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scaffoldContent: @Composable (Modifier) -> Unit = { scaffoldModifier ->
        Scaffold(
            modifier = scaffoldModifier,
            snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
            floatingActionButton = { floatingActionButton?.invoke() },
            topBar = {
                TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showAppBarTitle,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                navigationIcon = {
                    onBack?.let { navigateBack ->
                        IconButton(onClick = navigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                Locales.getString("global", "back"),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding -> content(innerPadding) }
    }

    if (onRefresh != null) {
        AdnsPullToRefresh(
            refreshing = refreshing,
            onRefresh = onRefresh,
            modifier = modifier,
        ) {
            scaffoldContent(Modifier.fillMaxSize())
        }
    } else {
        scaffoldContent(modifier)
    }
}
