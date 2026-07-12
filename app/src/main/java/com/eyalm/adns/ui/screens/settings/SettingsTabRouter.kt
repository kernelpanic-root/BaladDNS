package com.eyalm.adns.ui.screens.settings


import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceRegistry
import com.eyalm.adns.data.nextdns.settings.NextDnsSettingRegistry
import com.eyalm.adns.viewmodel.SettingsViewModel
import com.eyalm.adns.data.provider.DnsProviderCatalog
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.data.AppRuntimeRepositories
import com.eyalm.adns.data.activation.ActivationMode
import com.eyalm.adns.data.activation.ActivationRepositories
import com.eyalm.adns.ui.screens.ActivationExitPolicy
import com.eyalm.adns.ui.screens.ActivationScreen
import com.eyalm.adns.viewmodel.OnboardingViewModel

@Composable
fun SettingsTabRouter(
    modifier: Modifier = Modifier,
    onNavigateToProvidersActivity: (String) -> Unit,
    innerPadding: PaddingValues

) {
    val viewModel: SettingsViewModel = viewModel()
    val page by viewModel.page.collectAsState()
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val profileSession by viewModel.profileSessionState.collectAsState()
    val context = LocalContext.current
    val activationRepository = remember(context) {
        ActivationRepositories.getInstance(context.applicationContext)
    }
    val activationState by activationRepository.state.collectAsState()
    val capabilities by remember(context) {
        AppRuntimeRepositories.capabilities(context.applicationContext)
    }.state.collectAsState()
    val permissionViewModel: OnboardingViewModel = viewModel()
    val permissionAcquisitionState by permissionViewModel.permissionState.collectAsState()

    LaunchedEffect(selectedProvider) {
        if (
            (selectedProvider as? DnsProviderSelection.Enhanced)?.providerId ==
            DnsProviderCatalog.NEXTDNS
        ) {
            viewModel.refreshProfileSession()
        }
    }


    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(300)
                        ) +
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            initialOffset = { it / 8 }
                        )) togetherWith
                        (fadeOut(animationSpec = tween(90)) +
                                scaleOut(
                                    targetScale = 1.08f,
                                    animationSpec = tween(300)
                                ))
            } else {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(
                            initialScale = 1.08f,
                            animationSpec = tween(300)
                        )) togetherWith
                        (fadeOut(animationSpec = tween(90)) +
                                scaleOut(
                                    targetScale = 0.92f,
                                    animationSpec = tween(300)
                                ) +
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = tween(
                                        300,
                                        easing = FastOutSlowInEasing
                                    ),
                                    targetOffset = { it / 8 }
                                ))
            }.using(
                SizeTransform(clip = false)
            )
        },
    ) { step ->
        when (step) {
            SettingsViewModel.Page.MAIN -> {
                MainSettingsScreen(
                    modifier = modifier,
                    onAddQuickTile = { viewModel.addQuickTile() },
                    currentPage = page,
                    onPageChange = viewModel::setPage,
                    innerPadding = innerPadding
                )
            }
            SettingsViewModel.Page.PROVIDERS -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                ProvidersScreen(
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                    onEnhancedModeClick = onNavigateToProvidersActivity
                )
            }
            SettingsViewModel.Page.ACTIVATION -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                ActivationScreen(
                    state = activationState,
                    permissionAcquisitionState = permissionAcquisitionState,
                    controlOnlyEligible = capabilities.canManageNextDns,
                    exitPolicy = ActivationExitPolicy.Allowed,
                    onStartPermissionMonitoring =
                        permissionViewModel::startPermissionCheck,
                    onRequestShizuku = permissionViewModel::requestShizukuActivation,
                    onExit = {
                        viewModel.setPage(SettingsViewModel.Page.MAIN)
                    },
                    onUseControlOnly = {
                        activationRepository.changeMode(
                            ActivationMode.NextDnsControlOnly
                        )
                    },
                    onUsePrivileged = {
                        activationRepository.changeMode(
                            ActivationMode.PrivilegedDnsControl
                        )
                    },
                    onStopPermissionAcquisition =
                        permissionViewModel::stopPermissionAcquisition,
                )
            }
            SettingsViewModel.Page.NOTIFICATIONS -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                NotificationSettingsScreen(
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                )
            }
            SettingsViewModel.Page.WIFI_RULES -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                WifiRulesScreen(
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                )
            }
            SettingsViewModel.Page.ACCOUNT_SETTINGS -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                AccountSettingsScreen(
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                    canControlPrivateDns = capabilities.canControlPrivateDns,
                )
            }
            SettingsViewModel.Page.SETUP -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                SetupScreen(
                    profileId = profileSession.selectedProfileId,
                    canManageLinkedIp = profileSession.capabilities.canEditSettings,
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                    onSelectProfile = {
                        viewModel.setPage(SettingsViewModel.Page.ACCOUNT_SETTINGS)
                    },
                )
            }
            SettingsViewModel.Page.SECURITY -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                GenericCategoryScreen(
                    title = stringResource(R.string.security),
                    settingsPage = NextDnsSettingRegistry.security,
                    profileState = profileSession,
                    lists = NextDnsResourceRegistry.security,
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                )
            }

            SettingsViewModel.Page.PRIVACY -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                GenericCategoryScreen(
                    title = stringResource(R.string.privacy),
                    settingsPage = NextDnsSettingRegistry.privacy,
                    profileState = profileSession,
                    lists = NextDnsResourceRegistry.privacy,
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                )
            }

            SettingsViewModel.Page.PARENTAL_CONTROL -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                GenericCategoryScreen(
                    title = stringResource(R.string.parental_control),
                    settingsPage = NextDnsSettingRegistry.parentalControl,
                    profileState = profileSession,
                    lists = NextDnsResourceRegistry.parentalControl,
                    extraContent = { profile, refreshRevision ->
                        profile.selected?.let { selected ->
                            RecreationSection(
                                profileId = selected.id,
                                canEdit = profile.capabilities.canEditSettings,
                                refreshRevision = refreshRevision,
                            )
                        }
                    },
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                )
            }

            SettingsViewModel.Page.SETTINGS_PAGE -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                GenericCategoryScreen(
                    title = stringResource(R.string.settings),
                    settingsPage = NextDnsSettingRegistry.settings,
                    profileState = profileSession,
                    extraContent = { profile, refreshRevision ->
                        profile.selected?.let { selected ->
                            RewritesSection(
                                profileId = selected.id,
                                canEdit = profile.capabilities.canEditSettings,
                                refreshRevision = refreshRevision,
                            )
                            if (profile.capabilities.canManageAccess) {
                                Spacer(Modifier.height(20.dp))
                                AccessSection(
                                    profileId = selected.id,
                                    refreshRevision = refreshRevision,
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            ProfileActionsSection(
                                profile = selected,
                                capabilities = profile.capabilities,
                                onProfileRemoved = { removedProfileId ->
                                    viewModel.onProfileRemoved(
                                        removedProfileId,
                                        onComplete = {
                                            viewModel.setPage(SettingsViewModel.Page.ACCOUNT_SETTINGS)
                                        },
                                    )
                                },
                                onProfileListChanged = viewModel::onProfileRenamed,
                                onOpenProfileSettings = {
                                    viewModel.setPage(SettingsViewModel.Page.ACCOUNT_SETTINGS)
                                },
                            )
                        }
                    },
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                    onLogsCleared = viewModel::invalidateLogs,
                )
            }
            SettingsViewModel.Page.GENERIC_LIST -> {
                // Back goes to the parent category, NOT to MAIN
                val parentPage = viewModel.getListParentPage()
                BackHandler { viewModel.setPage(parentPage) }
                GenericListScreen(
                    onBack = { viewModel.setPage(parentPage) },
                    profileState = profileSession,
                )
            }
            SettingsViewModel.Page.LOGS -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                LogsScreen(
                    onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) },
                    profileState = profileSession,
                )
            }
            SettingsViewModel.Page.LANGUAGE -> {
                BackHandler { viewModel.setPage(SettingsViewModel.Page.MAIN) }
                LanguageScreen(onBack = { viewModel.setPage(SettingsViewModel.Page.MAIN) })
            }
        }
    }
}
