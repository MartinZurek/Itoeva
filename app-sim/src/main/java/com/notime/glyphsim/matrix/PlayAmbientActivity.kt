package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import java.time.LocalTime
import kotlin.random.Random

/**
 * Kleine autonome Regungen des Avatars im Play-Modus-Dock, wenn gerade KEINE echte Erinnerung
 * offen ist (siehe ui/DockScreen.kt) - der Baustein, der aus einem idlenden Sprite einen an die
 * Uhrzeit gekoppelten Tagesablauf macht (Aufstehen/Bewegen, Trinken, Fokus, soziale Kontakte,
 * Schlafen, ... - alles ueber den Tag verteilt animiert).
 *
 * **Warum das ueberhaupt noetig ist.** Der Avatar ist im Play-Modus dauerhaft sichtbar (nicht
 * mehr nur, solange eine Erinnerungs-Animation laeuft) - zwischen zwei echten Ausloesungen liegen
 * je nach Spezies/Level aber mehrere Minuten (siehe PlayGamePlan), in denen er sonst nur
 * unveraendert idle daesaesse. Diese Klasse fuellt genau diese Luecke.
 *
 * **Zweite Ausbaustufe: Tageszeit-Gewichtung.** [nextTopic] wuerfelt nicht mehr gleichverteilt
 * unter den zwoelf [AnimationType]s, sondern nach [DayPhase] - morgens eher Bewegung/Trinken,
 * mittags eher Fokus/Arbeit, abends eher Ruhe/Naehe, nachts fast nur Schlaf. Dieselben zwoelf
 * Themen wie im echten Erinnerungs-Spiel (siehe PlayModeRoll/PlayGamePlan), keine neuen Typen
 * noetig - "Aufstehen" ist MOVE/GENERAL, "Essen" ist DRINK, "soziale Kontakte" ist LOVE. Die
 * eigentliche Animation kommt unveraendert aus [AvatarAnimations.reactionFor] - dieselbe
 * Bibliothek, die auch eine echte gefuetterte Erinnerung zeigt. MEDICINE bleibt wie dort
 * ausgeschlossen: eine autonome Regung darf nie wie eine echte Medikamenten-Erinnerung wirken.
 *
 * **Dritte Ausbaustufe: echte Gewohnheiten als zweites Signal.** [nextTopic] nimmt zusaetzlich
 * [boostedTopics] entgegen - Themen, bei denen der Nutzer heute eine ECHTE Erinnerung mit
 * Tagesziel noch nicht erreicht hat (siehe [com.notime.glyphsim.ui.PlayHabitSignal]). Der Avatar
 * greift dadurch von sich aus haeufiger zu einem Thema, das noch offen ist, und seltener zu
 * einem, das schon erledigt ist - "Fuettern" wirkt sich damit nicht nur auf die Stimmung aus
 * (siehe [AvatarMood]), sondern sichtbar auf das, was der Avatar aus eigenem Antrieb tut.
 * Additiv statt ersetzend mit der Tagesphase kombiniert (siehe [combinedWeights]) - ein offenes
 * Thema kommt haeufiger vor, verdraengt aber nicht komplett, was gerade zur Uhrzeit passt.
 *
 * **Bewusst noch OHNE Spezies-Charakter in der Gewichtung.** [PlayGamePlan] gewichtet Themen
 * zusaetzlich nach Avatar-Charakter (Hootlet liest gern, Wyrmling bewegt sich gern) - diese
 * Klasse hier noch nicht. Dritte moegliche Signalquelle fuer eine spaetere Ausbaustufe, nach
 * demselben additiven Muster wie [boostedTopics] anzuschliessen, ohne die anderen beiden
 * anzufassen.
 */
object PlayAmbientActivity {

