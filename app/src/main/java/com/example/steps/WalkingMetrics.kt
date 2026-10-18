package com.example.steps

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

data class WalkingProfile(val heightCm: Double, val weightKg: Double, val calibratedStepCm: Double? = null) {
    val valid: Boolean get() = heightCm.isFinite() && heightCm in 80.0..250.0 &&
        weightKg.isFinite() && weightKg in 20.0..350.0 &&
        (calibratedStepCm == null || calibratedStepCm.isFinite() && calibratedStepCm in 20.0..150.0)
    val stepMeters: Double get() = (calibratedStepCm ?: (heightCm * 0.415)) / 100.0
    val estimatedStepCm: Double get() = calibratedStepCm ?: (heightCm * 0.415)
}

data class WalkingEstimate(val meters: Double, val activeKcal: Double)

data class StepMilestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetText: String,
    val progress: Float,
    val achieved: Boolean
)

enum class WalkingPacePreset(val label: String, val cadence: Int, val metMultiplier: Double, val description: String) {
    GENTLE("Rahat Yürüyüş", 90, 0.85, "Günlük sakin adım temposu (~3.8 km/sa)"),
    MODERATE("Normal Tempo", 108, 1.0, "Standart sağlıklı yürüyüş (~4.8 km/sa)"),
    BRISK("Tempolu Yürüyüş", 125, 1.22, "Canlı kardiyo yürüyüşü (~5.8 km/sa)")
}

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

    fun activeMinutes(steps: Long, cadence: Int = 108): Int {
        if (steps <= 0L) return 0
        return ceil(steps.toDouble() / cadence.coerceIn(60, 180)).toInt()
    }

    fun averageSpeedKmh(meters: Double, activeMinutes: Int): Double {
        if (meters <= 0.0 || activeMinutes <= 0) return 0.0
        return (meters / 1000.0) / (activeMinutes / 60.0)
    }

    fun hydrationNeedMl(activeKcal: Double, activeMinutes: Int): Int {
        if (activeKcal <= 0.0 && activeMinutes <= 0) return 0
        return ((activeKcal * 1.15) + (activeMinutes * 3.5)).roundToInt().coerceAtLeast(0)
    }

    fun mosqueTripsEquivalent(steps: Long): Int {
        if (steps <= 0L) return 0
        // Average neighborhood walk to a mosque and back (~650 steps)
        return (steps / 650L).toInt()
    }

    fun bmi(profile: WalkingProfile?): Double? {
        if (profile == null || !profile.valid) return null
        val heightM = profile.heightCm / 100.0
        if (heightM <= 0.0) return null
        return profile.weightKg / (heightM * heightM)
    }

    fun bmiCategory(bmi: Double): String = when {
        bmi < 18.5 -> "Zayıf"
        bmi < 25.0 -> "İdeal Aralık"
        bmi < 30.0 -> "Fazla Kilolu"
        else -> "Obezite Sınırı"
    }

    fun hourlyDistribution(
        daySteps: Long,
        recordedHours: Map<Int, Long>,
        isToday: Boolean = false,
        currentHour: Int = 23
    ): List<Long> {
        if (daySteps <= 0L) return List(24) { 0L }
        val recordedSum = recordedHours.values.sum()
        if (recordedSum >= daySteps && recordedSum > 0L) {
            return List(24) { hour -> recordedHours[hour] ?: 0L }
        }
        val maxHour = if (isToday) currentHour.coerceIn(6, 23) else 22
        // Realistic prayer/daily routine weights across 24 hours
        val baseWeights = doubleArrayOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.35, // 00-05 (İmsak/Sabah)
            0.75, 1.35, 1.65, 1.15, 0.95, 1.10, // 06-11
            1.55, 1.75, 1.20, 1.45, 1.60, 1.90, // 12-17 (Öğle / İkindi)
            1.80, 1.40, 1.15, 0.85, 0.40, 0.10  // 18-23 (Akşam / Yatsı)
        )
        val activeWeights = DoubleArray(24) { h ->
            if (h > maxHour) 0.0 else baseWeights[h]
        }
        val weightSum = activeWeights.sum().coerceAtLeast(1.0)
        val remainder = (daySteps - recordedSum).coerceAtLeast(0L)
        val result = LongArray(24) { h ->
            val rec = recordedHours[h] ?: 0L
            val syn = (remainder * (activeWeights[h] / weightSum)).toLong()
            rec + syn
        }
        val diff = daySteps - result.sum()
        if (diff > 0) {
            val peakHour = (0..maxHour).maxByOrNull { activeWeights[it] } ?: 12
            result[peakHour] += diff
        }
        return result.toList()
    }

    fun buildMilestones(
        lifetimeSteps: Long,
        bestDaySteps: Long,
        currentStreak: Int,
        dailyGoal: Int
    ): List<StepMilestone> {
        val safeGoal = dailyGoal.coerceAtLeast(1000).toLong()
        return listOf(
            StepMilestone(
                id = "daily_goal",
                title = "Günlük Hedef Fatihi",
                subtitle = "En iyi günde hedefe ulaşma",
                targetText = "${bestDaySteps.coerceAtMost(safeGoal)} / $safeGoal adım",
                progress = (bestDaySteps.toFloat() / safeGoal).coerceIn(0f, 1f),
                achieved = bestDaySteps >= safeGoal
            ),
            StepMilestone(
                id = "streak_3",
                title = "İstikrar Serisi",
                subtitle = "Arka arkaya 3 aktif yürüyüş günü",
                targetText = "${currentStreak.coerceAtMost(3)} / 3 gün",
                progress = (currentStreak / 3f).coerceIn(0f, 1f),
                achieved = currentStreak >= 3
            ),
            StepMilestone(
                id = "steps_10k",
                title = "10.000 Adım Zirvesi",
                subtitle = "Bir günde 10.000 adım barajını aş",
                targetText = "${bestDaySteps.coerceAtMost(10_000L)} / 10.000",
                progress = (bestDaySteps / 10_000f).coerceIn(0f, 1f),
                achieved = bestDaySteps >= 10_000L
            ),
            StepMilestone(
                id = "lifetime_50k",
                title = "Bereketli Yolculuk",
                subtitle = "Toplamda 50.000 adıma ulaş",
                targetText = "${lifetimeSteps.coerceAtMost(50_000L)} / 50.000",
                progress = (lifetimeSteps / 50_000f).coerceIn(0f, 1f),
                achieved = lifetimeSteps >= 50_000L
            )
        )
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
