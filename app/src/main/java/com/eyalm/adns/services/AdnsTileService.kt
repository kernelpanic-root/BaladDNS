package com.eyalm.adns.services

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.eyalm.adns.R
import com.eyalm.adns.data.AppRuntimeRepositories
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.activation.ActivationRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdnsTileService : TileService() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.eyalm.adns.data.localization.localizedContext(newBase))
    }

    private val repository by lazy { DnsRepository(this) }
    private val capabilities by lazy { AppRuntimeRepositories.capabilities(this).state }
    private val toggleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listeningJob: Job? = null
    private var lastKnownState: Boolean? = null

    override fun onStartListening() {
        super.onStartListening()
        ActivationRepositories.getInstance(this).refreshPermission()

        listeningJob?.cancel()
        listeningJob = CoroutineScope(Dispatchers.Main).launch {
            val initialState = withContext(Dispatchers.IO) {
                repository.isAdBlockingActive()
            }
            lastKnownState = initialState
            updateTile(
                isActive = initialState,
                available = capabilities.value.canUseDnsToggleSurfaces,
            )

            combine(repository.getDnsStatusFlow(), capabilities) { active, capability ->
                active to capability.canUseDnsToggleSurfaces
            }.collect { (active, available) ->
                lastKnownState = active
                updateTile(active, available)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
        listeningJob = null
    }

    override fun onClick() {
        super.onClick()
        ActivationRepositories.getInstance(this).refreshPermission()
        if (!AppRuntimeRepositories.capabilities(this).current().canUseDnsToggleSurfaces) {
            updateTile(isActive = false, available = false)
            return
        }
        toggleScope.launch {
            repository.toggle()
            val actualState = repository.isAdBlockingActive()
            withContext(Dispatchers.Main) {
                lastKnownState = resolveQuickTileState(lastKnownState, actualState)
                if (listeningJob?.isActive == true) {
                    updateTile(
                        isActive = actualState,
                        available = capabilities.value.canUseDnsToggleSurfaces,
                    )
                }
            }
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

internal fun resolveQuickTileState(
    currentState: Boolean?,
    actualState: Boolean?,
): Boolean? = actualState ?: currentState
