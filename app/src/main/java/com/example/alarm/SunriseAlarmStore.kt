package com.example.alarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Stores user alarms independently of the existing prayer-notification preferences. */
class SunriseAlarmStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun list(): List<SunriseAlarm> = synchronized(lock) { read() }

    fun find(id: String): SunriseAlarm? = synchronized(lock) { read().firstOrNull { it.id == id } }

    fun save(alarm: SunriseAlarm) = synchronized(lock) {
        val alarms = read().toMutableList()
        val index = alarms.indexOfFirst { it.id == alarm.id }
        val normalised = alarm.copy(label = alarm.label.trim().take(80).ifBlank { SunriseAlarm.DEFAULT_LABEL })
        if (index >= 0) alarms[index] = normalised else alarms += normalised
        write(alarms)
    }

    fun delete(id: String) = synchronized(lock) { write(read().filterNot { it.id == id }) }

    private fun read(): List<SunriseAlarm> {
        val array = runCatching { JSONArray(preferences.getString(ALARMS, "[]")) }.getOrNull() ?: return emptyList()
        val ids = mutableSetOf<String>()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val alarm = runCatching {
                    SunriseAlarm(
                        id = item.getString("id"),
                        minutesBeforeSunrise = item.getInt("minutesBeforeSunrise"),
                        enabled = item.optBoolean("enabled", true),
                        label = item.optString("label", SunriseAlarm.DEFAULT_LABEL).trim().take(80)
                            .let { if (it == SunriseAlarm.LEGACY_DEFAULT_LABEL) SunriseAlarm.DEFAULT_LABEL else it }
                            .ifBlank { SunriseAlarm.DEFAULT_LABEL }
                    )
                }.getOrNull() ?: continue
                if (ids.add(alarm.id)) add(alarm)
            }
        }
    }

    private fun write(alarms: List<SunriseAlarm>) {
        val array = JSONArray()
        alarms.forEach { alarm ->
            array.put(JSONObject().apply {
                put("id", alarm.id)
                put("minutesBeforeSunrise", alarm.minutesBeforeSunrise)
                put("enabled", alarm.enabled)
                put("label", alarm.label)
            })
        }
        // Persist before scheduling. A receiver after process death must see the same settings.
        check(preferences.edit().putString(ALARMS, array.toString()).commit()) { "Could not save sunrise alarms" }
    }

    companion object {
        const val PREFERENCES = "sunrise_alarms"
        internal const val ALARMS = "alarms_v1"
        private val lock = Any()
    }
}
