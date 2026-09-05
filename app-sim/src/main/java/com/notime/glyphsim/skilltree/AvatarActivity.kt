package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayEffects
import com.notime.glyphsim.matrix.PlayRoutine
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep

/**
 * Was der Begleiter gerade tut - und seit wann.
 *
 * **Der Zustand, ohne den es Stufe 3 nicht geben kann.** Ein Blatt wie "Dribbling" ist keine
 * Beschaeftigung, der man nachgehen kann; es ist etwas, das man TUT, WAEHREND man Ball spielt.
 * Ohne ein "gerade laeuft Ballsport" haette eine Einlage nichts, worin sie stattfinden koennte,
 * und der Unterschied zwischen den Ebenen waere nur eine Einrueckung in der Anzeige.
 */
data class AvatarActivity(val nodeId: String, val sinceMillis: Long) {

    /**
     * Ob die Beschaeftigung abgelaufen ist.
     *
     * **Warum sie ueberhaupt ablaeuft:** Ohne Verfall wuerde der Begleiter noch Stunden spaeter
     * als "spielt Ball" gelten, und eine Einlage waere dann eine Antwort auf etwas, das laengst
     * vorbei ist. Wer die App am Morgen kurz benutzt und am Abend wiederkommt, erwartet keinen
     * Anschluss an den Vormittag.
     */
    fun isStale(nowMillis: Long): Boolean =
        nowMillis - sinceMillis >= LIFETIME_MS

    companion object {
        /**
         * Wie lange eine Beschaeftigung nachwirkt.
         *
         * Fuenf Minuten: lang genug, dass mehrere Zuege in einer Sitzung zusammenhaengen, kurz
         * genug, dass nichts ueber eine Pause hinweg stehen bleibt.
         */
        const val LIFETIME_MS = 5L * 60 * 1000
    }
}

/** Ein Schritt in dem, was nach einem Zug aus der Leiste abgespielt wird. */
sealed interface ActivityStep {
    val nodeId: String

    /** Der Begleiter faengt diese Beschaeftigung an - Stufe 1 oder 2. */
    data class Begin(override val nodeId: String) : ActivityStep

    /** Die Einlage in die laufende Beschaeftigung - Stufe 3. */
    data class Flourish(override val nodeId: String) : ActivityStep
}

/**
 * Was abgespielt wird, und was danach laeuft.
 *
 * [steps] ist bewusst eine Liste und kein einzelner Schritt: Wer eine Einlage auf eine unpassende
 * Beschaeftigung zieht, soll den Wechsel UND die Einlage in einem Zug sehen. Als zwei getrennte
 * Aufrufe waere dazwischen die Ruhelage zu sehen, und aus einer Bewegung wuerden zwei.
 */
data class ActivityPlan(
    val steps: List<ActivityStep>,
    /** Die Beschaeftigung, die danach laeuft. */
    val resultingActivity: String
) {
    /** Ob dafuer erst die Beschaeftigung gewechselt werden musste. */
    val isSwitch: Boolean get() = steps.size > 1
}

/**
 * Der kleine Ausschnitt aus der Welt, den eine Absicht fuer ihre konkrete Ausfuehrung braucht.
 *
 * Kein zweiter Weltzustand: [place] kommt direkt aus `DockScreen.currentPlace`, die Freischaltungen
 * aus [AvatarUnlockRepository]. Welche Avatar-Level welche Ablaufvarianten freischalten, ist laut
 * `Tagesablauf.md` weiterhin eine offene Produktentscheidung und wird hier deshalb bewusst NICHT
 * vorweggenommen. Die Ausfuehrung bleibt vollstaendig bei [PlayRoutine] und `DockScreen.runRoutine`.
 */
data class ActivityContext(
    val place: PlayScene.Place,
    val unlockedNodeIds: Set<String>
)

/**
 * Ergebnis der bestehenden Aktivitaetsentscheidung plus die Routine, die sie in der Welt sichtbar
 * macht. [plan] bleibt die semantische Wahrheit (Begin/Flourish); [routine] ist nur ihre vorhandene
 * Ausfuehrungsebene.
 */
data class ResolvedActivity(
    val plan: ActivityPlan,
    val topic: AnimationType,
    val routine: PlayRoutine
)

/**
 * **Die Regeln fuer "was passiert, wenn ich das auf den Begleiter ziehe".**
 *
 * Reine Rechnung ohne Compose, Datenbank und Coroutinen - dadurch laesst sich das Verhalten
 * pruefen, das sich sonst nur durch Zuschauen beurteilen liesse. Und genau hier steckt die
 * Mechanik, um die es bei Stufe 3 geht.
 */
