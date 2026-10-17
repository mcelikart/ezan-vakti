package com.example.steps

data class CounterSample(val total: Long, val boot: Int, val timestamp: Long)

data class CounterDecision(
    val steps: Long,
    val checkpoint: CounterSample?,
    val resetCandidate: CounterSample? = null
)

object StepCounterMath {
    private const val MAX_STEPS_PER_SECOND = 6.0

    private fun plausible(steps: Long, elapsed: Long): Boolean =
        steps <= elapsed.coerceAtLeast(0L) / 1_000_000_000.0 * MAX_STEPS_PER_SECOND + 2

    fun evaluate(previous: CounterSample?, current: CounterSample, resetCandidate: CounterSample? = null): CounterDecision {
        val unchanged = CounterDecision(0, previous, resetCandidate)
        if (current.total < 0 || current.timestamp < 0) return unchanged
        if (previous == null) return CounterDecision(0, current)
        if (previous.boot != current.boot) {
            return CounterDecision(if (plausible(current.total, current.timestamp)) current.total else 0, current)
        }
        if (current.timestamp <= maxOf(previous.timestamp, resetCandidate?.timestamp ?: 0)) return unchanged
        if (current.total < previous.total) {
            // Confirm a reset with two increasing low readings; a single bad low value must not inflate the next delta.
            if (resetCandidate != null && current.timestamp > resetCandidate.timestamp &&
                current.total > resetCandidate.total &&
                plausible(current.total - resetCandidate.total, current.timestamp - resetCandidate.timestamp)) {
                return CounterDecision(current.total - resetCandidate.total, current)
            }
            return CounterDecision(0, previous, current)
        }
        val change = current.total - previous.total
        if (!plausible(change, current.timestamp - previous.timestamp)) return unchanged
        return CounterDecision(change, current)
    }

    fun delta(previous: CounterSample?, current: CounterSample): Long? {
        val decision = evaluate(previous, current)
        return if (decision.checkpoint == current) decision.steps else null
    }
}
