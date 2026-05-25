package com.eyalm.adns

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.ui.screens.HomeScreen
import com.eyalm.adns.ui.screens.SettingsTabRouter
import com.eyalm.adns.ui.screens.StatsScreen
import com.eyalm.adns.ui.screens.UpdateDialog
import com.eyalm.adns.ui.theme.AdnsTheme
import com.eyalm.adns.viewmodel.MainViewModel
import com.eyalm.adns.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private fun handleShortcutIntent(intent: Intent?) {
        if (intent?.action == "com.eyalm.adns.TOGGLE_ACTION") {
            viewModel.toggleDns()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcutIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            settingsViewModel.refreshProvider()
            if (settingsViewModel.selectedProvider.value is DnsProvider.Enhanced) {
                settingsViewModel.email = settingsViewModel.getEmail()
                settingsViewModel.profiles = settingsViewModel.getProfiles()
                settingsViewModel.currentProfile = settingsViewModel.getCurrentProfile()
            }
            if (viewModel.dnsStats == null) {
                try {
                    viewModel.getStats()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error getting stats", e)

                }
            }
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val context = applicationContext
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge()
        handleShortcutIntent(intent)
        Locales.init(context)

        setContent {
            AdnsTheme {
                val isEnabled by viewModel.adBlockingState.collectAsState()
                val runningTime by viewModel.runningTimeFlow.collectAsState()
                val server by viewModel.dnsUrlFlow.collectAsState()
                val showDialog = remember { mutableStateOf(false) }
                val settingsPage by settingsViewModel.page.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        isEnabled = isEnabled,
                        runningTime = runningTime,
                        onToggle = { viewModel.toggleDns() },
                        modifier = Modifier.padding(innerPadding),
                        server = server,
                        onEditClick = {
                            settingsViewModel.setPage(SettingsViewModel.Page.PROVIDERS)
                        },
                        checkForUpdate = viewModel::checkForUpdate,
                        settingsPage = settingsPage

                    )
                }

                if (showDialog.value) {
                    DnsDialog(
                        onDismissRequest = { showDialog.value = false },
                        onConfirmation = {
                            viewModel.setDnsUrl(it)
                            showDialog.value = false
                        },
                        currentUrl = server
                    )
                }

            }
        }

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Greeting(
    isEnabled: Boolean,
    runningTime: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    server: String = "dns.adguard-dns.com",
    onEditClick: () -> Unit = {},
    checkForUpdate: ((String?) -> Unit) -> Unit = {},
    settingsPage: SettingsViewModel.Page = SettingsViewModel.Page.MAIN
) {

    var selectedItem by remember { mutableIntStateOf(0) }
    val items = remember { listOf("Home", "Stats", "Settings") }
    val selectedIcons = remember { listOf(Icons.Filled.Home, Icons.Filled.Insights, Icons.Filled.Settings) }
    val unselectedIcons = remember {
        listOf(Icons.Outlined.Home, Icons.Outlined.Insights, Icons.Outlined.Settings)
    }
    val context = LocalContext.current
    val onNavigateToProviders = remember(context) {
        { providerId: String ->
            val intent = Intent(context, ProviderLoginActivity::class.java).apply {
                putExtra("provider", providerId)
            }
            context.startActivity(intent)
        }
    }
    val latestVersion = remember { mutableStateOf<String?>(null) }

    val settingsViewModel: SettingsViewModel = viewModel()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            settingsViewModel.refreshNotification()
            Log.d("Permission", "Permission Granted")
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, "dns_status_channel")
            }
            context.startActivity(intent)
        } else {
            Log.d("Permission", "Permission Denied")
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    }

    if (!BuildConfig.IS_FOSS) {
        LaunchedEffect(Unit) {
            checkForUpdate { version ->
                Log.d("update", "Latest version: $version")
                latestVersion.value = version
            }
        }

        latestVersion.value?.let { version ->
            UpdateDialog(
                version = version,
                onClose = { latestVersion.value = null }
            )
        }
    }

    BackHandler(enabled = selectedItem != 0) {
        selectedItem = 0
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = !(settingsPage != SettingsViewModel.Page.MAIN && selectedItem == 2),
                enter = androidx.compose.animation.slideInVertically { it } + fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { it } + fadeOut()
            ) {
                NavigationBar {
                    items.forEachIndexed { index, item ->
                        if ((index == 1 && settingsViewModel.selectedProvider.collectAsState().value is DnsProvider.Enhanced) || index != 1)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                    contentDescription = item,
                                )
                            },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index },
                        )
                    }

                }
            }


        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedItem,
            transitionSpec = {
                ((fadeIn(animationSpec = tween(220, delayMillis = 80)) +
                        scaleIn(
                            initialScale = 0.93f,
                            animationSpec = tween(300, easing = LinearOutSlowInEasing)
                        )) togetherWith
                        (fadeOut(animationSpec = tween(120)) +
                                scaleOut(
                                    targetScale = 1.07f,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                )))
                    .using(SizeTransform(clip = false))
            },
            label = "MainContentTransition"
        ) { targetIndex ->
            when (targetIndex) {
                0 -> {
                    HomeScreen(
                        isEnabled = isEnabled,
                        runningTime = runningTime,
                        onToggle = onToggle,
                        server = server,
                        onEditClick = {
                            selectedItem = 2
                            onEditClick()
                        },
                        innerPadding = innerPadding,
                        onSettingsClick = {
                            selectedItem = 2
                        }
                    )

                }

                1 -> StatsScreen(
                    innerPadding
                )

                2 -> {
                    SettingsTabRouter(
                        modifier = Modifier.padding(innerPadding),
                        onNavigateToProvidersActivity = onNavigateToProviders,
                        permissionLauncher = permissionLauncher,
                        innerPadding = innerPadding
                    )
                }
            }
        }

    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AdnsTheme {
        Greeting(
            isEnabled = true,
            runningTime = "00:05:23",
            onToggle = {}
        )
    }
}