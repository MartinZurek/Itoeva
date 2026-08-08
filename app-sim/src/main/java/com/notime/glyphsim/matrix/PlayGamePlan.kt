package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType

/**
 * Eine Entwicklungsstufe im Spielplan eines Avatars.
 *
 * [fromLevel] ist das Level, ab dem diese Stufe gilt (die passende ist die letzte, deren
 * [fromLevel] das aktuelle Level nicht ueberschreitet). [topicWeights] gewichtet die Themen
 * gegeneinander - ein Thema mit Gewicht 6 kommt dreimal so oft wie eines mit Gewicht 2.
 * [intervalMinutes] ist die Spanne, aus der die Wartezeit bis zum naechsten Auftauchen gewuerfelt
 * wird.
 */
data class PlayStage(
    val fromLevel: Int,
    val topicWeights: Map<AnimationType, Int>,
    val intervalMinutes: IntRange
)

/**
 * Der Spielplan EINES Avatars: was er einem wann vorsetzt.
 *
 * **Warum ein Plan statt reinem Zufall.** Vorher wuerfelte der Spielmodus zu 70 % das
 * Signature-Thema und sonst irgendeines der uebrigen - gleichverteilt. Das ist zwar
 * abwechslungsreich, aber es erzaehlt nichts: Jeder Avatar fuehlte sich nach denselben zwoelf
 * Themen in zufaelliger Reihenfolge an, nur mit anderem Schwerpunkt. Ein Spielplan macht daraus
 * eine Handschrift - Hootlet setzt einem Ruhe und Buecher vor, Wyrmling treibt an, Gloop
 * entschleunigt.
 *
 * **Warum Stufen nach Level.** Sie sind der Ort, an dem sich die "Entwicklung" tatsaechlich zeigt,
 * ohne dass dafuer neue Pixel-Art noetig waere: Am Anfang ist ein Avatar auf sein eigenes Thema
 * konzentriert, mit steigendem Level oeffnet er sich - Wyrmling lernt zur Bewegung die Erholung
 * dazu, Starlet neben der Achtsamkeit das Lesen. Der Fortschritt aendert also, WAS man erlebt, und
 * nicht nur eine Zahl.
 *
 * **Der Zufall bleibt.** Der Plan legt die Gewichte fest, nicht die Reihenfolge - gewuerfelt wird
 * weiterhin bei jedem Auftauchen neu (siehe [PlayModeRoll]). Es ist ein Wahrscheinlichkeitsraum,
 * kein Drehbuch.
 *
 * [AnimationType.MEDICINE] kommt in KEINEM Plan vor: Eine zufaellige Spiel-Ausloesung darf nie wie
 * eine echte Medikamenten-Erinnerung wirken.
 *
 * **Erster Entwurf.** Die Gewichte sind aus den Charakterbeschreibungen der Avatare abgeleitet
 * (siehe [AvatarSpecies.personalityRes]/[AvatarSpecies.taglineRes]) und bewusst als Datenstruktur
 * angelegt, damit sich eine ausformulierte Storyline je Avatar hier eintragen laesst, ohne die
 * Mechanik anzufassen.
 */
data class PlayGamePlan(val stages: List<PlayStage>) {
    fun stageFor(level: Int): PlayStage =
        stages.last { it.fromLevel <= level }
}

object PlayGamePlans {

    fun forSpecies(species: AvatarSpecies): PlayGamePlan = when (species) {
        AvatarSpecies.PUFFLING -> puffling
        AvatarSpecies.STARLET -> starlet
        AvatarSpecies.WYRMLING -> wyrmling
        AvatarSpecies.FENNEC -> fennec
        AvatarSpecies.GLOOP -> gloop
        AvatarSpecies.HOOTLET -> hootlet
    }

