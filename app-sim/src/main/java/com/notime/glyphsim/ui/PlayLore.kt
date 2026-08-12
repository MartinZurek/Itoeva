package com.notime.glyphsim.ui

import android.content.Context
import androidx.annotation.StringRes
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarSpecies
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
 * **Ein Stueck am Tag** - die Geschichte laeuft ueber eine Woche, nicht ueber einen Abend.
 *
 * Ein erster Entwurf gab alles sofort frei, mit dem Argument, ein Zeitschloss sei ein
 * Belohnungsplan. Das Argument war nicht falsch, es hat nur den Gegenstand verfehlt: Eine
 * Geschichte, die man in drei Minuten leerliest, ist danach leer - und diese sieben Stuecke sind
 * gerade das, was dem Wesen ueber Wochen hinweg etwas zu sagen gibt. Wer jemanden kennenlernt,
 * erfaehrt auch nicht alles am ersten Abend.
 *
 * **Der Kalender begrenzt das TEMPO, nicht den Zugang** - und daran haengt, ob das ein Rhythmus
 * ist oder eine Gaengelung:
 *
 * - Das erste Stueck ist sofort da. Auf den Anfang wartet niemand.
 * - Danach kommt je vergangenem Kalendertag eines dazu, und zwar ANGESAMMELT: Wer drei Tage nicht
 *   gefragt hat, bekommt drei. Nichts verfaellt, nichts muss taeglich abgeholt werden - genau das
 *   waere der Belohnungsplan gewesen, gegen den das Argument sich richtete.
 * - Gezaehlt wird ab dem Tag, an dem er zum ersten Mal etwas erzaehlt hat, nicht ab der
 *   Installation: Die Uhr laeuft ab dem Kennenlernen.
 * - Und wer von vorn hoeren will, kann das jederzeit ([forget]) - die freigeschalteten Tage
 *   bleiben freigeschaltet. Zweimal warten muss niemand.
 */
object PlayLore {

    private const val PREFS = "play_lore"

    /** Wieviele Stuecke jedes Wesen zu erzaehlen hat. */
    const val PIECES = 7

    private const val KEY_FIRST_DAY = "first_day_"

    /**
     * Wieviele Stuecke an diesem Tag ueberhaupt zur Verfuegung stehen.
     *
     * Reine Rechnung, damit sich genau die Faelle pruefen lassen, die man sonst nur durch Warten
     * pruefen koennte: der erste Tag, der Tag danach, eine Woche Abwesenheit, ein
     * zurueckgestelltes Geraet.
     *
     * [firstDay] `null` heisst: Er hat noch nie etwas erzaehlt - dann ist genau eines bereit.
     */
    fun unlockedBy(firstDay: LocalDate?, today: LocalDate, pieces: Int = PIECES): Int {
        if (firstDay == null) return 1
        val days = ChronoUnit.DAYS.between(firstDay, today)
        // Ein zurueckgestelltes Geraet darf nichts wegnehmen, was schon freigeschaltet war -
        // deshalb nach unten auf eins begrenzt und nicht auf die gerechnete Zahl.
        return (days + 1).coerceIn(1L, pieces.toLong()).toInt()
    }

    /** Wieviel dieses Wesen HEUTE erzaehlen darf. */
    fun available(
        context: Context,
        species: AvatarSpecies,
        today: LocalDate = LocalDate.now()
    ): Int = unlockedBy(firstDay(context, species), today)

    private fun firstDay(context: Context, species: AvatarSpecies): LocalDate? {
        val stored = prefs(context).getLong(KEY_FIRST_DAY + species.name, 0L)
        return if (stored <= 0L) null else LocalDate.ofEpochDay(stored)
    }

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

    /** Das naechste Stueck - `null`, wenn fuer heute nichts mehr da ist. */
    @StringRes
    fun nextPiece(context: Context, species: AvatarSpecies): Int? {
        val index = heard(context, species)
        if (index >= available(context, species)) return null
        return story(species).getOrNull(index)
    }

    /**
     * Haelt fest, dass ein Stueck erzaehlt wurde - und beim ersten Mal, wann das war.
     *
     * Der Tag des ersten Erzaehlens ist der Anker fuer alles Weitere (siehe [unlockedBy]). Er wird
     * genau einmal gesetzt und danach nie wieder angefasst, auch nicht von [forget]: Sonst wuerde
     * "von vorn hoeren" die Wartezeit zurueckdrehen, und man muesste sich das Freigeschaltete ein
     * zweites Mal verdienen.
     */
    fun remember(
        context: Context,
        species: AvatarSpecies,
        today: LocalDate = LocalDate.now()
    ) {
        val next = (heard(context, species) + 1).coerceAtMost(PIECES)
        val editor = prefs(context).edit().putInt(key(species), next)
        if (firstDay(context, species) == null) {
            editor.putLong(KEY_FIRST_DAY + species.name, today.toEpochDay())
        }
        editor.apply()
    }

    /** Ob er HEUTE noch etwas zu erzaehlen hat. */
    fun hasMore(context: Context, species: AvatarSpecies): Boolean =
        heard(context, species) < available(context, species)

    /**
     * Ob ueberhaupt noch etwas kommt - oder ob die Geschichte zu Ende ist.
     *
     * Der Unterschied zu [hasMore] ist der ganze Sinn des Kalenders und muss im Gespraech
     * ankommen: "heute nichts mehr" ist eine Verabredung fuer morgen, "das war alles" ein
     * Abschluss. Beides mit demselben Satz zu beantworten waere die schlechteste Fassung von
     * beidem.
     */
    fun hasMoreEver(context: Context, species: AvatarSpecies): Boolean =
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
