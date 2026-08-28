package com.lifetrack.core

import com.lifetrack.core.ui.GreetingPeriod
import com.lifetrack.core.ui.greetingFor
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {

    @Test
    fun `boundaries land in the expected bucket`() {
        assertEquals(GreetingPeriod.NIGHT, greetingFor(4))
        assertEquals(GreetingPeriod.MORNING, greetingFor(5))
        assertEquals(GreetingPeriod.MORNING, greetingFor(11))
        assertEquals(GreetingPeriod.AFTERNOON, greetingFor(12))
        assertEquals(GreetingPeriod.AFTERNOON, greetingFor(16))
        assertEquals(GreetingPeriod.EVENING, greetingFor(17))
        assertEquals(GreetingPeriod.EVENING, greetingFor(20))
        assertEquals(GreetingPeriod.NIGHT, greetingFor(21))
        assertEquals(GreetingPeriod.NIGHT, greetingFor(23))
        assertEquals(GreetingPeriod.NIGHT, greetingFor(0))
    }
}