object AvatarActivityPlans {

    /**
     * [current] ist die laufende Beschaeftigung, oder `null`, wenn keine laeuft. Abgelaufene
     * uebergibt der Aufrufer gar nicht erst (siehe [AvatarActivityBus.currentIfFresh]).
     *
     * ## Die drei Faelle
     *
     * - **Eine Beschaeftigung gezogen** (Stufe 1 oder 2): Er faengt sie an. Auch dann, wenn er sie
     *   schon tut - ein zweiter Zug auf dieselbe Sache ist ein Anstupsen, kein Nichts.
     * - **Eine Einlage auf die passende Beschaeftigung**: nur die Einlage. Die Beschaeftigung
     *   laeuft weiter, sie wird nicht neu begonnen - sonst faenge er nach jedem Trick wieder von
     *   vorn an.
     * - **Eine Einlage auf etwas anderes**: erst der Wechsel zur zugehoerigen Beschaeftigung, dann
     *   die Einlage. Wer schlaeft und ein Dribbling bekommt, steht auf, geht spielen und dribbelt
     *   dann - er dribbelt nicht im Bett.
     *
     * **Passend heisst: genau der Elternknoten.** Wer allgemein "Sport" macht und ein Dribbling
     * bekommt, wechselt zu Ballsport - fuer einen Trick braucht es einen Ball, und "Sport" ist
     * noch keiner.
     */
    fun planFor(current: AvatarActivity?, dropped: AnimationNode): ActivityPlan {
        if (dropped.kind == AnimationNode.Kind.ACTIVITY) {
            return ActivityPlan(listOf(ActivityStep.Begin(dropped.id)), dropped.id)
        }

        // Eine Einlage ohne Elternknoten kann es im Baum nicht geben (Stufe 3 haengt immer unter
        // Stufe 2) - falls doch, spielt sie fuer sich, statt dass hier etwas abstuerzt.
        val parent = dropped.parentId
            ?: return ActivityPlan(listOf(ActivityStep.Flourish(dropped.id)), dropped.id)

        return if (current?.nodeId == parent) {
            ActivityPlan(listOf(ActivityStep.Flourish(dropped.id)), parent)
        } else {
            ActivityPlan(
                listOf(ActivityStep.Begin(parent), ActivityStep.Flourish(dropped.id)),
                parent
            )
        }
    }

    /**
     * Ob dieser Knoten schon eine kontextuelle Ausfuehrung in der Welt besitzt.
     *
     * Diese kleine Grenze verhindert, dass derselbe Skill zugleich als alte zufaellige Einlage
     * UND als neue verhaltensveraendernde Routine abgespielt wird. Weitere vertikale Schnitte
     * koennen hier spaeter hinzukommen, ohne [SkillRepertoire] ihre IDs beizubringen.
     */
    fun supportsContextualExecution(node: AnimationNode): Boolean =
        node.id in FOOTBALL_INTENT_NODES ||
            node.id in STRENGTH_INTENT_NODES ||
            node.id in MUSIC_INTENT_NODES

