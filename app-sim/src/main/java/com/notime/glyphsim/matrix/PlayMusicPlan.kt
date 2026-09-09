package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType

/**
 * **Welche Musik zur Lage passen WUERDE** - und ausdruecklich nicht, ob ueberhaupt welche laeuft.
 *
 * ## Die Trennung, um die es hier geht
 *
 * > Die Welt entscheidet, WAS passen wuerde. Der Nutzer entscheidet, OB ueberhaupt Musik laufen
 * > darf.
 *
 * Diese Datei ist vollstaendig die erste Haelfte davon. Sie kennt weder die Einstellung des
 * Nutzers noch einen Player, noch ueberhaupt Android - sie beantwortet nur die Frage "was waere
 * hier richtig?". Die zweite Haelfte liegt in [com.notime.glyphsim.ui.PlayMusic], und dort steht
 * auch die Sperre: Ist Musik ausgeschaltet, wird hier gar nicht erst gefragt.
 *
 * Der Grund fuer die Trennung ist nicht Ordnungsliebe. Eine Auswahl, die zugleich weiss, ob sie
 * spielen darf, verleitet dazu, beim naechsten Sonderfall "dann schalten wir es eben ein" zu
 * schreiben - und genau das darf nie passieren.
 */

/**
 * Wofuer ein Stueck gedacht ist - dieselbe Bezeichnung wie im Feld `role` in
 * `music/manifest.json`.
 *
 * **Rollen statt Dateinamen** ueberall dort, wo entschieden wird: Ein Ort und eine Tageszeit
 * ergeben eine ROLLE, und erst ganz am Ende wird daraus eine Datei. Sonst haenge die
 * Weltlogik an Dateinamen, und ein zweiter Abendtrack braeuchte eine Aenderung an jeder
 * Entscheidungsstelle statt an einer.
 *
 * **Eine Rolle kann MEHRERE Stuecke haben.** Bis zur Variantenfaehigkeit galt "eine Rolle
 * entspricht genau einer Datei"; wer nachmittags zu Hause sass, hoerte deshalb denselben
 * Neunzig-Sekunden-Loop, solange er zusah. Die Rolle war dabei richtig - es fehlte die
 * Moeglichkeit, sie mit mehr als einem Stueck zu erfuellen. Deshalb steht hier jetzt der
 * gemeinsame RESSOURCEN-STAMM, und die einzelnen Dateien haengen eine zweistellige Nummer daran
 * ([variantResource]). Welche davon laeuft, entscheidet [PlayMusicRotation] - nach der Rolle,
 * nicht statt ihrer.
 *
 * [androidResource] bleibt als erste Variante erhalten und muss mit `android_resource` desselben
 * Tracks im Manifest uebereinstimmen - `tools/music/generate_music.py` prueft beim Erzeugen, dass
 * die Rolle hier bekannt ist.
 */
enum class MusicRole(val manifestName: String, val resourceBase: String) {

    /**
     * Der Grundcharakter des normalen Tages - die musikalische Identitaet von Itoeva.
     *
     * Seit PR #89 als `Lantern Streets` ausgeliefert. Die Rolle bleibt vom Dateinamen getrennt,
     * damit die Welt weiterhin Bedeutung statt Asset-Namen auswaehlt.
     */
    MAIN_DAY("main_day_background", "itoeva_main_day"),

    /** Ruhige Abend- und Nachtstunden zu Hause und an stillen Naturorten. */
    HOME_EVENING("home_evening_background", "itoeva_home_evening"),

    /** Der frueh Morgen, falls er sich spaeter vom uebrigen Tag abheben soll. */
    MORNING("morning_background", "itoeva_morning"),

    /** Bewegung und Anstrengung - energischer als der normale Tag. */
    SPORT("sport_background", "itoeva_sport"),

    /** Traum-Szenen. Bewusst schon benannt, damit sie spaeter keine Sonderregel brauchen. */
    DREAM("dream_background", "itoeva_dream"),

