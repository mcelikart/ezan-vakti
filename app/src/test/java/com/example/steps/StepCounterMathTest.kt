package com.example.steps

import org.junit.Assert.*
import org.junit.Test

class StepCounterMathTest {
    private fun sample(total: Long, seconds: Long, boot: Int = 1) = CounterSample(total, boot, seconds * 1_000_000_000)
    @Test fun firstSampleDoesNotImportEarlierSteps() {
        assertEquals(0L, StepCounterMath.delta(null, sample(9000, 10)))
    }
    @Test fun restartUsesStoredCheckpoint() {
        assertEquals(18L, StepCounterMath.delta(sample(100, 10), sample(118, 30)))
    }
    @Test fun duplicateAndOutOfOrderSamplesAreIgnored() {
        assertEquals(0L, StepCounterMath.delta(sample(100, 10), sample(100, 10)))
        assertNull(StepCounterMath.delta(sample(100, 10), sample(95, 9)))
    }
    @Test fun rebootRecognizedEvenIfNewTotalExceedsOldTotal() {
        assertEquals(150L, StepCounterMath.delta(sample(100, 1000), sample(150, 100, 2)))
    }
    @Test fun isolatedLowGlitchDoesNotInflateNextReading() {
        val previous = sample(1000, 100)
        val low = StepCounterMath.evaluate(previous, sample(10, 101))
        assertEquals(previous, low.checkpoint)
        assertEquals(0L, low.steps)
        val recovered = StepCounterMath.evaluate(low.checkpoint, sample(1002, 102), low.resetCandidate)
        assertEquals(2L, recovered.steps)
    }
    @Test fun confirmedSensorResetResumesFromLowCheckpoint() {
        val low = StepCounterMath.evaluate(sample(1000, 100), sample(0, 101))
        val confirmed = StepCounterMath.evaluate(low.checkpoint, sample(2, 102), low.resetCandidate)
        assertEquals(2L, confirmed.steps)
        assertEquals(2L, confirmed.checkpoint!!.total)
    }
    @Test fun repeatedZeroGlitchesDoNotConfirmReset() {
        val low = StepCounterMath.evaluate(sample(1000, 100), sample(0, 101))
        val repeated = StepCounterMath.evaluate(low.checkpoint, sample(0, 102), low.resetCandidate)
        val recovered = StepCounterMath.evaluate(repeated.checkpoint, sample(1002, 103), repeated.resetCandidate)
        assertEquals(2L, recovered.steps)
    }
    @Test fun outOfOrderResetCandidateCannotReplaceNewerReading() {
        val low = StepCounterMath.evaluate(sample(1000, 100), sample(0, 105))
        val stale = StepCounterMath.evaluate(low.checkpoint, sample(1, 103), low.resetCandidate)
        assertEquals(low, stale)
    }
    @Test fun impossibleJumpDoesNotPoisonBaseline() {
        val original = sample(100, 100)
        val invalid = StepCounterMath.evaluate(original, sample(5000, 101))
        assertEquals(original, invalid.checkpoint)
        assertEquals(2L, StepCounterMath.evaluate(invalid.checkpoint, sample(102, 102)).steps)
    }
    @Test fun delayedBatchesUseEventTimeRatherThanArrivalRate() {
        assertEquals(1000L, StepCounterMath.delta(sample(100, 10), sample(1100, 610)))
    }
    @Test fun negativeValuesCannotCreateOrModifyCheckpoint() {
        assertNull(StepCounterMath.evaluate(null, sample(-1, 10)).checkpoint)
        assertNull(StepCounterMath.evaluate(null, sample(1, -1)).checkpoint)
    }
}
