package com.kernelpanic.baladdns.data.activation

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivationSnapshotCodecTest {
    @Test
    fun `activation codec round trips persisted intent independently of permission`() {
        val snapshot = StoredActivationSnapshot(
            migrationVersion = 1,
            onboardingComplete = true,
            mode = ActivationMode.NextDnsControlOnly.storageValue,
            needsModeChoice = false,
        )

        assertEquals(
            snapshot,
            ActivationSnapshotCodec.decode(ActivationSnapshotCodec.encode(snapshot)),
        )
    }
}
