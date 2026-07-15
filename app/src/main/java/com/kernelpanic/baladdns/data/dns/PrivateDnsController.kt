package com.kernelpanic.baladdns.data.dns

import com.kernelpanic.baladdns.data.PrivateDnsObservation
import com.kernelpanic.baladdns.data.provider.PrivateDnsHostname
import kotlinx.coroutines.CancellationException

interface PrivateDnsSettings {
    fun readMode(): String?

    fun readSpecifier(): String?

    fun writeMode(value: String): Boolean

    fun writeSpecifier(value: String): Boolean
}

enum class DnsDisableBehavior(val storageValue: String) {
    Automatic("automatic"),
    Off("off"),
}

sealed interface DnsWriteResult {
    data class Success(val observation: PrivateDnsObservation) : DnsWriteResult

    data object PermissionMissing : DnsWriteResult

    data object MissingHostname : DnsWriteResult

    data class Rejected(val observation: PrivateDnsObservation) : DnsWriteResult

    data class Failure(val error: Throwable) : DnsWriteResult
}

interface PrivateDnsControl {
    fun observe(): PrivateDnsObservation

    suspend fun enable(hostname: String?): DnsWriteResult

    suspend fun disable(behavior: DnsDisableBehavior): DnsWriteResult
}

class PrivateDnsController(
    private val settings: PrivateDnsSettings,
) : PrivateDnsControl {
    override fun observe(): PrivateDnsObservation = try {
        when (settings.readMode()) {
            MODE_HOSTNAME -> settings.readSpecifier()
                ?.let(PrivateDnsHostname::parsePreservingCase)
                ?.let { PrivateDnsObservation.Hostname(it.ascii) }
                ?: PrivateDnsObservation.Off

            MODE_AUTOMATIC -> PrivateDnsObservation.Automatic
            else -> PrivateDnsObservation.Off
        }
    } catch (_: SecurityException) {
        PrivateDnsObservation.PermissionMissing
    }

    override suspend fun enable(hostname: String?): DnsWriteResult {
        val normalized = PrivateDnsHostname.parsePreservingCase(hostname)?.ascii
            ?: return DnsWriteResult.MissingHostname
        return writeAndVerify(
            expected = PrivateDnsObservation.Hostname(normalized),
        ) {
            settings.writeSpecifier(normalized) && settings.writeMode(MODE_HOSTNAME)
        }
    }

    override suspend fun disable(behavior: DnsDisableBehavior): DnsWriteResult {
        val mode = when (behavior) {
            DnsDisableBehavior.Automatic -> MODE_AUTOMATIC
            DnsDisableBehavior.Off -> MODE_OFF
        }
        val expected = when (behavior) {
            DnsDisableBehavior.Automatic -> PrivateDnsObservation.Automatic
            DnsDisableBehavior.Off -> PrivateDnsObservation.Off
        }
        return writeAndVerify(expected) { settings.writeMode(mode) }
    }

    private inline fun writeAndVerify(
        expected: PrivateDnsObservation,
        write: () -> Boolean,
    ): DnsWriteResult = try {
        if (!write()) {
            DnsWriteResult.Rejected(observe())
        } else {
            val observed = observe()
            if (observed == expected) {
                DnsWriteResult.Success(observed)
            } else {
                DnsWriteResult.Rejected(observed)
            }
        }
    } catch (_: SecurityException) {
        DnsWriteResult.PermissionMissing
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        DnsWriteResult.Failure(error)
    }

    companion object {
        const val MODE_HOSTNAME = "hostname"
        const val MODE_OFF = "off"
        const val MODE_AUTOMATIC = "opportunistic"
    }
}
