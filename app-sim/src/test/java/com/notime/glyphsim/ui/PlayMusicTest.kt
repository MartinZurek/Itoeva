package com.notime.glyphsim.ui

import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.MusicContext
import com.notime.glyphsim.matrix.MusicRole
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayCharacterTheme
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.settings.SettingsCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, OB im Spielmodus ueberhaupt Musik laufen darf - die zweite Haelfte der Trennung
 * "die Welt entscheidet was, der Nutzer entscheidet ob".
 *
 * Diese Faelle sind am Geraet kaum herzustellen: Man muesste mit laufendem Podcast und stumm
 * gestelltem Telefon in den Spielmodus gehen, und zwar in einem Build, in dem der passende
 * Track noch fehlt.
 */
class PlayMusicTest {

    private val abendsZuhause = MusicContext(
        PlayAmbientActivity.DayPhase.EVENING, PlayScene.Place.LIVING
    )
    private val mittagsImPark = MusicContext(
        PlayAmbientActivity.DayPhase.MIDDAY, PlayScene.Place.PARK
    )

    /** Der heutige Auslieferungsstand: Tages- und Abendtrack. */
    private val heute = setOf(MusicRole.HOME_EVENING, MusicRole.MAIN_DAY)

    private fun decide(
        enabled: Boolean = true,
        context: MusicContext = abendsZuhause,
        available: Set<MusicRole> = heute,
        otherAudioActive: Boolean = false,
        deviceSilent: Boolean = false
    ) = PlayMusic.decide(enabled, context, available, otherAudioActive, deviceSilent)

    // ================= Das OB =================

    @Test
    fun `eingeschaltet, Track passend, nichts im Weg - dann laeuft sie`() {
        assertEquals(MusicRole.HOME_EVENING, decide())
    }

    /**
     * **Die wichtigste Zeile dieser Datei.** Beim allerersten Start ist der Schalter aus; eine
     * App, die nach einem Update ungefragt Musik spielt, hat ihr Vertrauen verspielt, bevor der
     * erste Takt vorbei ist.
     */
    @Test
    fun `der Standard beim allerersten Start ist aus`() {
        assertFalse(SettingsCatalog.MusicEnabled.default)
    }

    /**
     * **Kein Szenenwechsel darf Musik eigenmaechtig einschalten.** Ausgeschaltet heisst
     * ausgeschaltet, egal wie gut die Szene passen wuerde - deshalb steht `enabled` in
     * [PlayMusic.decide] vor dem Resolver und nicht daneben.
     */
    @Test
    fun `ausgeschaltet schlaegt jede noch so passende Szene`() {
        assertNull(decide(enabled = false))
        assertNull(decide(enabled = false, context = mittagsImPark))
        assertNull(decide(enabled = false, available = MusicRole.entries.toSet()))
    }

    /**
     * Laeuft ein Podcast, bleibt es still - und zwar ohne Audio-Focus anzufordern. Wer Focus
     * greift, pausiert die Wiedergabe des Nutzers; ein Spielmodus, der den Podcast anhaelt, ist
     * kaputt, unabhaengig davon, wie huebsch die Musik war.
     */
    @Test
    fun `ueber fremdem Ton bleibt es still`() {
        assertNull(decide(otherAudioActive = true))
    }

    @Test
    fun `bei stumm gestelltem Geraet bleibt es still`() {
        assertNull(decide(deviceSilent = true))
    }

    // ================= Das OB trifft das WAS =================

    /**
     * Eingeschaltet, aber fuer diese Lage gibt es nichts: still. Das bleibt wichtig fuer Builds,
     * in denen ein erzeugtes Asset noch nicht gemergt ist.
     */
    @Test
    fun `eingeschaltet ohne passenden Track ergibt Stille`() {
        assertNull(decide(context = mittagsImPark, available = setOf(MusicRole.HOME_EVENING)))
    }

    /** Sobald der Tages-Track existiert, fuellt sich genau diese Luecke - ohne weitere Aenderung. */
    @Test
    fun `mit Tages-Track wird aus der Mittagsstille Musik`() {
        assertEquals(
            MusicRole.MAIN_DAY,
            decide(context = mittagsImPark)
        )
    }

    /** Jede Sperre allein genuegt; sie muessen sich nicht gegenseitig aufheben koennen. */
    @Test
    fun `jede Sperre allein genuegt`() {
        assertNull(decide(enabled = false, otherAudioActive = true))
        assertNull(decide(otherAudioActive = true, deviceSilent = true))
        assertNull(decide(enabled = false, deviceSilent = true, available = emptySet()))
    }

