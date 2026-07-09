package com.eyalm.adns.domain

import com.eyalm.adns.data.activation.ActivationMode
import com.eyalm.adns.data.activation.ActivationState
import com.eyalm.adns.data.provider.DnsProviderCatalog
import com.eyalm.adns.data.provider.DnsProviderSelection

enum class MainTab {
    Home,
    Stats,
    Settings,
}

sealed interface AppDestination {
    data object Onboarding : AppDestination
    data object Activation : AppDestination
    data class Main(val tab: MainTab) : AppDestination
}

data class AppCapabilityInputs(
    val activation: ActivationState,
    val provider: DnsProviderSelection,
    val nextDnsSessionActive: Boolean,
    val nextDnsProfileSelected: Boolean,
)

data class AppCapabilities(
    val canControlPrivateDns: Boolean,
    val canUseDnsToggleSurfaces: Boolean,
    val canManageNextDns: Boolean,
    val showHome: Boolean,
    val showStats: Boolean,
    val showActivationWarning: Boolean,
    val canRunRuntimeMonitor: Boolean,
    val canUseWifiRules: Boolean,
    val defaultTab: MainTab,
    val visibleTabs: List<MainTab>,
    val startupDestination: AppDestination,
)

fun deriveAppCapabilities(inputs: AppCapabilityInputs): AppCapabilities {
    val activation = inputs.activation
    val isNextDns = (inputs.provider as? DnsProviderSelection.Enhanced)
        ?.providerId == DnsProviderCatalog.NEXTDNS
    val canManageNextDns = isNextDns &&
        inputs.nextDnsSessionActive &&
        inputs.nextDnsProfileSelected
    val incompatibleControlOnlyProvider =
        activation.mode == ActivationMode.NextDnsControlOnly && !isNextDns
    val showHome = activation.onboardingComplete &&
        activation.mode == ActivationMode.PrivilegedDnsControl
    val showStats = canManageNextDns
    val defaultTab = if (activation.mode == ActivationMode.NextDnsControlOnly) {
        MainTab.Settings
    } else {
        MainTab.Home
    }
    val visibleTabs = if (activation.mode == ActivationMode.NextDnsControlOnly) {
        buildList {
            add(MainTab.Settings)
            if (showStats) add(MainTab.Stats)
        }
    } else {
        buildList {
            if (showHome) add(MainTab.Home)
            if (showStats) add(MainTab.Stats)
            add(MainTab.Settings)
        }
    }
    val requiresActivation = activation.needsReactivation || incompatibleControlOnlyProvider
    val startupDestination = when {
        !activation.onboardingComplete -> AppDestination.Onboarding
        requiresActivation -> AppDestination.Activation
        else -> AppDestination.Main(
            defaultTab.takeIf(visibleTabs::contains) ?: MainTab.Settings
        )
    }

    return AppCapabilities(
        canControlPrivateDns = activation.canControlPrivateDns,
        canUseDnsToggleSurfaces = activation.canControlPrivateDns,
        canManageNextDns = canManageNextDns,
        showHome = showHome,
        showStats = showStats,
        showActivationWarning = requiresActivation,
        canRunRuntimeMonitor = activation.canControlPrivateDns,
        canUseWifiRules = activation.canControlPrivateDns,
        defaultTab = defaultTab,
        visibleTabs = visibleTabs,
        startupDestination = startupDestination,
    )
}

fun resolveAvailableMainTab(
    current: MainTab,
    capabilities: AppCapabilities,
): MainTab = when {
    current in capabilities.visibleTabs -> current
    capabilities.defaultTab in capabilities.visibleTabs -> capabilities.defaultTab
    else -> MainTab.Settings
}
