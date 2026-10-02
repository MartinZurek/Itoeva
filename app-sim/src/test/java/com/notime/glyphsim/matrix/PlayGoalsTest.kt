package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayGoals.Intention
import com.notime.glyphsim.matrix.PlayGoals.Kind
import com.notime.glyphsim.matrix.PlayGoals.Progress
import com.notime.glyphsim.matrix.PlayGoals.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayGoalsTest {
    @Test
    fun `visible session count matches real project completion`() {
        for (project in PlayGoals.Project.entries) {
            var progress = PlayGoals.Progress(project = project.ordinal)
            repeat(PlayGoals.sessionCount(project)) { session ->
                val day = session.toLong() + 10
                val step = PlayGoals.due(progress, day, 16 * 60, homeDay = false, away = false)!!
                progress = PlayGoals.completed(progress, step, day)
                if (session + 1 < PlayGoals.sessionCount(project)) {
                    assertEquals(project, PlayGoals.currentProject(progress))
                    assertEquals(session + 1, progress.session)
                }
            }
            assertTrue(project in progress.finished)
            assertEquals(0, progress.session)
            assertEquals(project.ordinal + 1, progress.project)
        }
    }

    /** Ein Leben im Zeitraffer: jede Viertelstunde fragen, was dran ist, und es tun. */
    private fun leben(tage: Int, heimtag: (Long) -> Boolean = { true }): Pair<Progress, List<Pair<Long, PlayGoals.Step>>> {
        var stand = Progress()
        val getan = mutableListOf<Pair<Long, PlayGoals.Step>>()
        for (tag in 0L until tage) for (m in 0 until 24 * 60 step 15) {
            val schritt = PlayGoals.due(stand, tag, m, heimtag(tag), away = false) ?: continue
            stand = PlayGoals.completed(stand, schritt, tag)
            getan += tag to schritt
        }
        return stand to getan
    }

    @Test
    fun `an Tagen daheim gibt es jeden Tag ein Vorhaben und einen Arbeitsgang`() {
        val (_, getan) = leben(8)
        for (tag in 0L until 8) {
            assertEquals("Tag $tag", 1, getan.count { it.first == tag && it.second.kind == Kind.INTENTION })
            assertEquals("Tag $tag", 1, getan.count { it.first == tag && it.second.kind == Kind.PROJECT })
        }
    }

    @Test
    fun `die Projekte werden der Reihe nach fertig und bleiben stehen`() {
        val (stand, _) = leben(12)
        assertTrue(Project.BIRDHOUSE in stand.finished)
        assertTrue(Project.HERBS in stand.finished)
        assertTrue(Project.KITE in stand.finished)
        val dinge = PlayGoals.acquisitions(stand)
        assertTrue(PlayScene.Acquisition.BIRDHOUSE in dinge)
        assertTrue(PlayScene.Acquisition.KITE_WALL in dinge)
        assertTrue(PlayScene.Acquisition.HERB_BUSH in dinge)
        assertTrue(PlayScene.Acquisition.HERB_SPROUT !in dinge)
    }

    @Test
    fun `der Kraeutertopf waechst sichtbar mit`() {
        val keimling = Progress(project = Project.HERBS.ordinal, session = 2, finished = setOf(Project.BIRDHOUSE))
        assertTrue(PlayScene.Acquisition.HERB_SPROUT in PlayGoals.acquisitions(keimling))
        val vorher = Progress(project = Project.HERBS.ordinal, session = 1)
        assertTrue(PlayScene.Acquisition.HERB_SPROUT !in PlayGoals.acquisitions(vorher))
    }

    @Test
    fun `unterwegs ruht alles, und an Reisetagen gibt es kein Tagesvorhaben`() {
        assertNull(PlayGoals.due(Progress(), 0, 10 * 60 + 30, homeDay = true, away = true))
        val (_, getan) = leben(6) { false }
        assertTrue(getan.none { it.second.kind == Kind.INTENTION })
        // Nach der Heimkehr am Abend geht es mit Arbeitsgaengen daheim weiter.
        assertTrue(getan.any { it.second.kind == Kind.PROJECT })
    }

    @Test
    fun `draussen gearbeitet wird nur bei Tageslicht`() {
        // Der erste Gang des Vogelhauses fuehrt in den Wald.
        assertNull(PlayGoals.due(Progress(), 0, 20 * 60, homeDay = false, away = false))
        assertNotNull(PlayGoals.due(Progress(), 0, 16 * 60, homeDay = false, away = false))
    }

    @Test
    fun `jedes Vorhaben hat seine Zeit`() {
        assertTrue(20 * 60 !in PlayGoals.windowOf(Intention.SUNSET))
        assertTrue(22 * 60 + 15 in PlayGoals.windowOf(Intention.STARS))
        assertEquals(Intention.entries.toSet(), (0L until 8).map { PlayGoals.intentionFor(it) }.toSet())
    }

    @Test
    fun `jeder Ablauf spricht nur Plaetze an, die es am Ort gibt, und legt ab, was er nimmt`() {
        for (routine in PlayGoals.allRoutines()) {
            var ort: PlayScene.Place? = null
            var inDerHand = 0
            for (step in routine.steps) {
                when (step) {
                    is RoutineStep.GoToPlace -> ort = step.place
                    is RoutineStep.GoTo -> {
                        val hier = ort
                        assertNotNull("GoTo ohne Ort: $routine", hier)
                        for (species in AvatarSpecies.entries) {
                            assertTrue("${step.station} fehlt an $hier ($species)", step.station in PlayScene.stationsAt(hier!!, species))
                        }
                    }
                    is RoutineStep.Take -> inDerHand++
                    RoutineStep.Drop -> inDerHand--
                    else -> Unit
                }
            }
            assertEquals("Genommenes bleibt in der Hand: $routine", 0, inDerHand)
        }
    }

    @Test
    fun `der Stand ueberlebt das Speichern`() {
        val stand = Progress(project = 4, session = 2, lastProjectDay = 17, intentionDay = 16, finished = setOf(Project.KITE))
        assertEquals(stand, PlayGoals.decode(PlayGoals.encode(stand)))
        assertEquals(Progress(), PlayGoals.decode(null))
        assertEquals(Progress(), PlayGoals.decode("kaputt"))
    }

    /** Was die Vorhaben hinterlassen, muss man sehen - bei jedem Wesen und jeder Bildbreite. */
    @Test
    fun `jedes Ergebnis ist zu sehen`() {
        val fehlend = mutableListOf<String>()
        val pfade: List<Set<PlayScene.Acquisition>> = listOf(
            emptySet(),
            setOf(PlayScene.Acquisition.BACKPACK, PlayScene.Acquisition.WALLMAP, PlayScene.Acquisition.SCOOTER),
            setOf(PlayScene.Acquisition.FEEDBOWL, PlayScene.Acquisition.PETBASKET, PlayScene.Acquisition.SLEEPING_PET),
            setOf(PlayScene.Acquisition.BLANKET, PlayScene.Acquisition.MOBILE, PlayScene.Acquisition.STARJAR),
            setOf(PlayScene.Acquisition.TOOLBOX, PlayScene.Acquisition.CONTRAPTION, PlayScene.Acquisition.SIGNALRIG)
        )
        val ergebnisse = listOf(
            PlayScene.Acquisition.BIRDHOUSE, PlayScene.Acquisition.HERB_SPROUT,
            PlayScene.Acquisition.HERB_BUSH, PlayScene.Acquisition.KITE_WALL
        )
        for (ergebnis in ergebnisse) {
            for (breite in listOf(PlayScene.MIN_SCENE_CELLS, 46, 54, 64, 72)) {
                for (species in AvatarSpecies.entries) for (boden in listOf(24, 82)) for (pfad in pfade) {
                    val ohne = PlayScene.build(
                        ergebnis.place, 0, breite, boden, PlayAmbientActivity.DayPhase.MIDDAY,
                        species = species, acquisitions = pfad
                    ).associate { (it.x to it.y) to it.brightness }
                    val mit = PlayScene.build(
                        ergebnis.place, 0, breite, boden, PlayAmbientActivity.DayPhase.MIDDAY,
                        species = species, acquisitions = pfad + ergebnis
                    ).associate { (it.x to it.y) to it.brightness }
                    // Nach Helligkeit verglichen, nicht nur nach Belegung: Draussen fuellt der
                    // Hintergrund die Stellen ohnehin, ein Ding davor aendert dort nur den Ton.
                    val anders = mit.count { (pos, hell) -> ohne[pos] != hell }
                    if (anders < 8) fehlend += "$ergebnis/$species/$breite/$boden/${pfad.size}"
                }
            }
        }
        assertEquals(emptyList<String>(), fehlend.distinct().take(12))
    }
}