    // ================= Das Charakterstueck =================

    private val begruessung = mittagsImPark.copy(characterTheme = AvatarSpecies.WYRMLING)

    /** Variante 3 gehoert dem Wyrmling - und nur die darf es zu hoeren bekommen. */
    private val wyrmlingVariante = MusicRole.characterThemeVariant(AvatarSpecies.WYRMLING)

    /**
     * **Der Fall, den es heute fuer fuenf von sechs Wesen gibt.** Sobald ein einziges Themenstueck
     * ausgeliefert ist, gilt die Rolle als vorhanden; ohne [PlayMusic.rolesFor] bekaeme das
     * Wyrmling damit das Stueck des Pufflings zu hoeren.
     */
    @Test
    fun `ein fremdes Themenstueck zaehlt nicht als das eigene`() {
        val angebot = heute + MusicRole.CHARACTER_THEME
        assertEquals(
            angebot - MusicRole.CHARACTER_THEME,
            PlayMusic.rolesFor(angebot, begruessung, characterThemeVariants = listOf(1))
        )
        assertEquals(
            angebot,
            PlayMusic.rolesFor(angebot, begruessung, listOf(1, wyrmlingVariante))
        )
    }

    @Test
    fun `ohne Anlass bleibt das Angebot unveraendert`() {
        val angebot = heute + MusicRole.CHARACTER_THEME
        // Ohne gesetztes Wesen faellt die Rolle heraus - der Resolver wuerde sie ohnehin nie
        // vorschlagen, aber ein Angebot, das etwas Unerreichbares enthaelt, waere irrefuehrend.
        assertEquals(heute, PlayMusic.rolesFor(angebot, mittagsImPark, listOf(1, 2, 3)))
        // Und eine Rollenmenge ohne das Thema bleibt unangetastet.
        assertEquals(heute, PlayMusic.rolesFor(heute, begruessung, emptyList()))
    }

    /**
     * **Warum das Thema nicht rotieren darf.** Ueberall sonst sind mehrere Varianten mehrere
     * Stuecke derselben Stimmung; hier gehoert jede einem Wesen. Ein Wechsel nach fuenf Minuten
     * waere kein frischer Track, sondern ein anderes Wesen.
     */
    @Test
    fun `die Variante des Themas steht fest, jede andere rotiert frei`() {
        assertEquals(
            wyrmlingVariante,
            PlayMusic.fixedVariant(MusicRole.CHARACTER_THEME, begruessung)
        )
        assertNull(PlayMusic.fixedVariant(MusicRole.MAIN_DAY, begruessung))
        assertNull(PlayMusic.fixedVariant(MusicRole.HOME_EVENING, begruessung))
        assertNull(PlayMusic.fixedVariant(MusicRole.CHARACTER_THEME, mittagsImPark))
    }

    /**
     * Die Laenge der Begruessung ist um genau eine Ueberblendung kuerzer als das Stueck (siehe
     * [com.notime.glyphsim.matrix.PlayCharacterTheme]). Beide Zahlen stehen an ihrem eigenen Ort;
     * dass sie zusammenpassen, soll ein Test sagen und nicht das Ohr.
     */
    @Test
    fun `Begruessungsfenster und Ueberblendung passen zusammen`() {
        assertEquals(PlayMusic.CROSSFADE_MS, PlayCharacterTheme.FADE_MS)
    }

    // ================= Die Ueberblendung =================

    @Test
    fun `Ueberblendung behaelt an Anfang Mitte und Ende ihre Energie`() {
        val start = PlayMusic.transitionVolumes(0f)
        val middle = PlayMusic.transitionVolumes(0.5f)
        val end = PlayMusic.transitionVolumes(1f)

        assertEquals(0.35f, start.first, 0.0001f)
        assertEquals(0f, start.second, 0.0001f)
        assertTrue(middle.first > 0f && middle.second > 0f)
        assertEquals(middle.first, middle.second, 0.0001f)
        assertEquals(0f, end.first, 0.0001f)
        assertEquals(0.35f, end.second, 0.0001f)
    }

    @Test
    fun `unterbrochene Ueberblendung setzt ohne Lautstaerkesprung fort`() {
        val teilweiseEingeblendet = PlayMusic.transitionVolumes(0.25f).second
        val neuerWechsel = PlayMusic.transitionVolumes(0f, teilweiseEingeblendet)

        assertEquals(teilweiseEingeblendet, neuerWechsel.first, 0.0001f)
        assertEquals(0f, neuerWechsel.second, 0.0001f)
    }
}
