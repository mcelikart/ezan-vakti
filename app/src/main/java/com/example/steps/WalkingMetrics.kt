package com.example.steps

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class WalkingProfile(val heightCm: Double, val weightKg: Double, val calibratedStepCm: Double? = null) {
    val valid: Boolean get() = heightCm.isFinite() && heightCm in 80.0..250.0 &&
        weightKg.isFinite() && weightKg in 20.0..350.0 &&
        (calibratedStepCm == null || calibratedStepCm.isFinite() && calibratedStepCm in 20.0..150.0)
    val stepMeters: Double get() = (calibratedStepCm ?: (heightCm * 0.415)) / 100.0
}

data class WalkingEstimate(val meters: Double, val activeKcal: Double)

object WalkingMetrics {
    fun estimate(steps: Long, profile: WalkingProfile?): WalkingEstimate? {
        if (steps < 0 || profile == null || !profile.valid) return null
        val distance = steps.toDouble() * profile.stepMeters
        // ACSM level-walking horizontal component: 0.1 ml O2/kg/m, 5 kcal/L.
        // This is net activity energy, excluding resting energy; slope and running are not modelled.
        return WalkingEstimate(distance, distance * profile.weightKg * 0.0005)
    }

    fun calibratedStepCm(distanceMeters: Double, countedSteps: Int): Double? {
        if (!distanceMeters.isFinite() || distanceMeters <= 0 || countedSteps < 20) return null
        return (distanceMeters * 100 / countedSteps).takeIf { it in 20.0..150.0 }
    }
}

enum class ReportPeriod(val title: String) { DAY("Günlük"), WEEK("Haftalık"), MONTH("Aylık") }
data class ReportRange(val start: String, val end: String, val days: List<String>)

object WalkingDates {
    fun calendar(day: String): Calendar = Calendar.getInstance().apply {
        time = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(day)!!
    }
    fun shift(day: String, period: ReportPeriod, amount: Int): String = calendar(day).apply {
        add(when (period) {
            ReportPeriod.DAY -> Calendar.DAY_OF_MONTH
            ReportPeriod.WEEK -> Calendar.WEEK_OF_YEAR
            ReportPeriod.MONTH -> Calendar.MONTH
        }, amount)
    }.let { StepStore.dayKey(it.timeInMillis) }

    fun range(day: String, period: ReportPeriod): ReportRange {
        val start = calendar(day)
        when (period) {
            ReportPeriod.WEEK -> start.add(Calendar.DAY_OF_MONTH, -((start.get(Calendar.DAY_OF_WEEK) + 5) % 7))
            ReportPeriod.MONTH -> start.set(Calendar.DAY_OF_MONTH, 1)
            else -> Unit
        }
        val length = when (period) {
            ReportPeriod.DAY -> 1
            ReportPeriod.WEEK -> 7
            ReportPeriod.MONTH -> start.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        val days = List(length) { offset ->
            (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
                .let { StepStore.dayKey(it.timeInMillis) }
        }
        return ReportRange(days.first(), days.last(), days)
    }
}