    /**
     * Uebersetzt eine bereits vorhandene Skill-/Reminder-Absicht in eine konkrete vorhandene
     * [PlayRoutine]. Angeschlossen sind bisher drei Familien - Fussball, Kraft & Ausdauer und
     * Musizieren; fuer alle anderen Knoten bleibt die bisherige Reaktion unangetastet, statt hier
     * vorschnell ein Framework zu bauen. Jeder Schnitt kam als eine Verzweigung und ein
     * Schritt-Bauer dazu, nicht als zweite Entscheidungsschicht.
     *
     * Entscheidend: Freischaltungen waehlen NICHT bloss eine zusaetzliche Animation nach der
     * Handlung, sondern veraendern die Handlung selbst. Ohne Dribbling-Knoten gibt es keine
     * Dribbling-Sequenz; ohne Schuss-Knoten weder Zielen noch Schuss. Die neue, kleine
     * `Football(TOUCH)`-Phase am Anfang erweitert dabei nur den vorhandenen Fussball-Renderer um
     * einfachen Ballkontakt; `DRIBBLE` bleibt dadurch ausschliesslich dem freigeschalteten Skill.
     */
    fun resolve(
        current: AvatarActivity?,
        dropped: AnimationNode,
        context: ActivityContext
    ): ResolvedActivity? {
        if (!supportsContextualExecution(dropped)) return null

        val plan = planFor(current, dropped)
        // Zweiter Schnitt, gleiche Weiche: Kraft & Ausdauer laeuft neben dem Fussball her, nicht
        // in einem zweiten System. Alles andere faellt weiterhin auf den bisherigen Reaktionsweg.
        if (plan.resultingActivity == STRENGTH_NODE) return resolveStrength(plan, context)
        // Dritter Schnitt, dieselbe Weiche - und der erste Fall ausserhalb von `sport`. Der
        // Themen-Typ kommt dabei aus dem Baum (CREATIVITY), nicht aus einer Meinung dieser Datei.
        if (plan.resultingActivity == MUSIC_NODE) return resolveMusic(plan, context)
        if (plan.resultingActivity != BALLSPORT_NODE) return null

        val unlocked = context.unlockedNodeIds
        val ballsportLearned = BALLSPORT_NODE in unlocked
        val dribblingLearned = DRIBBLING_NODE in unlocked
        val shotLearned = SHOT_NODE in unlocked

        val localPractice = context.place in LOCAL_SPORT_PLACES
        val targetPlace = if (localPractice) context.place else PlayScene.Place.SPORT
        val anchor = anchorFor(targetPlace)

        // Die gelernten Koennens-Anteile - sie allein genuegen, wenn die Beschaeftigung schon
        // laeuft (siehe [isInsert]).
        val skill = buildList {
            if (dribblingLearned) {
                // Gelerntes Dribbling wird als erkennbare Folge sichtbar. Eine zusaetzliche
                // Level-Schwelle gibt es bewusst nicht: Welche Level Varianten freischalten,
                // bleibt laut Tagesablauf.md eine offene Produktentscheidung.
                add(RoutineStep.Stroll((anchor + 0.16f).coerceAtMost(0.72f)))
                add(RoutineStep.Football(PlayEffects.FootballPhase.DRIBBLE))
                add(RoutineStep.Linger(5_000L))
            }

            if (shotLearned) {
                add(RoutineStep.Football(PlayEffects.FootballPhase.AIM))
                add(RoutineStep.Linger(3_000L))
                add(RoutineStep.Football(PlayEffects.FootballPhase.KICK))
                add(RoutineStep.Linger(5_000L))
            }
        }

        if (isInsert(plan, skill)) return ResolvedActivity(plan, AnimationType.MOVE, PlayRoutine(skill))

        val steps = buildList {
            // Zuhause, im Arbeitszimmer usw. keinen Ball samt Tor in die Kulisse zaubern. Der
            // vorhandene GoToPlace-Schritt zeigt den Weg durch die Tuer; es ist also kein
            // Teleport. Park und Wiese sind dagegen glaubwuerdige lokale Uebungsorte.
            if (!localPractice) {
                add(RoutineStep.GoToPlace(PlayScene.Place.SPORT))
            }
            add(RoutineStep.Stroll(anchor))

            // Basiskontakt: Der Reminder darf auch einen Anfaenger zu einer kurzen Ballberuehrung
            // anregen. Das ist noch NICHT das freigeschaltete Dribbling-Repertoire.
            add(RoutineStep.Football(PlayEffects.FootballPhase.TOUCH))
            add(RoutineStep.Linger(if (ballsportLearned) 4_000L else 2_500L))

            addAll(skill)
        }

        return ResolvedActivity(
            plan = plan,
            topic = AnimationType.MOVE,
            routine = PlayRoutine(steps)
        )
    }

