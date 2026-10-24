package com.example.alarm

/** One immutable, uniquely identified alarm-clock occurrence or snooze. */
data class SunriseAlarmDelivery(
    val alarmId: String,
    val token: String,
    val triggerAtMillis: Long,
    val sunriseAtMillis: Long,
    val sourceDate: String,
    val fromSnooze: Boolean = false
)
