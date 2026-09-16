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
        minuteOfDay: Int = 9 * 60,
        nextAction: ActionKind? = ActionKind.PURSUE_INTEREST,
        blockedBy: com.notime.glyphsim.living.Requirement? = null
    ) = ResidentSnapshot(
        profileId = "resident:test:$index",
        role = ResidentRole.entries[index % ResidentRole.entries.size],
        species = AvatarSpecies.entries[index % AvatarSpecies.entries.size],
        place = place,
        site = if (place == null) LivingSite.HOME else LivingRuntimeAdapter.siteFor(place),
        goal = GoalKind.HAVE_FUN,
        nextAction = nextAction,
        blockedBy = blockedBy,
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

    @Test
    fun `gemeinsames Training braucht zwei wirkliche kompatible Handlungen`() {
        val partner = resident(
            index = 0,
            place = PlayScene.Place.SPORT,
            nextAction = ActionKind.MOVE_BODY
        )

        assertEquals(
            partner.profileId,
            LivingPopulationLayout.sharedSportPartner(
                snapshots = listOf(partner),
                place = PlayScene.Place.SPORT,
                hostCompletedActions = listOf(ActionKind.MOVE_BODY),
                specialActivity = PlayRoutines.SpecialActivity.TRAINING
            )?.profileId
        )
    }

    @Test
    fun `gemeinsames Basketball braucht dieselben zwei wirklichen Handlungen`() {
        val partner = resident(
            index = 0,
            place = PlayScene.Place.SPORT,
            nextAction = ActionKind.MOVE_BODY
        )

        assertEquals(
            partner.profileId,
            LivingPopulationLayout.sharedSportPartner(
                snapshots = listOf(partner),
                place = PlayScene.Place.SPORT,
                hostCompletedActions = listOf(ActionKind.MOVE_BODY),
                specialActivity = PlayRoutines.SpecialActivity.BASKETBALL
            )?.profileId
        )
    }

    @Test
    fun `ort rolle und spezies allein behaupten keine gemeinsame Sportaktivitaet`() {
        val athleteWithoutAction = resident(
            index = ResidentRole.ATHLETE.ordinal,
            place = PlayScene.Place.SPORT,
            nextAction = ActionKind.READ
        ).copy(role = ResidentRole.ATHLETE, species = AvatarSpecies.WYRMLING)
        val cases = listOf(
            LivingPopulationLayout.sharedSportPartner(
                listOf(athleteWithoutAction), PlayScene.Place.SPORT,
                listOf(ActionKind.MOVE_BODY), PlayRoutines.SpecialActivity.TRAINING
            ),
            LivingPopulationLayout.sharedSportPartner(
                listOf(athleteWithoutAction.copy(nextAction = ActionKind.MOVE_BODY)),
                PlayScene.Place.SPORT, listOf(ActionKind.READ),
                PlayRoutines.SpecialActivity.TRAINING
            ),
            // Ein allgemeiner MOVE_BODY-Zustand allein reicht nicht: Weder Fussball noch Drachen
            // oder Angeln sind heute eine gemeinsame Szene - nur TRAINING und BASKETBALL.
            LivingPopulationLayout.sharedSportPartner(
                listOf(athleteWithoutAction.copy(nextAction = ActionKind.MOVE_BODY)),
                PlayScene.Place.SPORT, listOf(ActionKind.MOVE_BODY),
                PlayRoutines.SpecialActivity.FOOTBALL
            ),
            LivingPopulationLayout.sharedSportPartner(
                listOf(athleteWithoutAction.copy(nextAction = ActionKind.MOVE_BODY)),
                PlayScene.Place.SPORT, listOf(ActionKind.MOVE_BODY),
                PlayRoutines.SpecialActivity.KITE
            ),
            LivingPopulationLayout.sharedSportPartner(
                listOf(athleteWithoutAction.copy(nextAction = ActionKind.MOVE_BODY)),
                PlayScene.Place.SPORT, listOf(ActionKind.MOVE_BODY),
                PlayRoutines.SpecialActivity.FISHING
            ),
            LivingPopulationLayout.sharedSportPartner(
                listOf(athleteWithoutAction.copy(
                    nextAction = ActionKind.MOVE_BODY,
                    blockedBy = com.notime.glyphsim.living.Requirement.At(LivingSite.HOME)
                )),
                PlayScene.Place.SPORT, listOf(ActionKind.MOVE_BODY),
                PlayRoutines.SpecialActivity.TRAINING
            ),
            LivingPopulationLayout.sharedSportPartner(
                listOf(athleteWithoutAction.copy(
                    nextAction = ActionKind.MOVE_BODY,
                    blockedBy = com.notime.glyphsim.living.Requirement.At(LivingSite.HOME)
                )),
                PlayScene.Place.SPORT, listOf(ActionKind.MOVE_BODY),
                PlayRoutines.SpecialActivity.BASKETBALL
            ),
            LivingPopulationLayout.sharedSportPartner(
                listOf(athleteWithoutAction.copy(
                    nextAction = ActionKind.MOVE_BODY,
                    publiclyPresent = false
                )),
                PlayScene.Place.SPORT, listOf(ActionKind.MOVE_BODY),
                PlayRoutines.SpecialActivity.TRAINING
            ),
            LivingPopulationLayout.sharedSportPartner(
                listOf(athleteWithoutAction.copy(
                    nextAction = ActionKind.MOVE_BODY,
                    publiclyPresent = false
                )),
                PlayScene.Place.SPORT, listOf(ActionKind.MOVE_BODY),
                PlayRoutines.SpecialActivity.BASKETBALL
            )
        )

        assertTrue(cases.all { it == null })
    }
}