    /**
     * **Zweiter vertikaler Schnitt: Kraft & Ausdauer** - dasselbe Muster wie beim Fussball, mit
     * denselben vorhandenen Bausteinen.
     *
     * Nichts daran ist neu erfunden: [PlayEffects.TrainingPhase] und [RoutineStep.Training] gab es
     * schon (`PlayRoutines` benutzt sie in einer MOVE-Routine), `DockScreen.runRoutine` zeichnet
     * sie bereits. Was gefehlt hat, war die Verbindung zur Absicht - genau die Luecke, die der
     * Fussball-Schnitt fuer sein Gebiet geschlossen hat.
     *
     * **Die Freischaltung veraendert die Handlung, nicht bloss eine Zugabe danach.** `WARM_UP` ist
     * die Basis, die auch ein Anfaenger sehen darf - das Gegenstueck zu `TOUCH` beim Fussball.
     * `LIFT` erscheint ausschliesslich mit tatsaechlich freigeschaltetem `heben`; ohne den Knoten
     * gibt es kein Heben, egal auf welchem Level. Auch hier **keine erfundene Level-Schwelle**:
     * Welche Level Varianten oeffnen, bleibt laut `Tagesablauf.md` offen.
     *
     * **`REST` schliesst immer ab, auch ohne `LIFT`.** Beim Fussball ist `TOUCH` fuer sich schon
     * eine vollstaendige kleine Szene; Aufwaermen ohne Ausklang waere dagegen ein Stumpf - man
     * saehe jemanden anfangen und dann abbrechen. Der Abschluss ist kein Koennen, sondern das
     * Ende einer Einheit, und deshalb an keine Freischaltung geknuepft.
     */
    private fun resolveStrength(plan: ActivityPlan, context: ActivityContext): ResolvedActivity {
        val unlocked = context.unlockedNodeIds
        val strengthLearned = STRENGTH_NODE in unlocked
        val liftLearned = LIFT_NODE in unlocked

        val localPractice = context.place in LOCAL_SPORT_PLACES
        val targetPlace = if (localPractice) context.place else PlayScene.Place.SPORT
        val anchor = anchorFor(targetPlace)

        val skill = buildList {
            if (liftLearned) {
                add(RoutineStep.Training(PlayEffects.TrainingPhase.LIFT))
                add(RoutineStep.Linger(5_000L))
            }
        }

        if (isInsert(plan, skill)) return ResolvedActivity(plan, AnimationType.MOVE, PlayRoutine(skill))

        val steps = buildList {
            // Wie beim Fussball: kein Teleport. Drinnen fuehrt der sichtbare Weg nach draussen.
            if (!localPractice) {
                add(RoutineStep.GoToPlace(PlayScene.Place.SPORT))
            }
            add(RoutineStep.Stroll(anchor))

            add(RoutineStep.Training(PlayEffects.TrainingPhase.WARM_UP))
            add(RoutineStep.Linger(if (strengthLearned) 4_000L else 2_500L))

            addAll(skill)

            add(RoutineStep.Training(PlayEffects.TrainingPhase.REST))
            add(RoutineStep.Linger(3_000L))
        }

        return ResolvedActivity(
            plan = plan,
            // Kraft & Ausdauer haengt wie der Ballsport unter `sport`, und dessen eingebauter Typ
            // ist MOVE. Kein eigener Typ - der Baum sagt es bereits.
            topic = AnimationType.MOVE,
            routine = PlayRoutine(steps)
        )
    }

