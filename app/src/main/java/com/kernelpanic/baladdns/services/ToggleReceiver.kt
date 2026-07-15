package com.kernelpanic.baladdns.services

import android.content.BroadcastReceiver
import android.content.Intent
import com.kernelpanic.baladdns.MainActivity
import com.kernelpanic.baladdns.data.AppRuntimeRepositories
import com.kernelpanic.baladdns.data.DnsRepository
import com.kernelpanic.baladdns.data.activation.ActivationRepositories
import com.kernelpanic.baladdns.data.dns.DnsConfigurationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
        if (intent?.action == ACTION_TOGGLE_DNS) {
            val safeContext = context ?: return
            ActivationRepositories.getInstance(safeContext).refreshPermission()
            if (!AppRuntimeRepositories.capabilities(safeContext).current().canUseDnsToggleSurfaces) {
                openActivation(safeContext)
                return
            }

            val pendingResult = goAsync()
            val repository = DnsRepository(safeContext)

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    if (repository.toggle() == DnsConfigurationResult.PermissionMissing) {
                        openActivation(safeContext)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_DNS = "com.kernelpanic.baladdns.action.TOGGLE_DNS"

        private fun openActivation(context: android.content.Context) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
        }
    }
}