    // ---- PUFFLING: der neugierige Optimist ----
    // Entdeckt staendig Neues und freut sich ueber jede Kleinigkeit - sein Plan ist deshalb der
    // breiteste von allen und wird mit jeder Stufe noch bunter. Zuegiges Tempo: Neugier wartet
    // nicht lange.
    private val puffling = PlayGamePlan(
        listOf(
            PlayStage(
                fromLevel = 1,
                topicWeights = mapOf(
                    AnimationType.GENERAL to 6,
                    AnimationType.DRINK to 3,
                    AnimationType.MOVE to 3
                ),
                intervalMinutes = 3..7
            ),
            PlayStage(
                fromLevel = 3,
                topicWeights = mapOf(
                    AnimationType.GENERAL to 5,
                    AnimationType.DRINK to 3,
                    AnimationType.MOVE to 3,
                    AnimationType.BOOK to 2,
                    AnimationType.CREATIVITY to 2
                ),
                intervalMinutes = 3..8
            ),
            PlayStage(
                fromLevel = 5,
                topicWeights = mapOf(
                    AnimationType.GENERAL to 4,
                    AnimationType.DRINK to 3,
                    AnimationType.MOVE to 3,
                    AnimationType.BOOK to 3,
                    AnimationType.CREATIVITY to 3,
                    AnimationType.FOCUS to 2,
                    AnimationType.REST to 2,
                    AnimationType.LOVE to 2
                ),
                intervalMinutes = 2..8
            )
        )
    )

    // ---- STARLET: die freundliche Traeumerin ----
    // Warmherzig und ruhig, erinnert daran, freundlich zu sich selbst zu sein. Langsamstes Tempo
    // neben Gloop - Draengeln waere das Gegenteil von dem, wofuer sie steht.
    private val starlet = PlayGamePlan(
        listOf(
            PlayStage(
                fromLevel = 1,
                topicWeights = mapOf(
                    AnimationType.MINDFULNESS to 7,
                    AnimationType.REST to 3
                ),
                intervalMinutes = 6..12
            ),
            PlayStage(
                fromLevel = 3,
                topicWeights = mapOf(
                    AnimationType.MINDFULNESS to 6,
                    AnimationType.REST to 3,
                    AnimationType.LOVE to 3,
                    AnimationType.SLEEP to 2
                ),
                intervalMinutes = 5..12
            ),
            PlayStage(
                fromLevel = 5,
                topicWeights = mapOf(
                    AnimationType.MINDFULNESS to 5,
                    AnimationType.REST to 3,
                    AnimationType.LOVE to 3,
                    AnimationType.SLEEP to 2,
                    AnimationType.BOOK to 3,
                    AnimationType.CREATIVITY to 2
                ),
                intervalMinutes = 5..14
            )
        )
    )

    // ---- WYRMLING: der kleine Motivator ----
    // Mutig und voller Energie, aber nie hektisch. Schnellstes Tempo. Die spaeteren Stufen bringen
    // ihm bewusst Trinken und Erholung bei: Ein Motivator, der irgendwann auch ans Durchhalten
    // denkt, ist die glaubwuerdigere Entwicklung als einer, der nur lauter wird.
    private val wyrmling = PlayGamePlan(
        listOf(
            PlayStage(
                fromLevel = 1,
                topicWeights = mapOf(
                    AnimationType.MOVE to 7,
                    AnimationType.GENERAL to 3
                ),
                intervalMinutes = 2..5
            ),
            PlayStage(
                fromLevel = 3,
                topicWeights = mapOf(
                    AnimationType.MOVE to 6,
                    AnimationType.GENERAL to 2,
                    AnimationType.FOCUS to 3,
                    AnimationType.WORK to 2
                ),
                intervalMinutes = 2..6
            ),
            PlayStage(
                fromLevel = 5,
                topicWeights = mapOf(
                    AnimationType.MOVE to 5,
                    AnimationType.GENERAL to 2,
                    AnimationType.FOCUS to 3,
                    AnimationType.WORK to 2,
                    AnimationType.DRINK to 3,
                    AnimationType.REST to 2
                ),
                intervalMinutes = 2..7
            )
        )
    )

