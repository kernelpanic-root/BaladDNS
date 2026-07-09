package com.eyalm.adns.data.dns

import org.junit.Assert.assertEquals
import org.junit.Test

class DnsDisableBehaviorRepositoryTest {
    @Test
    fun `legacy installs default to off and persist explicit choice`() {
        val store = FakeDnsDisableBehaviorStore()
        val repository = DnsDisableBehaviorRepository(store)

        assertEquals(DnsDisableBehavior.Off, repository.behavior.value)

        repository.set(DnsDisableBehavior.Automatic)

        assertEquals(DnsDisableBehavior.Automatic, repository.behavior.value)
        assertEquals("automatic", store.value)
    }

    @Test
    fun `unknown stored value falls back to off`() {
        val repository = DnsDisableBehaviorRepository(
            FakeDnsDisableBehaviorStore("future-value")
        )

        assertEquals(DnsDisableBehavior.Off, repository.behavior.value)
    }

    private class FakeDnsDisableBehaviorStore(
        var value: String? = null,
    ) : DnsDisableBehaviorStore {
        override fun read(): String? = value

        override fun write(value: String) {
            this.value = value
        }
    }
}
