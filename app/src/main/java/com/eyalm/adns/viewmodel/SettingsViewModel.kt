package com.eyalm.adns.viewmodel

import android.app.Application
import android.app.StatusBarManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.R
import com.eyalm.adns.data.ApiRepository
import com.eyalm.adns.data.Blocklist
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.models.DnsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    enum class Page {
        MAIN,
        PROVIDERS,
        ACCOUNT_SETTINGS,
        BLOCKLISTS
    }

    private val repository = DnsRepository(application)
    private val apiRepository = ApiRepository(application)

    private val _dnsUrl = MutableStateFlow(repository.getDnsUrl())
    val dnsUrl: StateFlow<String?> = _dnsUrl.asStateFlow()

    private val _page = MutableStateFlow(Page.MAIN)
    val page = _page.asStateFlow()

    fun setDnsUrl(url: String) {
        repository.setCustomUrl(url)
        _dnsUrl.value = url
    }

    fun addQuickTile() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager =
                getApplication<Application>().getSystemService(StatusBarManager::class.java)
            statusBarManager?.requestAddTileService(
                ComponentName(
                    getApplication(),
                    com.eyalm.adns.services.AdnsTileService::class.java
                ),
                "ADNS AdBlock",
                android.graphics.drawable.Icon.createWithResource(
                    getApplication(),
                    R.drawable.ic_launcher_foreground
                ),
                getApplication<Application>().mainExecutor
            ) { result ->
                val message = when (result) {
                    1 -> "Tile already added!"
                    2 -> "Tile added!"
                    else -> ""
                }
                if (message.isNotEmpty())
                    Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                getApplication(),
                "Feature not supported on this version",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun refreshNotification() {
        repository.updateNotification()

    }


    private val _selectedProvider = MutableStateFlow(repository.getSelectedProvider())
    val selectedProvider = _selectedProvider.asStateFlow()

    fun setProvider(providerId: String, url: String? = null) {
        repository.setProvider(providerId, url)
        _selectedProvider.value = repository.getSelectedProvider()
    }

    fun isLoggedIn(provider: DnsProvider): Boolean {
        return apiRepository.isLoggedIn(provider)
    }

    suspend fun getEmail(): String {
        return apiRepository.getNextDnsEmail()
    }

    fun setPage(page: Page) {
        _page.value = page
    }

    var blocklists: List<Blocklist>? by mutableStateOf(null)


    fun getBlocklists() {
        viewModelScope.launch {
            val blocklistsResponse = apiRepository.getNextDnsBlocklists()
            blocklists = blocklistsResponse
        }
    }


    fun updateBlocklists(blocklistId: String) {
        viewModelScope.launch {
            apiRepository.updateNextDnsBlocklists(blocklistId)
        }
    }

    fun removeBlocklists(blocklistId: String) {
        viewModelScope.launch {
            apiRepository.removeNextDnsBlocklists(blocklistId)
        }
    }

}