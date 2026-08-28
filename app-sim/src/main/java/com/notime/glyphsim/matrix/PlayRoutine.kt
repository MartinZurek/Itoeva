package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import kotlin.random.Random

/**
 * Ein einzelner Schritt in einem Tagesablauf - siehe [PlayRoutine].
 */
sealed interface RoutineStep {

    /** Zu einem benannten Platz gehen (siehe [PlayScene.Station]). */
    data class GoTo(val station: PlayScene.Station) : RoutineStep

    /**
     * Zu einer freien Stelle gehen, angegeben als Bruchteil der Bildschirmbreite - fuers
     * Umherlaufen dort, wo es nichts zu benutzen gibt (draussen).
     */
    data class Stroll(val anchorX: Float) : RoutineStep

    /**
     * Den Platz, an dem die Figur gerade steht, tatsaechlich BENUTZEN: sich ins Bett legen, sich
     * in den Sessel setzen. Sie rueckt dabei auf den Benutzungsplatz der Requisite, und deren
     * vordere Teile (Decke, Sitzkante) legen sich ueber sie - siehe [PlayScene.buildFront].
     */
    data class Occupy(val station: PlayScene.Station) : RoutineStep

    /** Wieder aufstehen: zurueck auf den Boden, Verdeckung endet. */
    data object Rise : RoutineStep

    /** Eine Handlung ausfuehren - greift auf die vorhandene Reaktions-Bibliothek zurueck. */
    data class Act(val topic: AnimationType) : RoutineStep

    /** Eine Alltagsregung einschieben. */
    data class Stir(val fidget: AvatarAnimations.Fidget) : RoutineStep

    /** Eine Phase der mehrstufigen Drachen-Szene sichtbar machen. */
    data class Kite(val phase: PlayEffects.KitePhase) : RoutineStep

    /** Eine Phase der Fussball-Szene; TRICK wird erst nach dem Lernen verwendet. */
    data class Football(val phase: PlayEffects.FootballPhase) : RoutineStep

    /** Eine Phase der Basketball-Szene: prellen, zielen, werfen, treffen. */
    data class Basketball(val phase: PlayEffects.BasketballPhase) : RoutineStep

    /** Eine Phase des Krafttrainings auf dem Sportplatz. */
    data class Training(val phase: PlayEffects.TrainingPhase) : RoutineStep

    /** Eine Phase der Angel-Szene am Teich. */
    data class Fishing(val phase: PlayEffects.FishingPhase) : RoutineStep

    /** Einen Moment nichts tun (Ruhe-Schleife laeuft weiter). */
    data class Linger(val millis: Long) : RoutineStep

    /**
     * Einen Gegenstand schalten - derzeit die Stehlampe (siehe [PlayScene.build]).
     *
     * Der erste Schritt, der die WELT veraendert statt nur die Figur. Alle uebrigen bewegen oder
     * animieren sie; dieser hinterlaesst etwas, das auch dann noch da ist, wenn sie weitergeht.
     */
    data class Switch(val device: PlayScene.Station, val on: Boolean) : RoutineStep

    /**
     * Einen Gegenstand AN SICH NEHMEN - er wird ab jetzt sichtbar mitgetragen, bis [Drop] kommt
     * (siehe [PlayEffects.carriedCells]).
     *
     * Zusammen mit dem Aufblitzen im Moment des Zugriffs ist das die Antwort auf die Frage, die
     * man sich beim Zuschauen sonst staendig stellt: Was hat er da eigentlich vor? Ein Buch in der
     * Hand auf dem Weg zum Sessel beantwortet sie, ohne dass irgendwo Text stehen muesste.
     */
    data class Take(val item: PlayEffects.Carried) : RoutineStep

    /** Das Getragene wieder ablegen. */
    data object Drop : RoutineStep

    /**
     * MITTEN im Ablauf in einen anderen Raum wechseln - durch die Tuer (siehe moveToPlace in
     * DockScreen).
     *
     * Damit wird ein Vorhaben moeglich, das sich ueber mehrere Zimmer erstreckt: einkaufen gehen,
     * mit dem Eingekauften nach Hause kommen, es dort essen. Vorher legte der Ort vor dem Ablauf
     * fest, wo alles stattfindet - eine Besorgung liess sich so gar nicht erzaehlen.
     */
    data class GoToPlace(val place: PlayScene.Place) : RoutineStep
}