    /**
     * **Dritter vertikaler Schnitt: Musizieren** - und der erste Fall ausserhalb von `sport`.
     *
     * Damit beantwortet dieser Schnitt die Frage, die die beiden Sport-Faelle offen lassen mussten:
     * Traegt das Muster auch eine Beschaeftigung mit einem anderen Themen-Typ und einem anderen
     * Ortsprofil - oder war es nur zweimal derselbe Fall?
     *
     * **Erneut nichts Neues erfunden.** [PlayEffects.MusicPhase] und [RoutineStep.Music] gab es
     * schon; `PlayRoutines` benutzt sie sogar bereits in einer eigenen CREATIVITY-Routine
     * ("Musizieren im Park"), und `DockScreen.runRoutine` zeichnet den Schritt. Die Phase war also
     * im Alltag laengst verbunden - nur nicht mit einer *Absicht*. Genau diese eine Luecke schliesst
     * der Schnitt, wie zuvor bei Fussball und Kraft.
     *
     * **Der Themen-Typ kommt aus dem Baum, nicht von hier.** `kreativ` ist als
     * [AnimationType.CREATIVITY] angelegt; die beiden Sport-Familien liefern MOVE, weil `sport` so
     * angelegt ist. Dass diese Datei den Typ nirgends selbst waehlt, ist der eigentliche Beleg,
     * dass hier kein zweites Regelwerk neben dem Baum entsteht.
     *
     * **Eine Freischaltung, mehr behauptet der Schnitt nicht.** `TUNE` und `PLAY` bilden zusammen
     * die Basis, die auch ohne jeden Knoten vollstaendig ist: Er stimmt und er spielt. `FINALE`
     * erscheint ausschliesslich mit tatsaechlich freigeschaltetem `singen` - auf keinem Level und
     * bei keiner Spezies sonst. Keine erfundene Level-Schwelle; welche Level Varianten oeffnen,
     * bleibt laut `Tagesablauf.md` offen.
     *
     * **Warum `PLAY` zur Basis gehoert und nicht zur Freischaltung.** Beim Fussball ist `TOUCH`
     * fuer sich eine ganze kleine Szene, beim Training braucht `WARM_UP` den Ausklang `REST`.
     * Stimmen allein waere hier dasselbe Stueck Stumpf: Man saehe jemanden ein Instrument richten
     * und dann aufhoeren. Das Koennen liegt im Abschluss, den `FINALE` mit fuenf statt drei Noten
     * sichtbar macht - nicht darin, ueberhaupt einen Ton zu spielen.
     *
     * **`drum` und `bolt` bleiben bewusst aussen vor.** Beide haengen unter `kreativ/musik`, aber
     * die vorhandene Choreografie kennt keine Phase, die sie voneinander unterscheiden koennte.
     * Sie als Absicht zu fuehren und dann dasselbe zu zeigen wie `singen` waere eine Behauptung
     * ohne Deckung; sie behalten deshalb den bisherigen Reaktionsweg - genau wie `basketball` und
     * `trophy` beim Fussball-Schnitt.
     */
    private fun resolveMusic(plan: ActivityPlan, context: ActivityContext): ResolvedActivity {
        val unlocked = context.unlockedNodeIds
        val musicLearned = MUSIC_NODE in unlocked
        val singLearned = SING_NODE in unlocked

        val localStage = context.place in LOCAL_MUSIC_PLACES
        val targetPlace = if (localStage) context.place else PlayScene.Place.PARK
        val anchor = anchorFor(targetPlace)

        val skill = buildList {
            if (singLearned) {
                add(RoutineStep.Music(PlayEffects.MusicPhase.FINALE))
                add(RoutineStep.Linger(4_000L))
            }
        }

        if (isInsert(plan, skill)) {
            return ResolvedActivity(plan, AnimationType.CREATIVITY, PlayRoutine(skill))
        }

        val steps = buildList {
            // Anders als beim Sport ist der Ortswechsel hier die Ausnahme: Ein Instrument braucht
            // kein Feld, nur Platz zum Sitzen. Wohnzimmer und Leseecke taugen dafuer so gut wie
            // Park und Wiese. Wo es dagegen laut, nass oder eng ist, fuehrt der sichtbare Weg in
            // den Park - kein Teleport, derselbe vorhandene Schritt wie in beiden Sport-Faellen.
            if (!localStage) {
                add(RoutineStep.GoToPlace(PlayScene.Place.PARK))
            }
            add(RoutineStep.Stroll(anchor))

            add(RoutineStep.Music(PlayEffects.MusicPhase.TUNE))
            add(RoutineStep.Linger(if (musicLearned) 3_000L else 2_000L))

            add(RoutineStep.Music(PlayEffects.MusicPhase.PLAY))
            add(RoutineStep.Linger(if (musicLearned) 6_000L else 4_000L))

            addAll(skill)
        }

        return ResolvedActivity(
            plan = plan,
            // Aus dem Baum: `kreativ` ist als CREATIVITY angelegt. Diese Datei waehlt den Typ
            // nirgends selbst - sie liest ihn nur ab.
            topic = AnimationType.CREATIVITY,
            routine = PlayRoutine(steps)
        )
    }

    /**
     * **Eine Einlage in eine bereits LAUFENDE Beschaeftigung bekommt keinen neuen Anlauf.**
     *
     * [planFor] beantwortet das bereits: Laeuft der Wirt schon, enthaelt der Plan nur einen
     * `Flourish` und keinen `Begin` - und seine Doku sagt auch warum ("sonst faenge er nach jedem
     * Trick wieder von vorn an"). Die Ausfuehrung hat diese Aussage bisher ignoriert und in jedem
     * Fall Hinweg, Anker und Basisphase vorangestellt. Wer beim Ballspielen ein Dribbling bekam,
     * sah ihn also erneut hinlaufen und den Ball erstmals beruehren.
     *
     * Codex hat das an der Kraft-Familie gemeldet; der Fussball-Schnitt trug denselben Fall von
     * Anfang an. Deshalb liegt die Regel hier gemeinsam und nicht zweimal: Zwei Familien, die
     * sich an derselben Stelle verschieden verhalten, waeren schlechter als eine Ungenauigkeit,
     * die man ueberall sieht.
     *
     * [skill] muss dabei nicht leer sein: Kommt ein Impuls fuer eine Faehigkeit, die gar nicht
     * freigeschaltet ist, gaebe eine reine Einlage eine Routine ohne einen einzigen Schritt. Dann
     * ist der volle Ablauf richtig - er zeigt wenigstens die Basis.
     */
    private fun isInsert(plan: ActivityPlan, skill: List<RoutineStep>): Boolean =
        skill.isNotEmpty() && plan.steps.none { it is ActivityStep.Begin }

