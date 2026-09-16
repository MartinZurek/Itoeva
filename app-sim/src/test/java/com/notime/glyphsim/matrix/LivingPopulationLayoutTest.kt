package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verhaltenstest fuer die schmale Darstellungsgrenze aus NT-089. */
class LivingPopulationLayoutTest {

    private fun resident(
        index: Int,
        place: PlayScene.Place?,
        present: Boolean = true,
        minuteOfDay: Int = 9 * 60
    ) = ResidentSnapshot(
        profileId = "resident:test:$index",
        role = ResidentRole.entries[index % ResidentRole.entries.size],
        species = AvatarSpecies.entries[index % AvatarSpecies.entries.size],
        place = place,
        site = if (place == null) LivingSite.HOME else LivingRuntimeAdapter.siteFor(place),
        goal = GoalKind.HAVE_FUN,
        nextAction = ActionKind.PURSUE_INTEREST,
        blockedBy = null,
        coins = index,
        portions = 1,
        minuteOfDay = minuteOfDay,
        publiclyPresent = present
    )

    @Test
    fun `nur wirklich anwesende Einwohner desselben Ortes erscheinen`() {
        val sichtbar = resident(0, PlayScene.Place.PARK)
        val zuhause = resident(1, null, present = false)
        val imLaden = resident(2, PlayScene.Place.SHOP)

        val result = LivingPopulationLayout.place(
            listOf(imLaden, zuhause, sichtbar),
            PlayScene.Place.PARK,
            hostLeftFraction = 0.3f,
            hostWidthFraction = 0.4f
        )

        assertEquals(listOf(sichtbar.profileId), result.map { it.resident.profileId })
        assertEquals(
            sichtbar.profileId,
            LivingPopulationLayout.nextVisitor(
                listOf(imLaden, zuhause, sichtbar),
                PlayScene.Place.PARK,
                previousProfileId = null
            )?.profileId
        )
    }

    @Test
    fun `zwei Einwohner passen bei vierzig Zellen neben den Hauptavatar`() {
        val residents = (0..2).map { resident(it, PlayScene.Place.PARK) }

        for (hostLeft in listOf(0f, 0.3f, 0.6f)) {
            val result = LivingPopulationLayout.place(
                residents,
                PlayScene.Place.PARK,
                hostLeftFraction = hostLeft,
                hostWidthFraction = 0.4f
            )

            assertEquals(LivingPopulationLayout.MAX_VISIBLE_RESIDENTS, result.size)
            for (placement in result) {
                assertTrue(placement.widthFraction <= 0.2f)
                assertTrue(
                    placement.leftFraction + placement.widthFraction <= hostLeft ||
                        placement.leftFraction >= hostLeft + 0.4f
                )
            }
            val first = result[0]
            val second = result[1]
            assertTrue(
                first.leftFraction + first.widthFraction <= second.leftFraction ||
                    second.leftFraction + second.widthFraction <= first.leftFraction
            )
        }
    }

    @Test
    fun `laufender Besuch wird nicht als zweite Kopie gezeichnet`() {
        val residents = (0..1).map { resident(it, PlayScene.Place.CITY) }

        val result = LivingPopulationLayout.place(
            residents,
            PlayScene.Place.CITY,
            hostLeftFraction = 0.4f,
            hostWidthFraction = 0.3f,
            visitingProfileId = residents.first().profileId
        )

        assertEquals(listOf(residents.last().profileId), result.map { it.resident.profileId })
    }

    @Test
    fun `eigene Einwohnerzeit versetzt die Ruhebewegung deterministisch`() {
        val holds = listOf(200L, 200L, 400L)
        assertEquals(0, LivingPopulationLayout.idleFrameIndex(holds, 0, 0, 200))
        assertEquals(1, LivingPopulationLayout.idleFrameIndex(holds, 0, 1, 200))
        assertEquals(2, LivingPopulationLayout.idleFrameIndex(holds, 0, 2, 200))
        assertEquals(0, LivingPopulationLayout.idleFrameIndex(holds, 4, 0, 200))
    }
}