    /**
     * Das persoenliche Musikstueck des anwesenden Wesens - sein eigenes Thema, nicht eine
     * weitere Hintergrundschleife. Anders als jede andere Rolle hier rotiert die Variante nicht
     * frei zwischen mehreren Stuecken derselben Stimmung: Variante 1 bis 6 gehoeren fest je
     * einer Spezies, siehe [characterThemeVariant].
     *
     * **Wann** es erklingt, steht in [PlayCharacterTheme]: beim ersten Erscheinen des Wesens an
     * einem Kalendertag, ein Stueck lang - und ausdruecklich nicht als Dauerschleife anstelle
     * des Tagesklangs.
     *
     * Die sechs Stuecke selbst sind noch nicht ausgeliefert (siehe ITO-0017 bis ITO-0022 in
     * `evolutions/BACKLOG.md`). Bis dahin greift die gewoehnliche Rangfolge: Fehlt das Stueck,
     * klingt der Tag wie sonst.
     */
    CHARACTER_THEME("character_theme_background", "itoeva_theme");

    /**
     * Der Ressourcenname der [variant]-ten Datei dieser Rolle, eins-basiert.
     *
     * Die zweistellige Nummer ist kein Schmuck: Sie haelt die Dateien im Verzeichnis in derselben
     * Reihenfolge wie im Manifest, und sie macht aus einer zehnten Variante keinen Namen, der
     * zwischen der ersten und der zweiten einsortiert wird.
     */
    fun variantResource(variant: Int): String = "%s_%02d".format(resourceBase, variant)

    /**
     * Die erste Variante - der Name, unter dem diese Rolle vor der Variantenfaehigkeit
     * ausgeliefert wurde. Bleibt bestehen, damit Manifest und bereits gemergte Dateien
     * unveraendert gueltig sind.
     */
    val androidResource: String get() = variantResource(1)

    companion object {
        /**
         * Wie viele Varianten je Rolle hoechstens gesucht werden.
         *
         * Eine Obergrenze braucht es, weil die Suche zur Laufzeit ueber `getIdentifier` laeuft und
         * sonst kein Ende haette. Acht ist grosszuegig gegenueber allem, was absehbar erzeugt
         * wird, und billig: Die Suche laeuft einmal je Abgleich, nicht je Bild.
         */
        const val MAX_VARIANTS = 8

        fun byManifestName(name: String): MusicRole? = entries.firstOrNull { it.manifestName == name }

        /**
         * Die [CHARACTER_THEME]-Variante von [species] - eins-basiert nach der
         * Deklarationsreihenfolge in [AvatarSpecies], nicht nach einer separat gepflegten
         * Zahlentabelle.
         *
         * Auf [AvatarSpecies.entries]`.indexOf` gestuetzt, damit eine spaeter angehaengte
         * siebte Spezies automatisch eine neue, noch unbelegte Variante bekommt - statt
         * stillschweigend das Thema eines bestehenden Wesens zu uebernehmen.
         */
        fun characterThemeVariant(species: AvatarSpecies): Int = AvatarSpecies.entries.indexOf(species) + 1
    }
}

/**
 * Der Ausschnitt aus der Welt, den die Musikauswahl braucht.
 *
 * Bewusst klein und bewusst kein zweiter Weltzustand: [dayPhase] kommt aus
 * [PlayAmbientActivity.currentDayPhase], [place] aus `DockScreen.currentPlace` und [topic] aus der
 * aktuell laufenden Handlung. Weitere Signale - etwa Wetter oder Stimmung - kommen erst als
 * zusaetzliches Feld dazu, wenn sie eine hoerbare Entscheidung tragen koennen.
 */
data class MusicContext(
    val dayPhase: PlayAmbientActivity.DayPhase,
    val place: PlayScene.Place,
    /**
     * Was die Figur gerade wirklich tut. Der Ort allein reicht dafuer nicht: Auf dem Sportplatz
     * kann sie auch nur ankommen oder warten. Ein spezifischer Aktivitaets-Track darf erst dann
     * uebernehmen, wenn Ort UND Beschaeftigung dieselbe Geschichte erzaehlen.
     */
    val topic: AnimationType? = null,
    /**
     * Das Wesen, dessen eigenes Stueck gerade an der Reihe ist - im Normalfall `null`.
     *
     * Nicht "welches Wesen anwesend ist": Das ist es immer, und ein daran haengendes Stueck waere
     * eine Dauerschleife. Gesetzt ist dieses Feld nur, solange der Anlass laeuft, den
     * [PlayCharacterTheme] beschreibt.
     */
    val characterTheme: AvatarSpecies? = null
)

