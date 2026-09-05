package com.notime.glyphsim.matrix

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
 * [androidResource] muss mit `android_resource` desselben Tracks im Manifest uebereinstimmen -
 * `tools/music/generate_music.py` prueft beim Erzeugen, dass die Rolle hier bekannt ist.
 */
enum class MusicRole(val manifestName: String, val androidResource: String) {

    /**
     * Der Grundcharakter des normalen Tages - die musikalische Identitaet von Itoeva.
     *
     * **Noch nicht erzeugt.** Solange kein Track mit dieser Rolle gemergt ist, faellt alles,
     * was hier landen wuerde, auf Stille zurueck (siehe [MusicResolver]). Das ist Absicht: Ein
     * Abendstueck den ganzen Tag zu spielen waere schlechter als nichts.
     */
    MAIN_DAY("main_day_background", "itoeva_main_day_01"),

    /** Ruhige Abend- und Nachtstunden zu Hause. Heute der einzige vorhandene Track. */
    HOME_EVENING("home_evening_background", "itoeva_home_evening_01"),

    /** Der frueh Morgen, falls er sich spaeter vom uebrigen Tag abheben soll. */
    MORNING("morning_background", "itoeva_morning_01"),

    /** Bewegung und Anstrengung - energischer als der normale Tag. */
    SPORT("sport_background", "itoeva_sport_01"),

    /** Traum-Szenen. Bewusst schon benannt, damit sie spaeter keine Sonderregel brauchen. */
    DREAM("dream_background", "itoeva_dream_01");

    companion object {
        fun byManifestName(name: String): MusicRole? = entries.firstOrNull { it.manifestName == name }
    }
}

/**
 * Der Ausschnitt aus der Welt, den die Musikauswahl braucht.
 *
 * Bewusst klein und bewusst kein zweiter Weltzustand: [dayPhase] kommt aus
 * [PlayAmbientActivity.currentDayPhase], [place] aus `DockScreen.currentPlace`. Weitere Signale -
 * eine laufende Sportroutine, ein besonderer Weltzustand - kommen als zusaetzliches Feld dazu,
 * wenn sie gebraucht werden, und nicht vorher.
 */
data class MusicContext(
    val dayPhase: PlayAmbientActivity.DayPhase,
    val place: PlayScene.Place
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
        PlayScene.Place.POND
    )

    /**
     * Die Rollen, die hier in Frage kommen - **vom Spezifischsten zum Allgemeinsten**.
     *
     * Oeffentlich, weil sich die Absicht sonst nur ueber das Ergebnis pruefen liesse: Ein Test
     * kann so festhalten, dass am Sportplatz der Sport-Track VOR dem Tages-Track steht, auch
     * wenn es beide heute noch gar nicht gibt.
     */
    fun candidates(context: MusicContext): List<MusicRole> = buildList {
        // Ort schlaegt Tageszeit, solange es nicht Nacht ist: Wer nachts am Sportplatz steht,
        // trainiert nicht, sondern geht nach Hause.
        if (context.place == PlayScene.Place.SPORT &&
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
     * [available] beantwortet der Aufrufer, weil nur er weiss, was im Paket liegt. Heute ist das
     * genau [MusicRole.HOME_EVENING]; Morgen und Mittag ergeben deshalb **Stille**, und das ist
     * die richtige Antwort, bis der Tages-Track existiert.
     */
    fun resolve(context: MusicContext, available: Set<MusicRole>): MusicRole? =
        candidates(context).firstOrNull { it in available }
}