/**
 * Was der Avatar an einem Ort TUT - als Folge von Schritten statt als eine einzelne Animation.
 *
 * **Warum das der entscheidende Unterschied ist.** Bis hierher spielte der Avatar eine Reaktion
 * NEBEN einem Moebelstueck ab: Er stand am Bett und machte Schlafbewegungen, aber er legte sich
 * nie hinein. Das ist der Abstand zwischen einer Figur vor einer Kulisse und einem Bewohner. Was
 * gefehlt hat, waren nicht mehr Animationen - es war eine Ebene dazwischen, auf der eine
 * Taetigkeit aus mehreren Schritten mit Wegen dazwischen besteht.
 *
 * Ein Ablauf wie "sich etwas zu essen machen" ist genau das: zum Regal gehen, etwas herunterholen,
 * damit zum Tisch gehen, zubereiten, essen. Fuenf Schritte, drei davon Wege - und erst dadurch
 * entsteht der Eindruck von Absicht statt von Zufallsbewegung.
 *
 * **Alles Vorhandene wird wiederverwendet.** [RoutineStep.Act] greift auf dieselben
 * Reaktions-Sequenzen zurueck, die auch eine gefuetterte Erinnerung zeigt
 * ([AvatarAnimations.reactionFor]); [RoutineStep.GoTo] benutzt denselben Geh-Zyklus wie bisher.
 * Neu ist allein die Verkettung - deshalb ist dieser Schritt so klein, obwohl er so viel aendert.
 *
 * **Erweiterbar gedacht.** Neue Taetigkeiten entstehen als neue Schrittfolgen, ohne dass an der
 * Kulisse, den Reaktionen oder der Ablaufsteuerung etwas anzufassen waere. Wer einen Platz
 * hinzufuegt, traegt ihn in [PlayScene.Station] ein und kann ihn sofort in Ablaeufen benutzen.
 */
data class PlayRoutine(val steps: List<RoutineStep>)

object PlayRoutines {

    /**
     * Waehlt einen Ablauf zum Thema. Ein Thema hat oft MEHRERE moegliche Ablaeufe - das ist die
     * Stelle, an der sich Abwechslung am billigsten erzeugen laesst: dieselbe Absicht
     * ("etwas trinken") an unterschiedlichen Tagen anders ausgefuehrt.
     */
    /**
     * Waehlt einen Ablauf zum Thema.
     *
     * [needsShopping] ist der eine Fall, in dem NICHT gewuerfelt wird: Ist der Vorrat leer (siehe
     * [PlayPantry]), muss der Ablauf ueber den Laden fuehren. Damit hat zum ersten Mal ein Zustand
     * der Welt Vorrang vor dem Zufall - der Avatar geht einkaufen, WEIL nichts mehr da ist, und
     * nicht, weil die Wuerfel es so wollten.
     */
    fun forTopic(
        topic: AnimationType,
        needsShopping: Boolean = false,
        footballTrickLearned: Boolean = false,
        random: Random = Random
    ): PlayRoutine {
        val options = allFor(topic)
        if (needsShopping) {
            options.firstOrNull { routine ->
                routine.steps.any { it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP }
            }?.let { return it }
        }
        // Umgekehrt: Solange etwas da ist, faellt der Einkauf weg - sonst liefe er auch mit vollem
        // Kuehlschrank jedes zweite Mal in den Laden.
        val special = options.filter { routine ->
            routine.steps.any {
                it is RoutineStep.Kite || it is RoutineStep.Football ||
                    it is RoutineStep.Basketball || it is RoutineStep.Training ||
                    it is RoutineStep.Fishing
            }
        }
        val everyday = options.filterNot { routine ->
            routine in special || routine.steps.any {
                it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP
            }
        }
        val pool = everyday.ifEmpty { options }
        // Eine gemeinsame Ziehung statt nacheinander ausgefuehrter Prozentpruefungen: Sonst wird
        // jede spaeter eingetragene Sportart automatisch seltener als die davor. Basketball,
        // Fussball, Training, Drachen und Angeln sind hier gleichberechtigt.
        if (topic == AnimationType.MOVE && special.isNotEmpty() &&
            random.nextInt(100) < SPECIAL_ACTIVITY_CHANCE_PERCENT
        ) {
            val chosen = special.random(random)
            return if (chosen.steps.any { it is RoutineStep.Football }) {
                footballRoutine(footballTrickLearned)
            } else {
                chosen
            }
        }
        return pool[random.nextInt(pool.size)]
    }