    // ---- FENNEC: der ruhige Beschuetzer ----
    // Gelassen, aufmerksam, verlaesslich - "erinnert ohne je zu nerven". Haeufig, aber gleichmaessig:
    // die engste Intervall-Spanne aller Plaene, damit sein Takt vorhersehbar wirkt statt sprunghaft.
    private val fennec = PlayGamePlan(
        listOf(
            PlayStage(
                fromLevel = 1,
                topicWeights = mapOf(
                    AnimationType.DRINK to 7,
                    AnimationType.GENERAL to 3
                ),
                intervalMinutes = 3..5
            ),
            PlayStage(
                fromLevel = 3,
                topicWeights = mapOf(
                    AnimationType.DRINK to 6,
                    AnimationType.GENERAL to 2,
                    AnimationType.REST to 3,
                    AnimationType.MOVE to 2
                ),
                intervalMinutes = 3..6
            ),
            PlayStage(
                fromLevel = 5,
                topicWeights = mapOf(
                    AnimationType.DRINK to 5,
                    AnimationType.GENERAL to 2,
                    AnimationType.REST to 3,
                    AnimationType.MOVE to 2,
                    AnimationType.SLEEP to 2,
                    AnimationType.MINDFULNESS to 2
                ),
                intervalMinutes = 3..7
            )
        )
    )

    // ---- GLOOP: der Entschleuniger ----
    // Gemuetlich, etwas chaotisch, charmant. Langsam - und in der letzten Stufe bewusst die
    // breiteste, ungeordnetste Themenmischung: Das Chaotische seines Charakters zeigt sich darin,
    // dass am Ende alles gleich wahrscheinlich ist.
    private val gloop = PlayGamePlan(
        listOf(
            PlayStage(
                fromLevel = 1,
                topicWeights = mapOf(
                    AnimationType.REST to 7,
                    AnimationType.SLEEP to 3
                ),
                intervalMinutes = 7..14
            ),
            PlayStage(
                fromLevel = 3,
                topicWeights = mapOf(
                    AnimationType.REST to 5,
                    AnimationType.SLEEP to 3,
                    AnimationType.DRINK to 3,
                    AnimationType.GENERAL to 3
                ),
                intervalMinutes = 6..14
            ),
            PlayStage(
                fromLevel = 5,
                topicWeights = mapOf(
                    AnimationType.REST to 3,
                    AnimationType.SLEEP to 3,
                    AnimationType.DRINK to 3,
                    AnimationType.GENERAL to 3,
                    AnimationType.BOOK to 3,
                    AnimationType.CREATIVITY to 3,
                    AnimationType.LOVE to 3
                ),
                intervalMinutes = 5..16
            )
        )
    )

    // ---- HOOTLET: der weise Beobachter ----
    // Still, geduldig, klug - "beobachtet mehr, als er spricht". Ruhiges, weit auseinanderliegendes
    // Tempo; die Themen bleiben durchgehend die konzentrierten.
    private val hootlet = PlayGamePlan(
        listOf(
            PlayStage(
                fromLevel = 1,
                topicWeights = mapOf(
                    AnimationType.FOCUS to 7,
                    AnimationType.BOOK to 3
                ),
                intervalMinutes = 5..11
            ),
            PlayStage(
                fromLevel = 3,
                topicWeights = mapOf(
                    AnimationType.FOCUS to 6,
                    AnimationType.BOOK to 3,
                    AnimationType.WORK to 3,
                    AnimationType.CREATIVITY to 2
                ),
                intervalMinutes = 4..11
            ),
            PlayStage(
                fromLevel = 5,
                topicWeights = mapOf(
                    AnimationType.FOCUS to 5,
                    AnimationType.BOOK to 3,
                    AnimationType.WORK to 2,
                    AnimationType.CREATIVITY to 3,
                    AnimationType.MINDFULNESS to 3,
                    AnimationType.REST to 2
                ),
                intervalMinutes = 4..12
            )
        )
    )
}