    /**
     * FLOURISH: kurze artspezifische Freuden-Reaktion an Ort und Stelle (siehe
     * [AvatarAnimations.tapReaction]) - ein reiner Gruss ohne Bezug zu einem Thema. WANDER: der
     * Avatar sucht sich eine neue Stelle auf dem Bildschirm. PERFORM: die eigentliche
     * Tagesablauf-Szene - eine themenbezogene Reaktion (siehe [nextTopic]/
     * [AvatarAnimations.reactionFor]), so als haette der Avatar sich gerade selbst um etwas
     * gekuemmert. Ueberwiegend PERFORM, damit der Tagesablauf auch tatsaechlich zu sehen ist -
     * FLOURISH/WANDER bleiben als leichtere Auflockerung dazwischen.
     */
    enum class Action { FLOURISH, WANDER, PERFORM, FIDGET }

    fun nextAction(random: Random = Random): Action {
        val roll = random.nextInt(ACTION_WEIGHT_TOTAL)
        var remaining = roll
        remaining -= ACTION_WEIGHT_PERFORM
        if (remaining < 0) return Action.PERFORM
        remaining -= ACTION_WEIGHT_FIDGET
        if (remaining < 0) return Action.FIDGET
        remaining -= ACTION_WEIGHT_WANDER
        if (remaining < 0) return Action.WANDER
        return Action.FLOURISH
    }

    /**
     * Welche Alltagsregung als naechstes kommt - nach Tageszeit gewichtet, wie schon die Themen.
     *
     * Ein Gaehnen mittags um zwei ist nicht falsch, aber morgens beim Wachwerden und spaet abends
     * ist es RICHTIG - und genau solche kleinen Treffer sind es, die den Eindruck erzeugen, dass
     * da jemand einen Tag durchlebt statt Posen abzuspielen.
     */
    fun nextFidget(phase: DayPhase = currentDayPhase(), random: Random = Random): AvatarAnimations.Fidget {
        val weights = when (phase) {
            // Wachwerden: viel Strecken und Gaehnen.
            DayPhase.MORNING -> mapOf(
                AvatarAnimations.Fidget.STRETCH to 4,
                AvatarAnimations.Fidget.YAWN to 3,
                AvatarAnimations.Fidget.LOOK_AROUND to 2,
                AvatarAnimations.Fidget.SHAKE to 1
            )
            // Wach und aufmerksam.
            DayPhase.MIDDAY -> mapOf(
                AvatarAnimations.Fidget.LOOK_AROUND to 4,
                AvatarAnimations.Fidget.SHAKE to 3,
                AvatarAnimations.Fidget.STRETCH to 2,
                AvatarAnimations.Fidget.YAWN to 1
            )
            DayPhase.EVENING -> mapOf(
                AvatarAnimations.Fidget.YAWN to 3,
                AvatarAnimations.Fidget.STRETCH to 3,
                AvatarAnimations.Fidget.LOOK_AROUND to 2,
                AvatarAnimations.Fidget.SHAKE to 1
            )
            // Nachts fast nur noch muede Regungen.
            DayPhase.NIGHT -> mapOf(
                AvatarAnimations.Fidget.YAWN to 5,
                AvatarAnimations.Fidget.STRETCH to 2,
                AvatarAnimations.Fidget.LOOK_AROUND to 1
            )
        }
        val total = weights.values.sum()
        var roll = random.nextInt(total)
        for ((fidget, weight) in weights) {
            roll -= weight
            if (roll < 0) return fidget
        }
        return AvatarAnimations.Fidget.LOOK_AROUND
    }

    /** Grobe Tagesphase, aus der aktuellen Uhrzeit abgeleitet - kein eigener Zustand, wird bei
     *  jeder Regung frisch bestimmt, damit ein laufender Play-Modus die Grenze automatisch
     *  ueberschreitet, ohne dass irgendwo ein Timer dafuer noetig waere. */
    enum class DayPhase { MORNING, MIDDAY, EVENING, NIGHT }

    fun currentDayPhase(now: LocalTime = PlayTimeLapse.now()): DayPhase = when (now.hour) {
        in 6..10 -> DayPhase.MORNING
        in 11..17 -> DayPhase.MIDDAY
        in 18..22 -> DayPhase.EVENING
        else -> DayPhase.NIGHT
    }