    private const val SPECIAL_ACTIVITY_CHANCE_PERCENT = 70

    fun footballRoutine(trickLearned: Boolean): PlayRoutine = PlayRoutine(
        buildList {
            add(RoutineStep.GoToPlace(PlayScene.Place.SPORT))
            add(RoutineStep.Stroll(0.30f))
            add(RoutineStep.Football(PlayEffects.FootballPhase.DRIBBLE))
            add(RoutineStep.Linger(12_000L))
            add(RoutineStep.Football(PlayEffects.FootballPhase.AIM))
            add(RoutineStep.Linger(4_000L))
            if (trickLearned) {
                add(RoutineStep.Football(PlayEffects.FootballPhase.TRICK))
                add(RoutineStep.Linger(5_000L))
            }
            add(RoutineStep.Football(PlayEffects.FootballPhase.KICK))
            add(RoutineStep.Linger(8_000L))
        }
    )

    fun basketballRoutine(): PlayRoutine = PlayRoutine(
        listOf(
            RoutineStep.GoToPlace(PlayScene.Place.SPORT),
            RoutineStep.Stroll(0.32f),
            RoutineStep.Basketball(PlayEffects.BasketballPhase.DRIBBLE),
            RoutineStep.Linger(8_000L),
            RoutineStep.Basketball(PlayEffects.BasketballPhase.AIM),
            RoutineStep.Linger(3_000L),
            RoutineStep.Basketball(PlayEffects.BasketballPhase.SHOOT),
            RoutineStep.Linger(3_500L),
            RoutineStep.Basketball(PlayEffects.BasketballPhase.SCORE),
            RoutineStep.Linger(6_000L)
        )
    )

    fun trainingRoutine(): PlayRoutine = PlayRoutine(
        listOf(
            RoutineStep.GoToPlace(PlayScene.Place.SPORT),
            RoutineStep.Stroll(0.42f),
            RoutineStep.Training(PlayEffects.TrainingPhase.WARM_UP),
            RoutineStep.Linger(4_000L),
            RoutineStep.Training(PlayEffects.TrainingPhase.LIFT),
            RoutineStep.Linger(7_000L),
            RoutineStep.Training(PlayEffects.TrainingPhase.REST),
            RoutineStep.Linger(5_000L)
        )
    )

