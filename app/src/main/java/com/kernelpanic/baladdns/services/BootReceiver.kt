package com.kernelpanic.baladdns.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kernelpanic.baladdns.data.runtime.RuntimeServiceController

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            RuntimeServiceController.syncFromBoot(context)
        }
    }
}