    /**
     * Themenwahl fuer eine PERFORM-Regung: [phase] (Default: die aktuelle Uhrzeit) bestimmt die
     * Grundgewichtung, [boostedTopics] (Default: keine, siehe [com.notime.glyphsim.ui.PlayHabitSignal])
     * hebt heute noch offene echte Gewohnheiten zusaetzlich an.
     */
    fun nextTopic(
        phase: DayPhase = currentDayPhase(),
        boostedTopics: Set<AnimationType> = emptySet(),
        /**
         * Wo die Figur GERADE ist - Themen, die an diesen Ort gehoeren, werden bevorzugt.
         *
         * **Gemeldet als "er rennt immer nur von einem Raum in den naechsten".** Und so war es:
         * Jede Regung wuerfelte ein Thema, jedes Thema hat seinen Ort, und zwischen zwei Regungen
         * liegen wenige Sekunden - also wechselte die Figur im Schnitt alle zehn Sekunden das
         * Zimmer. Sie war damit fast nur unterwegs; angekommen war sie nie. Ausgerechnet die
         * Kulissen, an denen die meiste Arbeit haengt, sah man dadurch immer nur im
         * Vorbeigehen.
         *
         * Der Zuschlag macht daraus ein Verweilen: Wer in der Kueche steht, trinkt dort eher noch
         * etwas, als sofort ins Bad zu gehen. Bewusst nur ein Zuschlag und keine Sperre - ein
         * Wesen, das den Raum erst wechselt, wenn der Wuerfel es erzwingt, waere genauso falsch,
         * nur andersherum.
         */
        stayAt: PlayScene.Place? = null,
        /**
         * Die NEIGUNG des Wesens - die Themen seines Entwicklungspfades (siehe
         * [com.notime.glyphsim.ui.PlayPath]).
         *
         * **Das dritte Signal, und bewusst das schwaechste.** Eine offene Gewohnheit ist eine
         * Aufgabe von heute; der Pfad ist eine Neigung, die ueber Wochen entstanden ist. Deshalb
         * zaehlt er nur halb so viel: Er soll sich in der Haeufigkeit bemerkbar machen, ohne den
         * Tagesablauf zu uebernehmen. Ein Wesen, das ausschliesslich tut, wozu es neigt, waere
         * keine Figur mehr, sondern eine Einstellung.
         */
        leaning: Set<AnimationType> = emptySet(),
        random: Random = Random
    ): AnimationType = pickWeighted(combinedWeights(phase, boostedTopics, stayAt, leaning), random)

    /**
     * Grob am ueblichen Tagesrhythmus orientiert, nicht an einer festen Uhrzeit-Tabelle mit
     * Minuten-Genauigkeit - das waere fuer eine rein kosmetische Zwischen-Regung ueberkonstruiert.
     * MEDICINE taucht bewusst in keiner Phase auf (siehe Klassendoku).
     */
    private fun weightsFor(phase: DayPhase): Map<AnimationType, Int> = when (phase) {
        DayPhase.MORNING -> mapOf(
            AnimationType.MOVE to 5,
            AnimationType.GENERAL to 3,
            AnimationType.DRINK to 3,
            AnimationType.FOCUS to 2,
            AnimationType.MINDFULNESS to 1
        )
        DayPhase.MIDDAY -> mapOf(
            AnimationType.WORK to 3,
            AnimationType.FOCUS to 3,
            AnimationType.DRINK to 3,
            AnimationType.MOVE to 3,
            AnimationType.GENERAL to 2,
            AnimationType.BOOK to 1,
            AnimationType.CREATIVITY to 1
        )
        DayPhase.EVENING -> mapOf(
            AnimationType.REST to 3,
            AnimationType.LOVE to 2,
            // **Der Abendspaziergang.** Vorher stand hier gar kein MOVE, und damit war der Abend
            // vollstaendig drinnen - dabei ist es die Tageszeit, zu der jemand tatsaechlich noch
            // einmal vor die Tuer geht. Zusammen mit dem Licht in den Fenstern (siehe
            // PlayScene.litWindows) ist das die schoenste Szene, die diese Welt zu bieten hat,
            // und sie kam bis dahin nie vor.
            AnimationType.MOVE to 4,
            AnimationType.BOOK to 2,
            AnimationType.CREATIVITY to 2,
            AnimationType.MINDFULNESS to 2,
            AnimationType.DRINK to 1,
            AnimationType.GENERAL to 1
        )
        DayPhase.NIGHT -> mapOf(
            AnimationType.SLEEP to 5,
            AnimationType.REST to 3,
            AnimationType.MINDFULNESS to 1
        )
    }

