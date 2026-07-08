package com.eyalm.adns

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.LocaleHelper
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.data.nextdns.auth.NextDnsSessionManager
import com.eyalm.adns.ui.components.dialogs.BaseDialog
import com.eyalm.adns.ui.screens.HomeScreen
import com.eyalm.adns.ui.screens.settings.SettingsTabRouter
import com.eyalm.adns.ui.screens.StatsScreen
import com.eyalm.adns.ui.screens.UpdateDialog
import com.eyalm.adns.ui.theme.AdnsTheme
import com.eyalm.adns.viewmodel.MainViewModel
import com.eyalm.adns.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var lastAppliedLang: String? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

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
        val savedLang = LocaleHelper.getLanguage(this)
        if (lastAppliedLang != null && lastAppliedLang != savedLang) {
            recreate()
            return
        }
        lifecycleScope.launch {
            settingsViewModel.refreshProvider()
            if (settingsViewModel.selectedProvider.value is DnsProvider.Enhanced) {
                settingsViewModel.refreshProfileSession()
                settingsViewModel.email = settingsViewModel.getEmail()
            }
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyLocale(this)
        lastAppliedLang = LocaleHelper.getLanguage(this)
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

        lifecycleScope.launch {
            // migrate from 1.0.3
            val sharedPreferences = getSharedPreferences("adns_settings", MODE_PRIVATE)
            val oldHostname = sharedPreferences.getString("custom_url", null)
            oldHostname?.let {
                val provider = DnsProviders.getProviderByHostname(oldHostname)
                val repository = DnsRepository(context)
                sharedPreferences.edit { remove("custom_url") }
                if (provider is DnsProvider.Custom) {
                    repository.setProvider("custom", oldHostname)
                } else {
                    repository.setProvider(provider.id)
                }
            }
        }

        setContent {
            AdnsTheme {
                val isEnabled by viewModel.adBlockingState.collectAsState()
                val runningTime by viewModel.runningTimeFlow.collectAsState()
                val server by viewModel.dnsUrlFlow.collectAsState()
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
    val homeTab = stringResource(R.string.home)
    val statsTab = stringResource(R.string.stats)
    val settingsTab = stringResource(R.string.settings)
    val items = remember(homeTab, statsTab, settingsTab) { listOf(homeTab, statsTab, settingsTab) }
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
    val nextDnsSessionManager = remember(context) {
        NextDnsSessionManager.getInstance(context.applicationContext)
    }
    val reauthenticationRequested by nextDnsSessionManager.reauthenticationRequested.collectAsState()

    if (reauthenticationRequested) {
        BaseDialog(
            title = stringResource(R.string.nextdns_session_expired_title),
            body = stringResource(R.string.nextdns_session_expired_message),
            confirmLabel = stringResource(R.string.sign_in),
            destructive = false,
            onConfirm = {
                nextDnsSessionManager.dismissReauthenticationRequest()
                onNavigateToProviders(DnsProviders.NEXTDNS.id)
            },
            onDismiss = nextDnsSessionManager::dismissReauthenticationRequest,
        )
    }
    val latestVersion = remember { mutableStateOf<String?>(null) }

    val settingsViewModel: SettingsViewModel = viewModel()
    val selectedProvider by settingsViewModel.selectedProvider.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            settingsViewModel.setNotificationsEnabled(true)
            Log.d("Permission", "Permission Granted")
        } else {
            settingsViewModel.setNotificationsEnabled(false)
            Toast.makeText(
                context,
                R.string.notification_permission_denied,
                Toast.LENGTH_SHORT,
            ).show()
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
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                NavigationBar {
                    items.forEachIndexed { index, item ->
                        if ((index == 1 && selectedProvider is DnsProvider.Enhanced) || index != 1)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                    contentDescription = item,
                                )
                            },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = {
                                if (
                                    index != 1 ||
                                    nextDnsSessionManager.requestFeatureAccess()
                                ) {
                                    selectedItem = index
                                }
                            },
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
                ((fadeIn(animationSpec = tween(220)) +
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
