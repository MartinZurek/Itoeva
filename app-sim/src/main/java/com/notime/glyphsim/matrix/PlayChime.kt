package com.notime.glyphsim.matrix

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * **Der Klang des Wesens** - drei bis fuenf Toene, gerechnet statt abgespielt.
 *
 * Bis hierher war diese App vollstaendig stumm, und das war keine Luecke, sondern eine Aussage:
 * Der Glyph Toy erinnert ueber Licht. Was trotzdem fehlte, ist der Fall, in dem Ton FUER den
 * Nutzer arbeitet statt gegen ihn - eine Rueckmeldung darauf, dass etwas angekommen ist. Genau
 * dafuer und fuer nichts sonst gibt es diese Datei.
 *
 * **Warum gerechnet und nicht als Audiodatei.**
 *
 * 1. **Keine Lizenzfrage.** Eine Tonspur im Play Store braucht Rechte; eine Rechenvorschrift
 *    nicht. Beim Stand dieses Projekts (Android-Release zuerst) waere das eine Abhaengigkeit
 *    ohne Gegenwert.
 * 2. **Es passt zur Welt.** Die ganze Darstellung besteht aus gerechneten Zellen - Requisiten,
 *    Figuren, Wetter, Licht. Ein aufgenommener Klang daneben waere derselbe Bruch wie eine
 *    fotografierte Blume in einer Pixel-Kulisse. Eine Rechteckschwingung ist das klangliche
 *    Gegenstueck zu einer Silhouette aus sechzehn Zellen.
 * 3. **Es kostet nichts.** Kein Asset, keine Groesse im Paket, kein Dekoder.
 *
 * **Jede Kreatur hat ihr eigenes Motiv**, abgeleitet aus demselben Charakter wie Silhouette,
 * Wohnung und Stimme (siehe [AvatarSpecies]): Wyrmling geht hinauf und wird lauter, Gloop sackt
 * ab, Hootlet ruft zweimal denselben tiefen Ton. Es ist dieselbe Handschrift, nur in einer
 * anderen Sinnesart - und der billigste Wiedererkennungswert, den diese Welt zu vergeben hat.
 *
 * Wo und unter welchen Bedingungen das ueberhaupt erklingt, entscheidet
 * [com.notime.glyphsim.ui.PlaySound] - hier steht nur, WIE es klingt.
 */
object PlayChime {

    /** Womit ein Klang ausgeloest wird. */
    enum class Event {
        /** Eine Erinnerung wurde beantwortet - die haeufigste und deshalb kuerzeste Rueckmeldung. */
        FEED,

        /** Eine Stufe geschafft - der einzige Klang, der laenger sein darf. */
        LEVEL_UP,

        /** Jemand kommt vorbei - ein kurzer Gruss, kein Ereignis. */
        VISIT
    }

    /**
     * Ein Ton: Tonhoehe in Halbtoenen ueber dem Grundton, Dauer, Lautstaerke.
     *
     * In Halbtoenen statt in Hertz, weil sich Motive so ueberhaupt erst lesen lassen: "0, 4, 7"
     * ist ein Dur-Dreiklang, "440, 554, 659" ist eine Zahlenreihe. Und weil sich damit die
     * Grundtonhoehe je Ereignis verschieben laesst, ohne jedes Motiv neu zu schreiben.
     */
    data class Note(val semitones: Int, val millis: Int, val level: Float = 0.6f)

    /** Der Grundton, ueber dem alle Motive stehen - A4. */
    private const val ROOT_HZ = 440.0

    /** Abtastrate: fuer Rechteckschwingungen in diesem Tonbereich mehr als genug. */
    const val SAMPLE_RATE = 22_050

    /**
     * Das Motiv einer Kreatur zu einem Ereignis.
     *
     * **Ein Motiv je Kreatur, drei Abwandlungen je Ereignis** - nicht achtzehn eigene Melodien.
     * Wiedererkennbar wird ein Klang dadurch, dass er derselbe BLEIBT; das Ereignis aendert nur
     * Laenge und Richtung. Fuettern ist die kurze Fassung, ein Aufstieg die volle mit Aufschlag
     * nach oben, ein Besuch die halbe.
     */
    fun motifFor(species: AvatarSpecies, event: Event): List<Note> {
        val base = baseMotif(species)
        return when (event) {
            // Kurz: Diese Rueckmeldung kommt am haeufigsten, und alles, was man oft hoert, muss
            // kurz sein.
            Event.FEED -> base.take(3)
            // Voll, plus ein Ton eine Oktave hoeher als Abschluss.
            Event.LEVEL_UP -> base + Note(base.last().semitones + 12, 220, 0.55f)
            // Nur die ersten beiden, leiser: ein Nicken, keine Ansage.
            Event.VISIT -> base.take(2).map { it.copy(level = it.level * 0.7f) }
        }
    }

