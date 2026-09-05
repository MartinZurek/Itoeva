package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, WELCHE Musik zu einer Lage passt - nicht, ob welche laeuft.
 *
 * Diese Trennung ist der Grund, warum es diesen Test ueberhaupt geben kann: [MusicResolver]
 * kennt weder Einstellung noch Player noch Android. Am Geraet waeren dieselben Faelle nur
 * herzustellen, indem man vier Tageszeiten abwartet und sechzehn Orte besucht.
 */
class MusicResolverTest {

    private fun ctx(
        phase: PlayAmbientActivity.DayPhase,
        place: PlayScene.Place,
        topic: AnimationType? = null
    ) = MusicContext(phase, place, topic)

    /** Der Stand, der heute tatsaechlich ausgeliefert wird: Tages- und Abendtrack. */
    private val heute = setOf(MusicRole.HOME_EVENING, MusicRole.MAIN_DAY)

    /** Historischer und weiterhin wichtiger Rueckfalltest: nur der Abendtrack ist vorhanden. */
    private val nurAbendTrack = setOf(MusicRole.HOME_EVENING)

    // ================= Der heutige Stand =================

    /**
     * **Die wichtigste Zusicherung dieser Datei.** Solange es keinen Tages-Track gibt, bleibt es
     * morgens und mittags STILL. Ein Abendstueck den ganzen Tag zu spielen waere schlechter als
     * nichts - und es waere der bequeme Fehler, den ein Rueckfall "irgendwas ist besser als
     * Stille" genau hier erzeugen wuerde.
     */
    @Test
    fun `ohne Tages-Track bleiben Morgen und Mittag still`() {
        assertNull(MusicResolver.resolve(ctx(PlayAmbientActivity.DayPhase.MORNING, PlayScene.Place.LIVING), nurAbendTrack))
        assertNull(MusicResolver.resolve(ctx(PlayAmbientActivity.DayPhase.MIDDAY, PlayScene.Place.PARK), nurAbendTrack))
        assertNull(MusicResolver.resolve(ctx(PlayAmbientActivity.DayPhase.MIDDAY, PlayScene.Place.CITY), nurAbendTrack))
    }

    @Test
    fun `Abend und Nacht zu Hause ergeben Quiet Lanterns`() {
        for (phase in listOf(PlayAmbientActivity.DayPhase.EVENING, PlayAmbientActivity.DayPhase.NIGHT)) {
            for (place in listOf(PlayScene.Place.LIVING, PlayScene.Place.BEDROOM, PlayScene.Place.NOOK)) {
                assertEquals(
                    "$phase / $place",
                    MusicRole.HOME_EVENING,
                    MusicResolver.resolve(ctx(phase, place), heute)
                )
            }
        }
    }

    // ================= Die Rangfolge =================

    /**
     * Der Beleg, dass die Rangfolge stimmt, BEVOR es die Tracks gibt: Am Sportplatz steht der
     * Sport-Track vor dem Tages-Track. Sonst liesse sich das erst pruefen, wenn beide erzeugt
     * sind - und dann waere ein falscher Vorrang teuer.
     */
    @Test
    fun `bei echter Bewegung am Sportplatz steht Sport vor dem Tages-Track`() {
        val kandidaten = MusicResolver.candidates(
            ctx(PlayAmbientActivity.DayPhase.MIDDAY, PlayScene.Place.SPORT, AnimationType.MOVE)
        )
        assertEquals(listOf(MusicRole.SPORT, MusicRole.MAIN_DAY), kandidaten)
    }

    @Test
    fun `der Sportplatz allein behauptet noch keine Sporthandlung`() {
        assertEquals(
            listOf(MusicRole.MAIN_DAY),
            MusicResolver.candidates(
                ctx(PlayAmbientActivity.DayPhase.MIDDAY, PlayScene.Place.SPORT, AnimationType.GENERAL)
            )
        )
    }

    /** Sobald es ihn gibt, uebernimmt der spezifischere Track - ohne Aenderung am Resolver. */
    @Test
    fun `ein vorhandener Sport-Track schlaegt den Tages-Track`() {
        val lage = ctx(PlayAmbientActivity.DayPhase.MIDDAY, PlayScene.Place.SPORT, AnimationType.MOVE)
        assertEquals(MusicRole.MAIN_DAY, MusicResolver.resolve(lage, heute))
        assertEquals(
            MusicRole.SPORT,
            MusicResolver.resolve(lage, heute + MusicRole.SPORT)
        )
    }

