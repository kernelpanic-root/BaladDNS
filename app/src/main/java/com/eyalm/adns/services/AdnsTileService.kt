package com.eyalm.adns.services
import com.eyalm.adns.R


import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.eyalm.adns.data.AppRuntimeRepositories
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.activation.ActivationRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AdnsTileService : TileService() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.eyalm.adns.data.LocaleHelper.onAttach(newBase))
    }

    private val repository by lazy { DnsRepository(this) }
    private val capabilities by lazy { AppRuntimeRepositories.capabilities(this).state }
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        ActivationRepositories.getInstance(this).refreshPermission()

        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            combine(repository.getDnsStatusFlow(), capabilities) { active, capability ->
                active to capability.canUseDnsToggleSurfaces
            }.collect { (active, available) ->
                updateTile(active, available)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        job?.cancel()
        job = null
    }

    override fun onClick() {
        super.onClick()
        ActivationRepositories.getInstance(this).refreshPermission()
        if (!AppRuntimeRepositories.capabilities(this).current().canUseDnsToggleSurfaces) {
            updateTile(isActive = false, available = false)
            return
        }
        job = CoroutineScope(Dispatchers.Main).launch {
            repository.toggle()
        }
    }

    private fun updateTile(isActive: Boolean, available: Boolean) {
        val tile = qsTile ?: return
        tile.state = when {
            !available -> Tile.STATE_UNAVAILABLE
            isActive -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(
            if (available) R.string.adns_adblock else R.string.activation_required
        )
        tile.updateTile()
    }
}