    /** Angeln am Teich: auswerfen, lange warten, dann der Fang - ruhiger Gegenpol zu Fussball. */
    fun fishingRoutine(): PlayRoutine = PlayRoutine(
        listOf(
            RoutineStep.GoToPlace(PlayScene.Place.POND),
            RoutineStep.Stroll(0.30f),
            RoutineStep.Fishing(PlayEffects.FishingPhase.CAST),
            RoutineStep.Linger(3_000L),
            RoutineStep.Fishing(PlayEffects.FishingPhase.WAIT),
            RoutineStep.Linger(22_000L),
            RoutineStep.Fishing(PlayEffects.FishingPhase.CATCH),
            RoutineStep.Linger(6_000L),
            RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND)
        )
    )

    /**
     * ALLE Ablaeufe zu einem Thema - oeffentlich, weil sich sonst nicht pruefen laesst, dass jeder
     * Ablauf nur Plaetze anspricht, die es an seinem Ort tatsaechlich gibt (siehe
     * PlayRoutineTest). Ein Ablauf, der ins Leere greift, wuerde sonst stillschweigend einen
     * Schritt ueberspringen - der Avatar bliebe einfach stehen, ohne dass irgendwo ein Fehler
     * auftaucht.
     */
    fun allFor(topic: AnimationType): List<PlayRoutine> = when (topic) {

        // ---- Schlafen: hingehen, hineinlegen, liegen bleiben ----
        AnimationType.SLEEP -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.BED),
                    RoutineStep.Stir(AvatarAnimations.Fidget.YAWN),
                    RoutineStep.Occupy(PlayScene.Station.BED),
                    RoutineStep.Act(AnimationType.SLEEP),
                    RoutineStep.Linger(4_000L),
                    RoutineStep.Rise
                )
            ),
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.BED),
                    RoutineStep.Occupy(PlayScene.Station.BED),
                    RoutineStep.Linger(6_000L),
                    RoutineStep.Rise,
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH)
                )
            ),
            // Nachts einmal aufstehen und wieder hineinlegen - der Ablauf, den man beim
            // Zuschauen am ehesten wiedererkennt, weil ihn jeder kennt.
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.BED),
                    RoutineStep.Occupy(PlayScene.Station.BED),
                    RoutineStep.Linger(3_500L),
                    RoutineStep.Rise,
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Occupy(PlayScene.Station.BED),
                    RoutineStep.Act(AnimationType.SLEEP),
                    RoutineStep.Linger(3_000L),
                    RoutineStep.Rise
                )
            )
        )

        // ---- Ausruhen: aufs Sofa, Fernseher an ----
        AnimationType.REST -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.TV),
                    RoutineStep.Switch(PlayScene.Station.TV, on = true),
                    RoutineStep.GoTo(PlayScene.Station.SEAT),
                    RoutineStep.Occupy(PlayScene.Station.SEAT),
                    RoutineStep.Act(AnimationType.REST),
                    RoutineStep.Linger(4_000L),
                    RoutineStep.Rise,
                    RoutineStep.GoTo(PlayScene.Station.TV),
                    RoutineStep.Switch(PlayScene.Station.TV, on = false)
                )
            ),
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.SEAT),
                    RoutineStep.Occupy(PlayScene.Station.SEAT),
                    RoutineStep.Act(AnimationType.REST),
                    RoutineStep.Linger(3_000L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.YAWN),
                    RoutineStep.Rise
                )
            )
        )

        AnimationType.BOOK -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.LAMP),
                    RoutineStep.Switch(PlayScene.Station.LAMP, on = true),
                    RoutineStep.GoTo(PlayScene.Station.BOOKSHELF),
                    RoutineStep.Take(PlayEffects.Carried.BOOK),
                    RoutineStep.GoTo(PlayScene.Station.SEAT),
                    RoutineStep.Occupy(PlayScene.Station.SEAT),
                    RoutineStep.Act(AnimationType.BOOK),
                    RoutineStep.Linger(3_500L),
                    RoutineStep.Rise,
                    RoutineStep.GoTo(PlayScene.Station.BOOKSHELF),
                    RoutineStep.Drop,                                    // stellt es zurueck
                    RoutineStep.GoTo(PlayScene.Station.LAMP),
                    RoutineStep.Switch(PlayScene.Station.LAMP, on = false)
                )
            ),
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.BOOKSHELF),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Take(PlayEffects.Carried.BOOK),
                    RoutineStep.GoTo(PlayScene.Station.SEAT),
                    RoutineStep.Occupy(PlayScene.Station.SEAT),
                    RoutineStep.Act(AnimationType.BOOK),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.Rise,
                    RoutineStep.Drop
                )
            )
        )

        AnimationType.MINDFULNESS -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.SEAT),
                    RoutineStep.Occupy(PlayScene.Station.SEAT),
                    RoutineStep.Act(AnimationType.MINDFULNESS),
                    RoutineStep.Linger(3_000L),
                    RoutineStep.Rise
                )
            ),
            // **Im Stehen innehalten - und das ist mehr als eine zweite Variante.**
            //
            // Gemeldet als "wenn ich ihn um Achtsamkeit bitte, geht er ins Bett und schlaeft
            // kurz". Er ging in Wahrheit in die Leseecke und setzte sich; weil das Sitzmoebel ihn
            // aber bis zum Hals verdeckt (siehe PlayScene.buildFront) und die Reaktion eine ruhige
            // ist, sieht Sitzen mit geschlossenen Augen genauso aus wie Liegen. Wer stehen bleibt,
            // atmet sichtbar - dieselbe Absicht, aber nicht mehr zu verwechseln.
            PlayRoutine(
                listOf(
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Act(AnimationType.MINDFULNESS),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.Act(AnimationType.MINDFULNESS),
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH)
                )
            )
        )

        AnimationType.LOVE -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.SEAT),
                    RoutineStep.Occupy(PlayScene.Station.SEAT),
                    RoutineStep.Act(AnimationType.LOVE),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.Rise
                )
            ),
            // Zu zweit vor dem Geraet - dieselbe Naehe, nur nicht im Sitzen erzaehlt.
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.TV),
                    RoutineStep.Switch(PlayScene.Station.TV, on = true),
                    RoutineStep.Act(AnimationType.LOVE),
                    RoutineStep.Linger(3_000L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Switch(PlayScene.Station.TV, on = false)
                )
            )
        )

        // ---- Sich etwas holen: Kuehlschrank, dann Tisch. Der Weg dazwischen IST die Handlung. ----
        AnimationType.DRINK -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.FRIDGE),
                    RoutineStep.Take(PlayEffects.Carried.FOOD),
                    RoutineStep.GoTo(PlayScene.Station.TABLE),
                    RoutineStep.Drop,
                    RoutineStep.Act(topic),
                    RoutineStep.Linger(1_500L)
                )
            ),
            // Einkaufen: der erste Ablauf, der ueber mehrere Raeume geht - und seit es die
            // Strasse gibt, fuehrt er auch dorthin, wo ein Einkauf tatsaechlich entlanggeht.
            PlayRoutine(
                listOf(
                    RoutineStep.GoToPlace(PlayScene.Place.STREET),
                    RoutineStep.Stroll(0.66f),
                    RoutineStep.GoToPlace(PlayScene.Place.SHOP),
                    RoutineStep.GoTo(PlayScene.Station.RACK),
                    RoutineStep.Take(PlayEffects.Carried.FOOD),
                    RoutineStep.GoTo(PlayScene.Station.CHECKOUT),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),  // bezahlt
                    RoutineStep.GoToPlace(PlayScene.Place.KITCHEN),
                    RoutineStep.GoTo(PlayScene.Station.FRIDGE),
                    RoutineStep.Drop,                                       // raeumt ein
                    RoutineStep.GoTo(PlayScene.Station.TABLE),
                    RoutineStep.Act(AnimationType.DRINK)
                )
            ),
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.FRIDGE),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),  // sucht
                    RoutineStep.Take(PlayEffects.Carried.CUP),
                    RoutineStep.GoTo(PlayScene.Station.TABLE),
                    RoutineStep.Act(topic),
                    RoutineStep.Drop,
                    RoutineStep.GoTo(PlayScene.Station.FRIDGE),
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH)       // raeumt weg
                )
            )
        )

        // ---- Koerperpflege: ins Bad ----
        AnimationType.MEDICINE -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.BASIN),
                    RoutineStep.Stir(AvatarAnimations.Fidget.SHAKE),      // waescht sich
                    RoutineStep.GoTo(PlayScene.Station.TUB),
                    RoutineStep.Occupy(PlayScene.Station.TUB),
                    RoutineStep.Linger(4_000L),
                    RoutineStep.Rise,
                    RoutineStep.Stir(AvatarAnimations.Fidget.SHAKE)       // schuettelt sich trocken
                )
            ),
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.BASIN),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Act(AnimationType.MEDICINE),
                    RoutineStep.Linger(1_500L)
                )
            )
        )

        // ---- Arbeiten: ueber die Strasse hin, arbeiten, ueber die Strasse zurueck ----
        //
        // Der Ablauf beginnt DRAUSSEN (siehe PlayScene.forTopic) - der Arbeitsweg ist der
        // einzige Teil eines Arbeitstages, der unter freiem Himmel stattfindet, und er fehlte
        // vorher vollstaendig. Zwei Schritte auf der Strasse genuegen dafuer: Sie machen aus
        // "ist jetzt bei der Arbeit" ein "geht zur Arbeit".
        AnimationType.WORK -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.Stroll(0.72f),
                    RoutineStep.GoToPlace(PlayScene.Place.WORK),
                    RoutineStep.GoTo(PlayScene.Station.WORKPLACE),
                    RoutineStep.Act(AnimationType.WORK),
                    RoutineStep.Linger(3_000L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH),
                    RoutineStep.Act(AnimationType.WORK),
                    RoutineStep.Linger(2_000L),
                    // Feierabend: derselbe Weg zurueck. Ohne ihn bliebe die Figur bis zur
                    // naechsten Regung im Buero stehen, und der Arbeitstag haette kein Ende.
                    RoutineStep.GoToPlace(PlayScene.Place.STREET),
                    RoutineStep.Stroll(0.16f),
                    RoutineStep.Stir(AvatarAnimations.Fidget.YAWN)
                )
            ),
            PlayRoutine(
                listOf(
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH),
                    RoutineStep.Stroll(0.78f),
                    RoutineStep.GoToPlace(PlayScene.Place.WORK),
                    RoutineStep.GoTo(PlayScene.Station.WORKPLACE),
                    RoutineStep.Act(AnimationType.WORK),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.GoToPlace(PlayScene.Place.STREET),
                    RoutineStep.Stroll(0.20f)
                )
            ),
            // Derselbe Arbeitsweg, nur durch die STADT statt ueber die Strasse - hin UND zurueck
            // durch denselben Ort, damit der Feierabend am selben Platz endet, an dem der Tag
            // begann (siehe die Begruendung beim ersten Ablauf oben).
            PlayRoutine(
                listOf(
                    RoutineStep.GoToPlace(PlayScene.Place.CITY),
                    RoutineStep.Stroll(0.68f),
                    RoutineStep.GoToPlace(PlayScene.Place.WORK),
                    RoutineStep.GoTo(PlayScene.Station.WORKPLACE),
                    RoutineStep.Act(AnimationType.WORK),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH),
                    RoutineStep.Act(AnimationType.WORK),
                    RoutineStep.GoToPlace(PlayScene.Place.CITY),
                    RoutineStep.Stroll(0.24f)
                )
            )
        )

        AnimationType.FOCUS -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.DESK),
                    RoutineStep.Act(topic),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH),
                    RoutineStep.Act(topic)
                )
            ),
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.DESK),
                    RoutineStep.Act(topic),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Linger(2_000L)
                )
            ),
            // Mit Pause: aufstehen, einmal durchs Zimmer, zurueck an den Tisch. Wer eine Stunde
            // sitzt, steht zwischendurch auf - und im Bild ist genau das der Unterschied
            // zwischen Arbeiten und Dasitzen.
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.DESK),
                    RoutineStep.Act(topic),
                    RoutineStep.Linger(2_000L),
                    RoutineStep.Stroll(0.30f),
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH),
                    RoutineStep.GoTo(PlayScene.Station.DESK),
                    RoutineStep.Act(topic),
                    RoutineStep.Linger(1_500L)
                )
            )
        )

        // ---- Draussen: spazieren, Sport, in den Wald, ueber die Strasse ----
        //
        // **Der Grossteil der Abwechslung dieses Wesens steckt hier.** Alle uebrigen Themen sind
        // an ein Zimmer gebunden - man kocht in der Kueche und schlaeft im Schlafzimmer, da gibt
        // es wenig zu variieren. Draussen dagegen ist der ORT selbst waehlbar, und ein Gang durch
        // den Wald erzaehlt etwas voellig anderes als eine Runde um den Block, obwohl beide aus
        // denselben Schritten bestehen.
        AnimationType.MOVE -> listOf(
            // Drachensteigen: wenig Weg, lange erkennbare Beschaeftigung am selben Ort.
            PlayRoutine(
                listOf(
                    RoutineStep.Stroll(0.36f),
                    RoutineStep.Kite(PlayEffects.KitePhase.PREPARE),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.Kite(PlayEffects.KitePhase.LAUNCH),
                    RoutineStep.Act(AnimationType.MOVE),
                    RoutineStep.Kite(PlayEffects.KitePhase.FLY),
                    RoutineStep.Linger(28_000L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Linger(12_000L),
                    RoutineStep.Kite(PlayEffects.KitePhase.LAND),
                    RoutineStep.Linger(2_500L)
                )
            ),
            // Sport: quer durch den Park und zurueck, mit Bewegung an beiden Enden.
            PlayRoutine(
                listOf(
                    RoutineStep.Stroll(0.12f),
                    RoutineStep.Act(AnimationType.MOVE),
                    RoutineStep.Stroll(0.80f),
                    RoutineStep.Act(AnimationType.MOVE),
                    RoutineStep.Stroll(0.45f),
                    RoutineStep.Stir(AvatarAnimations.Fidget.SHAKE)
                )
            ),
            // Spaziergang mit Pause auf der Bank.
            PlayRoutine(
                listOf(
                    RoutineStep.Stroll(0.16f),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.GoTo(PlayScene.Station.BENCH),
                    RoutineStep.Occupy(PlayScene.Station.BENCH),
                    RoutineStep.Linger(3_500L),
                    RoutineStep.Rise,
                    RoutineStep.Stroll(0.75f),
                    RoutineStep.Act(AnimationType.MOVE)
                )
            ),
            // Der WALDSPAZIERGANG - ueber die Strasse hinaus, bis der Park hinter einem liegt.
            //
            // Die drei Orte hintereinander sind hier die eigentliche Aussage: Erst dadurch, dass
            // man an etwas VORBEIKOMMT, wird aus einem Weg eine Strecke. Am Stamm im Wald sitzen
            // heisst dann auch wirklich, angekommen zu sein.
            PlayRoutine(
                listOf(
                    RoutineStep.GoToPlace(PlayScene.Place.STREET),
                    RoutineStep.Stroll(0.70f),
                    RoutineStep.GoToPlace(PlayScene.Place.FOREST),
                    RoutineStep.Stroll(0.30f),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.GoTo(PlayScene.Station.BENCH),
                    RoutineStep.Occupy(PlayScene.Station.BENCH),
                    RoutineStep.Linger(4_000L),
                    RoutineStep.Rise,
                    RoutineStep.Stroll(0.84f),
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH)
                )
            ),
            // Nur in den Wald und dort umherstreifen - ohne Ziel, das ist der Punkt.
            PlayRoutine(
                listOf(
                    RoutineStep.GoToPlace(PlayScene.Place.FOREST),
                    RoutineStep.Stroll(0.18f),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Stroll(0.76f),
                    RoutineStep.Act(AnimationType.MOVE),
                    RoutineStep.Stroll(0.40f),
                    RoutineStep.Linger(2_000L)
                )
            ),
            // Eine Runde um den Block: Strasse, Bank, zurueck. Der kurze Ablauf fuer zwischendurch.
            PlayRoutine(
                listOf(
                    RoutineStep.GoToPlace(PlayScene.Place.STREET),
                    RoutineStep.Stroll(0.24f),
                    RoutineStep.Act(AnimationType.MOVE),
                    RoutineStep.GoTo(PlayScene.Station.BENCH),
                    RoutineStep.Occupy(PlayScene.Station.BENCH),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.Rise,
                    RoutineStep.Stroll(0.88f),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND)
                )
            ),
            // Spaziergang zur WIESE - ueber die Strasse hinaus, wie der Waldspaziergang oben,
            // nur zu dessen offenerem Gegenstueck: keine Baeume, nur Weite und eine Bank in der
            // Mitte zum Verweilen.
            PlayRoutine(
                listOf(
                    RoutineStep.GoToPlace(PlayScene.Place.STREET),
                    RoutineStep.Stroll(0.60f),
                    RoutineStep.GoToPlace(PlayScene.Place.MEADOW),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Stroll(0.35f),
                    RoutineStep.GoTo(PlayScene.Station.BENCH),
                    RoutineStep.Occupy(PlayScene.Station.BENCH),
                    RoutineStep.Linger(3_500L),
                    RoutineStep.Rise,
                    RoutineStep.Act(AnimationType.MOVE)
                )
            ),
            // Eigener Sportplatz; der gelernte Trick wird erst bei der Laufzeit-Auswahl ergänzt.
            footballRoutine(trickLearned = false),
            // Korb und Ball erscheinen nur fuer diesen Ablauf; der Platz bleibt sonst offen.
            basketballRoutine(),
            // Kraft und Ausdauer als zweiter Ast des Sports, mit sichtbarer Hantel.
            trainingRoutine(),
            // Eigener Teich, ruhiger Gegenpol zum Sportplatz - siehe fishingRoutine().
            fishingRoutine()
        )

        // ---- Etwas machen: in die eigene Ecke, ans Werkstueck, dranbleiben ----
        //
        // **Der Ablauf, der am wenigsten zum Ziel kommt, und das mit Absicht.** Alle uebrigen
        // enden mit einem Ergebnis: Der Einkauf steht im Kuehlschrank, das Buch ist zurueck im
        // Regal, die Arbeit ist getan. Hier wird zweimal angesetzt, einmal innegehalten und wieder
        // weitergemacht - so sieht es aus, wenn jemand an etwas ARBEITET, das noch nicht fertig
        // ist. Was genau, sagt die Werkstatt (siehe PlayScene.Home.craft): Der Drache schmiedet,
        // der Schleim toepfert, der Wuestenfuchs graebt.
        AnimationType.CREATIVITY -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.CRAFT),
                    RoutineStep.Act(AnimationType.CREATIVITY),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),   // betrachtet es
                    RoutineStep.Act(AnimationType.CREATIVITY),
                    RoutineStep.Linger(1_500L)
                )
            ),
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.CRAFT),
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH),       // macht sich bereit
                    RoutineStep.Act(AnimationType.CREATIVITY),
                    RoutineStep.Linger(3_000L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.SHAKE)          // schuettelt sich aus
                )
            ),
            // Musizieren im Park: das Handwerkszeug wird diesmal MITGENOMMEN statt an einem
            // festen Platz benutzt - dieselbe Reaktions-Animation, nur unterwegs statt an der
            // Werkbank, mit der Gitarre sichtbar in der Hand (siehe PlayEffects.Carried.GUITAR).
            PlayRoutine(
                listOf(
                    RoutineStep.Take(PlayEffects.Carried.GUITAR),
                    RoutineStep.GoToPlace(PlayScene.Place.PARK),
                    RoutineStep.Stroll(0.40f),
                    RoutineStep.Act(AnimationType.CREATIVITY),
                    RoutineStep.Linger(6_000L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Act(AnimationType.CREATIVITY),
                    RoutineStep.Linger(4_000L),
                    RoutineStep.Drop
                )
            ),
            // Staffelei mit hinaus auf die Wiese - dasselbe Prinzip, ein anderes Motiv: das Bild
            // entsteht draussen statt in der eigenen Ecke.
            PlayRoutine(
                listOf(
                    RoutineStep.Take(PlayEffects.Carried.EASEL),
                    RoutineStep.GoToPlace(PlayScene.Place.MEADOW),
                    RoutineStep.Stroll(0.55f),
                    RoutineStep.Act(AnimationType.CREATIVITY),
                    RoutineStep.Linger(5_000L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Act(AnimationType.CREATIVITY),
                    RoutineStep.Linger(3_000L),
                    RoutineStep.Drop
                )
            )
        )

        // ---- Einfach da sein: der haeufigste Fall und lange der aermste ----
        //
        // **GENERAL hatte genau einen Ablauf: an Ort und Stelle eine Reaktion, fertig.** Das war
        // vertretbar, solange es ein Rest-Thema war - im Spiel ist es aber das am haeufigsten
        // gewuerfelte ueberhaupt (siehe PlayGamePlans), und es fuehrt ins Wohnzimmer, also in den
        // Raum, in dem man ohnehin die meiste Zeit zusieht. Ausgerechnet dort tat er dann als
        // einziges nichts. Vier Fassungen kosten keine neue Animation und keine neue Requisite -
        // es sind Wege und Pausen zwischen dem, was es schon gibt.
        AnimationType.GENERAL -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.Act(topic),
                    RoutineStep.Linger(1_200L)
                )
            ),
            // Sich kurz hinsetzen, ohne etwas dabei zu tun. Der unscheinbarste Ablauf von allen -
            // und der, der am ehesten nach jemandem aussieht, der hier wohnt.
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.SEAT),
                    RoutineStep.Occupy(PlayScene.Station.SEAT),
                    RoutineStep.Linger(4_000L),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Rise
                )
            ),
            // Einmal durchs Zimmer und wieder zurueck.
            PlayRoutine(
                listOf(
                    RoutineStep.Stroll(0.68f),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Linger(1_500L),
                    RoutineStep.Stroll(0.22f),
                    RoutineStep.Act(topic)
                )
            ),
            // Nach dem Rechten sehen: Licht an, kurz stehen bleiben, Licht wieder aus.
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.TV),
                    RoutineStep.Switch(PlayScene.Station.TV, on = true),
                    RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                    RoutineStep.Linger(2_500L),
                    RoutineStep.Switch(PlayScene.Station.TV, on = false),
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH)
                )
            )
        )
    }
}
