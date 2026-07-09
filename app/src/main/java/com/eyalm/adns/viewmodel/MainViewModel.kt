package com.eyalm.adns.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.BuildConfig
import com.eyalm.adns.data.AppRuntimeRepositories
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.activation.ActivationRepositories
import com.eyalm.adns.data.dns.DnsConfigurationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.Locale


class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DnsRepository(application)
    private val sharedPrefs = application.getSharedPreferences("adns_settings", Context.MODE_PRIVATE)
    private val activationRepository = ActivationRepositories.getInstance(application)
    val capabilities = AppRuntimeRepositories.capabilities(application).state
    private val _lastDnsResult = MutableStateFlow<DnsConfigurationResult?>(null)
    val lastDnsResult = _lastDnsResult.asStateFlow()

    val dnsUrlFlow: StateFlow<String> = repository.getDnsUrlFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.getDnsUrl() ?: ""
        )

    val adBlockingState: StateFlow<Boolean> = repository.getDnsStatusFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.isAdBlockingActive()
        )

    fun toggleDns() {
        activationRepository.refreshPermission()
        if (!activationRepository.state.value.canControlPrivateDns) {
            _lastDnsResult.value = DnsConfigurationResult.PermissionMissing
            return
        }
        viewModelScope.launch {
            _lastDnsResult.value = repository.toggle()
        }
    }

    val runningTimeFlow: StateFlow<String> = flow {
        while (true) {
            val start = repository.getStartTime()
            if (start > 0 && repository.isAdBlockingActive()) {
                val duration = System.currentTimeMillis() - start
                emit(formatDuration(duration))
            } else {
                emit("00:00:00")
            }
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00:00")

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun checkForUpdate(onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.github.com/repos/eyalm2000/adns/releases")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                val releases = JSONArray(body)

                val latest = releases.getJSONObject(0)
                var latestVersion = latest.getString("tag_name")

                if (latestVersion[0] == 'v') { latestVersion = latestVersion.substring(1) }

                if (BuildConfig.VERSION_NAME == latestVersion) {
                    Log.d("update", "No new update, version from github: $latestVersion")
                    Log.d("update", "is new update dismissed: false}")
                    sharedPrefs.edit { putBoolean("isNewUpdateDismissed", false) }
                    withContext(Dispatchers.Main) { onResult(null) }
                } else {

                    if (sharedPrefs.getBoolean("isNewUpdateDismissed", false) ) {
                        Log.d("update", "isNewUpdateDismissed: true")
                        withContext(Dispatchers.Main) { onResult(null) }
                    } else {
                        sharedPrefs.edit { putBoolean("isNewUpdateDismissed", true) }
                        withContext(Dispatchers.Main) { onResult(latestVersion) }
                    }
                }
            } catch (e: java.io.IOException) {
                Log.e("MainViewModel", "Network error checking update", e)
                withContext(Dispatchers.Main) { onResult(null) }
            } catch (e: org.json.JSONException) {
                Log.e("MainViewModel", "JSON parse error checking update", e)
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }
}
