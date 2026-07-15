package com.kernelpanic.baladdns.data.nextdns.analytics

enum class AnalyticsPeriod(
    val wireValue: String,
    val localeKey: String,
) {
    Minutes30("-30m", "30m"),
    Hours6("-6h", "6h"),
    Hours24("-24h", "24h"),
    Days7("-7d", "7d"),
    Days30("-30d", "30d"),
    Months3("-3M", "3M"),
}

data class AnalyticsScope(
    val period: AnalyticsPeriod = AnalyticsPeriod.Days30,
    val deviceId: String? = null,
)