/**
 * **Sparsam und hierarchisch, nicht als vollstaendige Matrix.**
 *
 * Vier Tageszeiten mal sechzehn Orte waeren vierundsechzig Felder, von denen die allermeisten
 * dasselbe enthielten - und jeder neue Ort verlangte vier neue Entscheidungen. Stattdessen
 * liefert [candidates] je Lage eine kurze Liste von Rollen, die **vom Spezifischsten zum
 * Allgemeinsten** geordnet ist, und [resolve] nimmt die erste, die es tatsaechlich gibt. Fehlt
 * alles, bleibt es still.
 *
 * Dadurch waechst das Ganze durch Hinzufuegen: Ein neuer Sport-Track wird gehoert, sobald er
 * gemergt ist, ohne dass hier eine Zeile geaendert werden muss.
 */
object MusicResolver {

    /**
     * Orte, an denen ein Abend leise ist. Drinnen plus die stillen Aussenorte - der Sportplatz,
     * die Stadt und die Strasse stehen bewusst nicht hier.
     */
    private val QUIET_PLACES = setOf(
        PlayScene.Place.BEDROOM,
        PlayScene.Place.BATH,
        PlayScene.Place.NOOK,
        PlayScene.Place.LIVING,
        PlayScene.Place.KITCHEN,
        PlayScene.Place.DESK,
        PlayScene.Place.CRAFT,
        PlayScene.Place.POND,
        PlayScene.Place.FOREST,
        PlayScene.Place.MEADOW,
        PlayScene.Place.PARK
    )

    /**
     * Die Rollen, die hier in Frage kommen - **vom Spezifischsten zum Allgemeinsten**.
     *
     * Oeffentlich, weil sich die Absicht sonst nur ueber das Ergebnis pruefen liesse: Ein Test
     * kann so festhalten, dass am Sportplatz der Sport-Track VOR dem Tages-Track steht, auch
     * wenn es beide heute noch gar nicht gibt.
     */
    fun candidates(context: MusicContext): List<MusicRole> = buildList {
        // **Ganz vorn und ohne Ruecksicht auf Tageszeit und Ort** - aber nur, solange der Anlass
        // laeuft (siehe [PlayCharacterTheme]). Das Stueck beschreibt das Wesen und nicht die
        // Lage; es waere die falsche Reihenfolge, ein Wiedersehen von der Uhrzeit abhaengig zu
        // machen. Die uebrigen Kandidaten bleiben trotzdem stehen: Fehlt das Stueck dieses
        // Wesens noch, klingt der Tag wie sonst statt still zu werden.
        if (context.characterTheme != null) add(MusicRole.CHARACTER_THEME)

        // Eine echte Sporthandlung schlaegt die Tageszeit, solange es nicht Nacht ist. Der Ort
        // allein genuegt absichtlich nicht: Ein kurzer Weg oder eine Pause am Sportplatz soll
        // spaeter keinen energischen Track starten und gleich wieder abbrechen.
        if (context.place == PlayScene.Place.SPORT &&
            context.topic == AnimationType.MOVE &&
            context.dayPhase != PlayAmbientActivity.DayPhase.NIGHT
        ) {
            add(MusicRole.SPORT)
        }

        when (context.dayPhase) {
            // Nachts gibt es keinen Rueckfall auf den Tages-Track. Lieber still als munter.
            PlayAmbientActivity.DayPhase.NIGHT -> add(MusicRole.HOME_EVENING)

            PlayAmbientActivity.DayPhase.EVENING ->
                if (context.place in QUIET_PLACES) {
                    add(MusicRole.HOME_EVENING)
                    add(MusicRole.MAIN_DAY)
                } else {
                    // Abends noch unterwegs: Der Tag klingt nach, der Abendtrack ist der Rueckfall.
                    add(MusicRole.MAIN_DAY)
                    add(MusicRole.HOME_EVENING)
                }

            PlayAmbientActivity.DayPhase.MORNING -> {
                add(MusicRole.MORNING)
                add(MusicRole.MAIN_DAY)
            }

            PlayAmbientActivity.DayPhase.MIDDAY -> add(MusicRole.MAIN_DAY)
        }
    }

    /**
     * Die erste Rolle aus [candidates], zu der es tatsaechlich einen ausgelieferten Track gibt -
     * oder `null` fuer Stille.
     *
     * [available] beantwortet der Aufrufer, weil nur er weiss, was im Paket liegt. Fehlt eine
     * spezifische Rolle, greift die oben dokumentierte Rangfolge; fehlt auch ihr Rueckfall,
     * bleibt die Welt still statt einen unpassenden Track zu behaupten.
     */
    fun resolve(context: MusicContext, available: Set<MusicRole>): MusicRole? =
        candidates(context).firstOrNull { it in available }
}
