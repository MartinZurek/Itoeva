package com.notime.glyphsim.ui

import android.content.Context
import androidx.annotation.StringRes
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarSpecies

/**
 * **Die Geschichte des Wesens und seiner Welt** - in sieben Stuecken, die man sich erzaehlen
 * laesst, eines nach dem anderen.
 *
 * **Der eine Ort, an dem er etwas sagt, das nicht aus Daten kommt.** Alles Uebrige in diesem
 * Gespraech ist Auskunft ueber Zustaende: was heute geschah, wie die Woche stand, wo er gerade ist
 * (siehe [PlayTalk]). Der Grundsatz dort lautet: Er weiss wenig, aber was er sagt, stimmt. Hier
 * gilt er weiterhin - nur ist der Gegenstand ein anderer. Ueber SICH SELBST kann ein Wesen nichts
 * Falsches sagen; es gibt keinen Zustand, der dem widersprechen koennte. Deshalb ist das
 * Erzaehlen die einzige Stelle, an der erfundener Text nicht in Konflikt mit der Welt geraet.
 *
 * **Warum es das ueberhaupt braucht.** Die sechs Kreaturen hatten bisher einen Charakter, aber
 * keine Geschichte: eine Silhouette, eine Wohnung, einen Beruf, eine Landschaft - lauter
 * Eigenschaften, kein Leben. Man konnte ihnen zusehen und sie unterscheiden, aber nichts ueber
 * sie erfahren. Sieben kurze Stuecke aendern das, ohne dass eine einzige Zeile Spiellogik
 * dazukommt.
 *
 * **Aufbau je Wesen** - die Reihenfolge ist die eines Kennenlernens und nicht beliebig:
 *
 * 1. Wer er ist und was er tut
 * 2. Wie er wohnt
 * 3. Was er gern macht
 * 4. Wen er kennt
 * 5. Wie es bei ihm draussen aussieht
 * 6. Etwas, das er sonst nicht sagt
 * 7. Etwas ueber die gemeinsame Welt
 *
 * **Die vierte Stelle haelt die Welt zusammen.** Jedes Wesen erzaehlt dort von EINEM anderen -
 * Puffling von Gloop, Gloop von Puffling, Wyrmling von Fennec, Fennec von Wyrmling, Starlet von
 * Hootlet, Hootlet von Starlet. Erst dadurch sind es nicht sechs Figuren mit je eigener Welt,
 * sondern sechs Bewohner derselben. Und die Stellen fuenf und sieben erzaehlen dieselben Orte -
 * Laden, Strasse, Wald, Mond, Wetter - aus sechs verschiedenen Blickwinkeln.
 *
 * **Wieviel man auf einmal erfaehrt, entscheidet der Nutzer.** Es gibt kein Zeitschloss und keine
 * Bedingung: Wer alles hintereinander hoeren will, tippt siebenmal. Ein Wesen, das seine
 * Geschichte portionsweise nach Kalender freigibt, waere ein Belohnungsplan - und genau den soll
 * diese App nicht haben (siehe [com.notime.glyphsim.matrix.CompanionChapter] fuer dieselbe
 * Ueberlegung).
 */
object PlayLore {

    private const val PREFS = "play_lore"

    /** Wieviele Stuecke jedes Wesen zu erzaehlen hat. */
    const val PIECES = 7

    /**
     * Die Geschichte eines Wesens, in der Reihenfolge, in der sie erzaehlt wird.
     *
     * Die Texte stehen als Ressourcen und nicht im Code: Sie sind das einzige an dieser Datei, das
     * uebersetzt werden muss - und das einzige, das sich aendern wird, ohne dass sich die Mechanik
     * darum aendert.
     */
    @StringRes
    fun story(species: AvatarSpecies): List<Int> = when (species) {
        AvatarSpecies.PUFFLING -> listOf(
            R.string.lore_puffling_1, R.string.lore_puffling_2, R.string.lore_puffling_3,
            R.string.lore_puffling_4, R.string.lore_puffling_5, R.string.lore_puffling_6,
            R.string.lore_puffling_7
        )
        AvatarSpecies.STARLET -> listOf(
            R.string.lore_starlet_1, R.string.lore_starlet_2, R.string.lore_starlet_3,
            R.string.lore_starlet_4, R.string.lore_starlet_5, R.string.lore_starlet_6,
            R.string.lore_starlet_7
        )
        AvatarSpecies.WYRMLING -> listOf(
            R.string.lore_wyrmling_1, R.string.lore_wyrmling_2, R.string.lore_wyrmling_3,
            R.string.lore_wyrmling_4, R.string.lore_wyrmling_5, R.string.lore_wyrmling_6,
            R.string.lore_wyrmling_7
        )
        AvatarSpecies.FENNEC -> listOf(
            R.string.lore_fennec_1, R.string.lore_fennec_2, R.string.lore_fennec_3,
            R.string.lore_fennec_4, R.string.lore_fennec_5, R.string.lore_fennec_6,
            R.string.lore_fennec_7
        )
        AvatarSpecies.GLOOP -> listOf(
            R.string.lore_gloop_1, R.string.lore_gloop_2, R.string.lore_gloop_3,
            R.string.lore_gloop_4, R.string.lore_gloop_5, R.string.lore_gloop_6,
            R.string.lore_gloop_7
        )
        AvatarSpecies.HOOTLET -> listOf(
            R.string.lore_hootlet_1, R.string.lore_hootlet_2, R.string.lore_hootlet_3,
            R.string.lore_hootlet_4, R.string.lore_hootlet_5, R.string.lore_hootlet_6,
            R.string.lore_hootlet_7
        )
    }

    /**
     * Wieviel dieses Wesen schon erzaehlt hat.
     *
     * **Je Wesen gezaehlt und nicht insgesamt.** Wer den Avatar wechselt, faengt bei dem neuen von
     * vorn an - er hat ja seine eigene Geschichte. Und wer zurueckwechselt, ist da, wo er
     * aufgehoert hat: Was einem jemand erzaehlt hat, vergisst man nicht, weil man zwischendurch
     * mit jemand anderem geredet hat.
     */
    fun heard(context: Context, species: AvatarSpecies): Int =
        prefs(context).getInt(key(species), 0).coerceIn(0, PIECES)

    /** Das naechste Stueck - `null`, wenn alles erzaehlt ist. */
    @StringRes
    fun nextPiece(context: Context, species: AvatarSpecies): Int? {
        val index = heard(context, species)
        return story(species).getOrNull(index)
    }

    /** Haelt fest, dass ein Stueck erzaehlt wurde. */
    fun remember(context: Context, species: AvatarSpecies) {
        val next = (heard(context, species) + 1).coerceAtMost(PIECES)
        prefs(context).edit().putInt(key(species), next).apply()
    }

    /** Ob es noch etwas zu erzaehlen gibt. */
    fun hasMore(context: Context, species: AvatarSpecies): Boolean =
        heard(context, species) < PIECES

    /**
     * Von vorn hoeren.
     *
     * Weniger ein Bedienelement als eine Frage der Anstaendigkeit: Eine Geschichte, die man genau
     * einmal lesen darf und danach nie wieder, waere in einer App, die man ueber Monate benutzt,
     * eine seltsame Sparsamkeit.
     */
    fun forget(context: Context, species: AvatarSpecies) {
        prefs(context).edit().remove(key(species)).apply()
    }

    /**
     * Ein Schluessel je Wesen - deshalb steht diese Ablage nicht im SettingsCatalog: Der fuehrt
     * feste Schluessel, hier haengt einer an einem Enum-Wert.
     */
    private fun key(species: AvatarSpecies) = "heard_${species.name}"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
