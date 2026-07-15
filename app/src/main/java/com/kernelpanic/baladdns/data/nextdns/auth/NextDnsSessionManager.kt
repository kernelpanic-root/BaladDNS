package com.kernelpanic.baladdns.data.nextdns.auth

import android.content.Context
import com.kernelpanic.baladdns.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

sealed interface NextDnsManagementSession {
    data object SignedOut : NextDnsManagementSession
    data object Active : NextDnsManagementSession
    data object Expired : NextDnsManagementSession
}

class NextDnsSessionManager private constructor(context: Context) {
    private val tokenManager = TokenManager.getInstance(context.applicationContext)
    private val invalidationGate = SessionInvalidationGate()

    private val _state = MutableStateFlow(
        if (tokenManager.hasToken()) {
            NextDnsManagementSession.Active
        } else {
            NextDnsManagementSession.SignedOut
        }
    )
    val state = _state.asStateFlow()

    private val _reauthenticationRequested = MutableStateFlow(false)
    val reauthenticationRequested = _reauthenticationRequested.asStateFlow()

    fun authenticated() {
        invalidationGate.reset()
        _state.value = NextDnsManagementSession.Active
        _reauthenticationRequested.value = false
    }

    fun unauthorized() {
        if (_state.value == NextDnsManagementSession.SignedOut) return
        if (!invalidationGate.invalidateOnce()) return

        tokenManager.destroyApiKey()
        tokenManager.destroyEmail()
        _state.value = NextDnsManagementSession.Expired
        _reauthenticationRequested.value = true
    }

    fun signedOut() {
        tokenManager.destroyApiKey()
        tokenManager.destroyEmail()
        invalidationGate.reset()
        _state.value = NextDnsManagementSession.SignedOut
        _reauthenticationRequested.value = false
    }

    fun requestFeatureAccess(): Boolean {
        if (_state.value == NextDnsManagementSession.Active) {
            return true
        }
        _reauthenticationRequested.value = true
        return false
    }

    fun dismissReauthenticationRequest() {
        _reauthenticationRequested.value = false
    }

    companion object {
        @Volatile
        private var instance: NextDnsSessionManager? = null

        fun getInstance(context: Context): NextDnsSessionManager =
            instance ?: synchronized(this) {
                instance ?: NextDnsSessionManager(context.applicationContext).also {
                    instance = it
                }
            }
    }
}

internal class SessionInvalidationGate {
    private val invalidated = AtomicBoolean(false)

    fun invalidateOnce(): Boolean = invalidated.compareAndSet(false, true)

    fun reset() {
        invalidated.set(false)
    }
}