    /**
     * Der Kern des Charakters als Tonfolge - abgeleitet aus denselben Beschreibungen, aus denen
     * Silhouette und Wohnung entstanden sind.
     */
    private fun baseMotif(species: AvatarSpecies): List<Note> = when (species) {
        // Der neugierige Optimist: hinauf, leicht, ohne Nachdruck.
        AvatarSpecies.PUFFLING -> listOf(
            Note(0, 90), Note(4, 90), Note(7, 140), Note(12, 160)
        )
        // Die Traeumerin: hohe, weiche Toene, die eher schweben als anschlagen.
        AvatarSpecies.STARLET -> listOf(
            Note(12, 150, 0.45f), Note(16, 150, 0.45f), Note(19, 260, 0.4f), Note(14, 220, 0.35f)
        )
        // Der Motivator: kurze Fanfare, jeder Ton lauter als der davor.
        AvatarSpecies.WYRMLING -> listOf(
            Note(0, 70, 0.5f), Note(7, 70, 0.6f), Note(12, 70, 0.7f), Note(16, 200, 0.8f)
        )
        // Der ruhige Beschuetzer: zwei gleiche Toene, ruhig, wie ein Leuchtfeuer.
        AvatarSpecies.FENNEC -> listOf(
            Note(5, 200, 0.5f), Note(5, 120, 0.4f), Note(9, 260, 0.5f)
        )
        // Der Entschleuniger: sackt ab und wird dabei langsamer.
        AvatarSpecies.GLOOP -> listOf(
            Note(7, 160, 0.5f), Note(3, 200, 0.45f), Note(0, 300, 0.4f)
        )
        // Der Beobachter: zweimal derselbe tiefe Ton - ein Ruf, keine Melodie.
        AvatarSpecies.HOOTLET -> listOf(
            Note(-5, 220, 0.45f), Note(-5, 120, 0.3f), Note(-10, 320, 0.4f)
        )
    }

    /**
     * Rechnet ein Motiv in Abtastwerte um (16 Bit, mono).
     *
     * **Rechteck statt Sinus** - das ist der Chiptune-Klang, und er passt zu einer Welt aus
     * Zellen: beides kennt nur an und aus.
     *
     * **Die Huellkurve ist kein Schmuck, sondern die Bedingung dafuer, dass es nicht knackt.**
     * Ein Rechteck, das mitten in der Schwingung anfaengt und aufhoert, erzeugt an beiden Enden
     * einen Sprung im Signal - und den hoert man als Knacken, nicht als Ton. Ein kurzes Auf- und
     * Abblenden von wenigen Millisekunden beseitigt das vollstaendig.
     */
    fun render(notes: List<Note>, sampleRate: Int = SAMPLE_RATE): ShortArray {
        val total = notes.sumOf { it.millis * sampleRate / 1000 }
        val out = ShortArray(total)
        var offset = 0
        for (note in notes) {
            val length = note.millis * sampleRate / 1000
            val hz = ROOT_HZ * Math.pow(2.0, note.semitones / 12.0)
            val period = sampleRate / hz
            val fade = (sampleRate * FADE_MS / 1000).coerceAtMost(length / 2).coerceAtLeast(1)
            for (i in 0 until length) {
                // Rechteck: erste Halbwelle oben, zweite unten.
                val phase = (i % period.roundToInt().coerceAtLeast(1)).toDouble() / period
                val square = if (phase < 0.5) 1.0 else -1.0
                // Zusaetzlich ein leiser Sinus derselben Frequenz: nimmt dem Rechteck die
                // Schaerfe, ohne ihm seinen Charakter zu nehmen.
                val body = square * 0.8 + sin(2 * PI * phase) * 0.2
                val envelope = when {
                    i < fade -> i.toDouble() / fade
                    i > length - fade -> (length - i).toDouble() / fade
                    else -> 1.0
                }
                out[offset + i] = (body * envelope * note.level * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            offset += length
        }
        return out
    }

    /** Auf- und Abblendzeit je Ton - siehe [render]. */
    private const val FADE_MS = 6
}