    /** Nachts wird nicht auf den Tages-Track zurueckgefallen - lieber still als munter. */
    @Test
    fun `nachts gibt es keinen Rueckfall auf den Tages-Track`() {
        val kandidaten = MusicResolver.candidates(
            ctx(PlayAmbientActivity.DayPhase.NIGHT, PlayScene.Place.CITY)
        )
        assertEquals(listOf(MusicRole.HOME_EVENING), kandidaten)
        assertTrue(MusicRole.MAIN_DAY !in kandidaten)
    }

    /** Auch am Sportplatz nicht: Wer nachts dort steht, trainiert nicht. */
    @Test
    fun `nachts gibt es auch am Sportplatz keinen Sport-Track`() {
        assertTrue(
            MusicRole.SPORT !in MusicResolver.candidates(
                ctx(PlayAmbientActivity.DayPhase.NIGHT, PlayScene.Place.SPORT, AnimationType.MOVE)
            )
        )
    }

    @Test
    fun `abends unterwegs klingt der Tag nach, zu Hause nicht`() {
        assertEquals(
            listOf(MusicRole.MAIN_DAY, MusicRole.HOME_EVENING),
            MusicResolver.candidates(ctx(PlayAmbientActivity.DayPhase.EVENING, PlayScene.Place.CITY))
        )
        assertEquals(
            listOf(MusicRole.HOME_EVENING, MusicRole.MAIN_DAY),
            MusicResolver.candidates(ctx(PlayAmbientActivity.DayPhase.EVENING, PlayScene.Place.LIVING))
        )
    }

    @Test
    fun `stille Naturorte gehoeren abends zum ruhigen Track`() {
        for (place in listOf(PlayScene.Place.PARK, PlayScene.Place.FOREST, PlayScene.Place.MEADOW)) {
            assertEquals(
                "$place",
                listOf(MusicRole.HOME_EVENING, MusicRole.MAIN_DAY),
                MusicResolver.candidates(ctx(PlayAmbientActivity.DayPhase.EVENING, place))
            )
        }
    }

    // ================= Vollstaendigkeit =================

    /**
     * Kein Ort und keine Tageszeit darf ohne Vorschlag dastehen - sonst gaebe es eine Lage, in
     * der auch mit vollstaendigem Repertoire nie Musik liefe, und niemand faende heraus, welche.
     */
    @Test
    fun `jede Lage hat mindestens einen Kandidaten`() {
        for (phase in PlayAmbientActivity.DayPhase.entries) {
            for (place in PlayScene.Place.entries) {
                assertTrue(
                    "$phase / $place ohne Kandidat",
                    MusicResolver.candidates(ctx(phase, place)).isNotEmpty()
                )
            }
        }
    }

    /** Ohne einen einzigen Track bleibt es ueberall still - kein Absturz, keine Ausnahme. */
    @Test
    fun `ohne jeden Track bleibt es ueberall still`() {
        for (phase in PlayAmbientActivity.DayPhase.entries) {
            for (place in PlayScene.Place.entries) {
                assertNull("$phase / $place", MusicResolver.resolve(ctx(phase, place), emptySet()))
            }
        }
    }

    /** Kein Kandidat darf doppelt vorkommen - sonst waere die Rangfolge nur scheinbar eindeutig. */
    @Test
    fun `keine Lage nennt dieselbe Rolle zweimal`() {
        for (phase in PlayAmbientActivity.DayPhase.entries) {
            for (place in PlayScene.Place.entries) {
                val k = MusicResolver.candidates(ctx(phase, place))
                assertEquals("$phase / $place", k.size, k.distinct().size)
            }
        }
    }

    // ================= Rollen und Manifest =================

    /**
     * Die Rollennamen sind der Vertrag zum Manifest und zu `generate_music.py`. Ein Tippfehler
     * hier bliebe stumm: Der Track waere erzeugt, aber die App faende ihn nie.
     */
    @Test
    fun `Rollennamen und Ressourcennamen sind eindeutig und stabil`() {
        assertEquals(
            MusicRole.entries.size,
            MusicRole.entries.map { it.manifestName }.distinct().size
        )
        assertEquals(
            MusicRole.entries.size,
            MusicRole.entries.map { it.androidResource }.distinct().size
        )
        assertEquals(MusicRole.HOME_EVENING, MusicRole.byManifestName("home_evening_background"))
        assertEquals(MusicRole.MAIN_DAY, MusicRole.byManifestName("main_day_background"))
        assertNull(MusicRole.byManifestName("gibt_es_nicht"))
        assertEquals("itoeva_home_evening_01", MusicRole.HOME_EVENING.androidResource)
    }
}
