package com.example.alarm

import java.util.UUID

/** A daily alarm whose local clock time follows the dated sunrise, not a fixed hour. */
data class SunriseAlarm(
    val id: String = UUID.randomUUID().toString(),
    val minutesBeforeSunrise: Int = DEFAULT_MINUTES_BEFORE_SUNRISE,
    val enabled: Boolean = true,
    val label: String = DEFAULT_LABEL
) {
    init {
        require(id.isNotBlank() && id.length <= 100) { "Alarm identity is invalid" }
        require(minutesBeforeSunrise in 0..MAX_MINUTES_BEFORE_SUNRISE) { "Sunrise offset is out of range" }
    }

    companion object {
        const val DEFAULT_MINUTES_BEFORE_SUNRISE = 30
        const val MAX_MINUTES_BEFORE_SUNRISE = 180
        const val LEGACY_DEFAULT_LABEL = "Güneş alarmı"
        const val DEFAULT_LABEL = "Sabah namazı alarmı"
    }
}

data class SunriseDay(val date: String, val sunriseAtMillis: Long)

data class SunriseAlarmOccurrence(
    val alarmId: String,
    val sunriseAtMillis: Long,
    val triggerAtMillis: Long,
    val sourceDate: String
)
