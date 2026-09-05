package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepRoutineTest {
    @Test
    fun `sleep stays in bed until wake step`() {
        val routines = PlayRoutines.allFor(AnimationType.SLEEP)
        assertEquals(1, routines.size)
        val steps = routines.single().steps
        val occupy = steps.indexOfFirst {
            it is RoutineStep.Occupy && it.station == PlayScene.Station.BED
        }
        val sleepUntilMorning = steps.indexOfFirst { it is RoutineStep.SleepUntilMorning }
        val rise = steps.indexOfFirst { it is RoutineStep.Rise }

        assertTrue(occupy >= 0)
        assertTrue(sleepUntilMorning > occupy)
        assertTrue(rise > sleepUntilMorning)
        assertFalse(
            steps.subList(occupy + 1, sleepUntilMorning).any {
                it is RoutineStep.Rise ||
                    (it is RoutineStep.Stir && it.fidget == AvatarAnimations.Fidget.LOOK_AROUND)
            }
        )
    }
}