    /**
     * Wo auf der Breite des Bildes das Ganze stattfindet - je Ort einmal, fuer ALLE Familien.
     *
     * Hiess bis zum dritten Schnitt "fuer beide Familien". An den Werten aendert sich nichts:
     * Sie beschreiben den Ort, nicht die Taetigkeit, und ein Park ist zum Musizieren so breit wie
     * zum Kicken. Eine eigene Anker-Tabelle je Familie waere eine Kopie, die auseinanderlaeuft.
     */
    private fun anchorFor(place: PlayScene.Place): Float = when (place) {
        PlayScene.Place.SPORT -> 0.30f
        PlayScene.Place.PARK -> 0.42f
        PlayScene.Place.MEADOW -> 0.46f
        else -> 0.34f
    }

    /**
     * Die Beschaeftigung, in der eine Einlage stattfindet - fuer Anzeige und Pruefung.
     *
     * `null` fuer alles, was selbst eine Beschaeftigung ist.
     */
    fun hostActivityOf(node: AnimationNode): AnimationNode? =
        if (node.kind == AnimationNode.Kind.FLOURISH) {
            node.parentId?.let { AnimationTree.node(it) }
        } else {
            null
        }

    private const val BALLSPORT_NODE = "sport/ballsport"
    private const val DRIBBLING_NODE = "sport/ballsport/dribbling"
    private const val SHOT_NODE = "sport/ballsport/schuss"

    private const val STRENGTH_NODE = "sport/kraft-ausdauer"
    private const val LIFT_NODE = "sport/kraft-ausdauer/heben"

    private const val MUSIC_NODE = "kreativ/musik"
    private const val SING_NODE = "kreativ/musik/singen"

    private val FOOTBALL_INTENT_NODES = setOf(BALLSPORT_NODE, DRIBBLING_NODE, SHOT_NODE)
    private val STRENGTH_INTENT_NODES = setOf(STRENGTH_NODE, LIFT_NODE)

    /**
     * `drum` und `bolt` haengen ebenfalls unter `kreativ/musik`, fehlen hier aber absichtlich -
     * siehe [resolveMusic]: Ohne unterscheidbare Phase waere ihre Aufnahme eine Behauptung ohne
     * Deckung.
     */
    private val MUSIC_INTENT_NODES = setOf(MUSIC_NODE, SING_NODE)

    /**
     * Orte, an denen Sport ohne Ortswechsel glaubwuerdig ist - fuer BEIDE Familien dieselben.
     *
     * Hiess bis zum zweiten Schnitt `LOCAL_FOOTBALL_PLACES`. Die Menge hat sich nicht geaendert,
     * nur ihr Geltungsbereich: Wiese und Park taugen fuers Ballspielen genauso wie fuer ein paar
     * Uebungen. Eine zweite, identische Menge daneben waere eine Kopie, die frueher oder spaeter
     * auseinanderlaeuft.
     */
    private val LOCAL_SPORT_PLACES = setOf(
        PlayScene.Place.SPORT,
        PlayScene.Place.PARK,
        PlayScene.Place.MEADOW
    )

    /**
     * Orte, an denen Musizieren ohne Ortswechsel glaubwuerdig ist.
     *
     * **Eine bewusste Entscheidung, keine abgeleitete Tatsache.** Beim Sport zwingt die Sache
     * selbst nach draussen - ein Ball und ein Tor gehoeren nicht ins Wohnzimmer. Ein Instrument
     * braucht dagegen nur Platz zum Sitzen, und die Choreografie zeichnet Gitarre, Noten und
     * Buehnenlinie neben dem Avatar statt in die Kulisse. Deshalb kommen Wohnzimmer und Leseecke
     * zu Park und Wiese dazu.
     *
     * Absichtlich NICHT enthalten: das Schlafzimmer (nachts kein Konzert), Bad, Kueche, Werkstatt
     * und alles Oeffentliche. Dort fuehrt der sichtbare Weg in den Park.
     */
    private val LOCAL_MUSIC_PLACES = setOf(
        PlayScene.Place.PARK,
        PlayScene.Place.MEADOW,
        PlayScene.Place.LIVING,
        PlayScene.Place.NOOK
    )
}