    /**
     * Vereint Tagesphase und Gewohnheits-Signal additiv - der Ort, an dem eine kuenftige dritte
     * Signalquelle (siehe Klassendoku) genauso andocken wuerde: eine weitere Map/Set entgegennehmen
     * und ihre Gewichte hier draufaddieren, ohne [weightsFor] oder [pickWeighted] anzufassen.
     *
     * Ein geboostetes Thema, das in [phase] gar nicht vorkommt (Grundgewicht 0), taucht durch den
     * Bonus trotzdem gelegentlich auf - bewusst so: eine offene Trink-Gewohnheit darf auch nachts
     * einmal durchscheinen, nur seltener als ein phasentypisches Thema.
     *
     * MEDICINE wird hier defensiv nochmal aus [boostedTopics] entfernt, auch wenn
     * [com.notime.glyphsim.ui.PlayHabitSignal] es nie haette liefern sollen: eine echte, noch
     * offene Medikamenten-Erinnerung waere sonst genau das Signal, das MEDICINE in den
     * Zufallspool hebt - der eine Fall, den die Klassendoku ausdruecklich ausschliesst. Diese
     * Garantie soll unabhaengig davon gelten, was ein Aufrufer hier hineingibt.
     */
    private fun combinedWeights(
        phase: DayPhase,
        boostedTopics: Set<AnimationType>,
        stayAt: PlayScene.Place? = null,
        leaning: Set<AnimationType> = emptySet()
    ): Map<AnimationType, Int> {
        val base = weightsFor(phase)
        val relevantBoosts = boostedTopics - AnimationType.MEDICINE
        if (relevantBoosts.isEmpty() && stayAt == null && leaning.isEmpty()) return base
        val combined = base.toMutableMap()
        for (topic in relevantBoosts) {
            combined[topic] = (combined[topic] ?: 0) + HABIT_BOOST
        }
        // Die Neigung wirkt nur auf das, was zur Tageszeit ohnehin vorkommt - aus demselben Grund
        // wie beim Verweilen: Sie soll gewichten, nicht Themen erfinden.
        for (topic in base.keys) {
            if (topic in leaning) combined[topic] = (combined[topic] ?: 0) + LEANING_BONUS
        }
        if (stayAt != null) {
            // NUR was ohnehin schon zur Tageszeit passt: Ein Thema, das in dieser Phase gar nicht
            // vorkommt, soll nicht allein deshalb auftauchen, weil die Figur zufaellig im
            // richtigen Zimmer steht - sonst schliefe sie mittags weiter, bloss weil sie im
            // Schlafzimmer aufgewacht ist.
            for (topic in base.keys) {
                if (PlayScene.forTopic(topic) == stayAt) {
                    combined[topic] = (combined[topic] ?: 0) + STAY_BONUS
                }
            }
        }
        return combined
    }

    /** Dieselbe einfache Gewichts-Auswahl wie in PlayModeRoll.pickWeighted - hier dupliziert
     *  statt geteilt, weil die beiden Stellen unterschiedliche Dinge gewichten (Level-Stufe vs.
     *  Tagesphase) und rein zufaellig denselben Algorithmus verwenden. */
    private fun pickWeighted(weights: Map<AnimationType, Int>, random: Random = Random): AnimationType {
        val total = weights.values.sum()
        if (total <= 0) return AnimationType.GENERAL
        var roll = random.nextInt(total)
        for ((topic, weight) in weights) {
            roll -= weight
            if (roll < 0) return topic
        }
        return weights.keys.firstOrNull() ?: AnimationType.GENERAL
    }

