package com.alphito.kcal.feature.steps

import com.alphito.kcal.feature.steps.domain.StepCounterTracker
import org.junit.Assert.assertEquals
import org.junit.Test

class StepCounterTrackerTest {

    /** Feeds sensor events and keeps the running total that would be in Room. */
    private class Walk(restored: StepCounterTracker.Restored? = null) {
        val tracker = StepCounterTracker(restored)
        var saved = 0
        fun detect(steps: Int = 1) = repeat(steps) { saved += tracker.onDetector(1) }
        fun counter(value: Float) { saved += tracker.onCounter(value) }
    }

    @Test fun `first step is saved immediately`() {
        val w = Walk()
        w.counter(12_000f) // registration reading: the baseline, nothing walked yet
        w.detect()
        assertEquals(1, w.saved)
    }

    @Test fun `every detected step increases the count`() {
        val w = Walk()
        w.counter(12_000f)
        for (i in 1..10) {
            w.detect()
            assertEquals(i, w.saved)
        }
    }

    @Test fun `steps the counter ignores never go back to zero`() {
        // Seen on a Moto g35 (Sprd): 10 detector events in short bursts, no counter event at all.
        val w = Walk()
        w.counter(48_210f)
        w.detect(2); w.detect(4); w.detect(1); w.detect(3)
        assertEquals(10, w.saved)
        w.counter(48_210f) // counter still unchanged
        assertEquals(10, w.saved)
    }

    @Test fun `counter confirming detector steps adds nothing twice`() {
        val w = Walk()
        w.counter(12_000f)
        w.detect(10)
        w.counter(12_010f)
        assertEquals(10, w.saved)
    }

    @Test fun `steps before the counter's first report are kept`() {
        // Many counters only report once walking is confirmed. The old code used that first reading
        // as the baseline and lost every step up to it (the "starts after ~20 steps" bug).
        val w = Walk()
        w.detect(20)
        w.counter(54_321f)
        assertEquals(20, w.saved)
    }

    @Test fun `counter ahead of the detector catches the total up`() {
        val w = Walk()
        w.counter(1_000f)
        w.detect(8)
        w.counter(1_012f) // warm-up steps the detector missed
        assertEquals(12, w.saved)
        w.detect(4) // detector reaches 12 too: nothing new
        assertEquals(12, w.saved)
        w.detect(1)
        assertEquals(13, w.saved)
    }

    @Test fun `a 100 step walk with late batched counter reports counts 100`() {
        val w = Walk()
        w.counter(500f)
        var counter = 500f
        repeat(10) {
            w.detect(10)
            counter += 10
            w.counter(counter)
        }
        assertEquals(100, w.saved)
    }

    @Test fun `the total never decreases`() {
        val w = Walk()
        w.counter(1_000f)
        w.detect(15)
        w.counter(1_005f)
        assertEquals(15, w.saved)
        w.counter(1_003f) // a glitchy lower reading
        assertEquals(15, w.saved)
    }

    @Test fun `a repeated counter value adds nothing`() {
        val w = Walk()
        w.counter(3_000f)
        w.counter(3_005f)
        w.counter(3_005f)
        assertEquals(5, w.saved)
    }

    @Test fun `steps walked while the service was restarting are recovered`() {
        val w = Walk(StepCounterTracker.Restored(counterValue = 2_000f, detectorLead = 0))
        w.counter(2_030f)
        assertEquals(30, w.saved)
    }

    @Test fun `recovered gap doesn't double count the detector's earlier lead`() {
        // Last session saved 5 detector steps the counter hadn't reported yet; it reported them later.
        val w = Walk(StepCounterTracker.Restored(counterValue = 2_000f, detectorLead = 5))
        w.counter(2_030f)
        assertEquals(25, w.saved)
    }

    @Test fun `counter reset after reboot doesn't count the old baseline`() {
        val w = Walk(StepCounterTracker.Restored(counterValue = 9_000f, detectorLead = 0))
        w.counter(40f)
        assertEquals(0, w.saved)
        w.counter(55f)
        assertEquals(15, w.saved)
    }

    @Test fun `detector-only devices count every step`() {
        val w = Walk()
        w.detect(7)
        assertEquals(7, w.saved)
    }

    @Test fun `counter-only devices count from the first reading`() {
        val w = Walk()
        w.counter(700f)
        w.counter(720f)
        assertEquals(20, w.saved)
    }

    @Test fun `detector lead is tracked for the next restart`() {
        val w = Walk()
        w.counter(1_000f)
        w.detect(10)
        w.counter(1_004f)
        assertEquals(6, w.tracker.detectorLead)
        assertEquals(1_004f, w.tracker.lastCounterValue)
    }
}
