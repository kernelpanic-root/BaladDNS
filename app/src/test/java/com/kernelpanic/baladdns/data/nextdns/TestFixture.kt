package com.kernelpanic.baladdns.data.nextdns

internal fun nextDnsFixture(path: String): String =
    checkNotNull(object {}.javaClass.getResource("/nextdns/$path")) {
        "Missing NextDNS fixture: $path"
    }.readText()