    /**
     * Wartezeit bis zur naechsten Regung.
     *
     * Kurze Abstaende von 6-14s machten aus dem Tagesablauf eine Demonstrationsschleife: Die Figur
     * war fast staendig unterwegs oder begann schon die naechste Sache. 18-36s lassen eine Szene
     * erst als Aufenthalt lesbar werden. Laengere Routinen tragen ihre eigene Zeit und werden
     * deshalb nicht durch kuenstlich kurze Zwischenpausen verdichtet.
     */
    fun nextPauseMillis(phase: DayPhase = currentDayPhase()): Long =
        (PAUSE_RANGE_MS.random() * restfulness(phase) * PlayTimeLapse.paceFactor())
            .toLong().coerceAtLeast(400L)

    /**
     * Wie ruhig es zur jeweiligen Tageszeit zugeht - streckt die Pausen zwischen den Regungen.
     *
     * Ohne das war der Avatar um drei Uhr nachts genauso umtriebig wie mittags, und das nahm dem
     * ganzen Tagesablauf die Glaubwuerdigkeit: Ein Wesen, das rund um die Uhr im selben Takt
     * herumlaeuft, hat keinen Tag, sondern eine Schleife. Nachts liegt zwischen zwei Regungen
     * gut das Dreifache - es TUT weiterhin etwas, nur eben selten und langsam, wie jemand, der
     * schlaeft und sich gelegentlich umdreht.
     */
    private fun restfulness(phase: DayPhase): Float = when (phase) {
        DayPhase.MORNING -> 1f
        DayPhase.MIDDAY -> 1f
        DayPhase.EVENING -> 1.4f
        DayPhase.NIGHT -> 3.2f
    }

    private val PAUSE_RANGE_MS = 18_000L..36_000L

    private const val HABIT_BOOST = 4

    /**
     * **So oft darf eine Regung am selben Ort bleiben**, bevor der Zuschlag fuers Verweilen
     * aussetzt (der Aufrufer zaehlt mit, siehe DockScreen).
     *
     * Ohne diese Grenze haette das Verweilen den umgekehrten Fehler erzeugt: Wer im Wohnzimmer
     * sitzt, bekommt dort dauernd Rueckenwind fuers Bleiben - und die Figur kaeme kaum noch vor
     * die Tuer, also genau in den Zustand, gegen den Strasse und Wald ueberhaupt angelegt wurden.
     * Mehrere Regungen lang verweilen ist ein Aufenthalt; danach faellt der Zuschlag weg und ein
     * Ortswechsel wird wieder wahrscheinlicher.
     */
    const val MAX_STAY_ROUNDS = 5

    /**
     * Zuschlag fuers Bleiben (siehe [nextTopic]).
     *
     * Fuenf gibt dem aktuellen Ort zusaetzliches Gewicht, erfindet aber keine unpassenden Themen:
     * nachts geht die Figur weiterhin ins Bett, auch wenn sie zuvor in der Kueche stand.
     */
    private const val STAY_BONUS = 5

    /**
     * Zuschlag fuer die Neigung - halb so viel wie eine offene Gewohnheit.
     *
     * Eine offene Gewohnheit ist eine Aufgabe von heute und darf drueckend wirken; eine Neigung
     * ist gewachsen und darf nur faerben.
     */
    private const val LEANING_BONUS = 2

    private const val ACTION_WEIGHT_PERFORM = 7
    private const val ACTION_WEIGHT_FIDGET = 5
    private const val ACTION_WEIGHT_WANDER = 1

    /**
     * Die grosse Freuden-Reaktion ist bewusst die SELTENSTE.
     *
     * Anfangs kam sie in jedem fuenften Durchgang, also gut alle anderthalb Minuten - eine Figur,
     * die derart oft vor Freude auf und ab huepft, wirkt nicht froehlich, sondern beliebig. Sie
     * ist die Ausnahme, die kleinen Alltagsregungen ([Action.FIDGET]) sind die Regel.
     */
    private const val ACTION_WEIGHT_FLOURISH = 1
    private const val ACTION_WEIGHT_TOTAL =
        ACTION_WEIGHT_PERFORM + ACTION_WEIGHT_FIDGET + ACTION_WEIGHT_WANDER + ACTION_WEIGHT_FLOURISH
}
