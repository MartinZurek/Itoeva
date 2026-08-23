package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayFootballSkillTest {
    @Test
    fun `nur eine benannte Fussballanimation lehrt den Trick`() {
        assertTrue(PlayFootballSkill.isFootballAnimation(AnimationType.MOVE, "Football"))
        assertTrue(PlayFootballSkill.isFootballAnimation(AnimationType.MOVE, "Fußball Trick"))
        assertFalse(PlayFootballSkill.isFootballAnimation(AnimationType.MOVE, null))
        assertFalse(PlayFootballSkill.isFootballAnimation(AnimationType.MOVE, "Spazieren"))
        assertFalse(PlayFootballSkill.isFootballAnimation(AnimationType.DRINK, "Football"))
    }
}
