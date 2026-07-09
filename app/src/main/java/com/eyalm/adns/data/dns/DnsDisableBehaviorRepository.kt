package com.eyalm.adns.data.dns

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface DnsDisableBehaviorStore {
    fun read(): String?

    fun write(value: String)
}

class DnsDisableBehaviorRepository(
    private val store: DnsDisableBehaviorStore,
) {
    private val _behavior = MutableStateFlow(parse(store.read()))
    val behavior: StateFlow<DnsDisableBehavior> = _behavior.asStateFlow()

    fun set(value: DnsDisableBehavior) {
        store.write(value.storageValue)
        _behavior.value = value
    }

    private fun parse(value: String?): DnsDisableBehavior =
        DnsDisableBehavior.entries.firstOrNull { it.storageValue == value }
            ?: DnsDisableBehavior.Off
}
