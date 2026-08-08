package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.FrameCrossfade

/**
 * Liefert die Frames fuer den Tamagotchi-Avatar in ui/DockScreen.kt: eine ruhige
 * Idle-Schleife (waehrend er auf die Uhr wartet) und eine kurze, pro [AnimationType]
 * thematisch passende Reaktionssequenz, ausgeloest sobald die Uhr auf ihn geschoben
 * wird - beides parametrisiert ueber [AvatarSpecies] (sechs waehlbare Grundformen,
 * siehe [AvatarBodies]), damit alle Spezies dieselbe Reaktions-Bibliothek teilen und
 * sich nur in Silhouette/Kopfschmuck/Fuessen/Schwanz unterscheiden.
 *
 * Anders als matrix/ReminderAnimations.kt (sparsame Punktwolken fuers 13x13-
 * Hardware-Raster) ist die Kreatur eine gefuellte Silhouette auf dem groesseren
 * 16x16-[AvatarGeometry]-Raster - Augen/Mund sind dabei NICHT zusaetzliche Punkte,
 * sondern "ausgestanzte" Luecken in der gefuellten Form (wie bei einer beleuchteten
 * Kuerbis-Laterne), das gibt mehr Kontrast/Struktur als reine Punktkonturen.
 * [AvatarSpriteView] rendert das Ergebnis als eckiges Pixel-Sprite statt als rundem
 * LED-Punktraster.
 *
 * Bewusst OHNE Helligkeits-Ueberblendung zwischen Keyframes (steps = 0 statt des
 * Crossfade-Defaults 2): auf einer gefuellten Sprite-Silhouette liess das ganze
 * Bereiche der Kreatur in Zwischenhelligkeit "verwaschen" statt hart umzuschalten -
 * bei einem Punktraster (Uhr, Erinnerungs-Icons) liest sich dasselbe Prinzip als
 * weiches Ein-/Ausblenden einzelner LEDs, bei einer gefuellten Figur eher wie ein
 * unscharfer Uebergang. Harte Keyframe-Wechsel plus [MatrixAnimator.AVATAR_FRAME_DELAY_MS]
 * (deutlich schneller als der Glyph-Matrix-Takt) ergeben zusammen die knackige,
 * schnelle Bewegung wie beim Claude-Code-Maskottchen statt der vorherigen traegen,
 * weichgezeichneten Wirkung.
 */
object AvatarAnimations {
    private const val GRID = AvatarGeometry.SIZE
    private const val CROSSFADE_STEPS = 0

    // ---- Timing ----
    //
    // Frueher liefen ALLE Reaktionen mit starren 120ms pro Frame. Das liess ruhige Reaktionen
    // (Ausruhen, Einschlafen, Achtsamkeit) genauso hetzen wie einen Hopser und nahm den
    // Betonungen jede Wirkung - echte Animation lebt von Tempowechsel: schneller Einsatz,
    // langsames Ausklingen. Jeder Keyframe traegt deshalb jetzt seine eigene Standzeit.
    /** Knackige Aktion (Absprung, Zuender, Zoom). */
    internal const val FAST_MS = 90L
    /** Grundtakt. */
    internal const val BEAT_MS = 140L
    /** Ruhige, getragene Bewegung (Atmen, Einschlafen, Daempfen). */
    internal const val SLOW_MS = 260L
    /**
     * Nachklang am Ende einer Reaktion. Ohne ihn waren die Reaktionen mit 5-8 Frames unter einer
     * Sekunde vorbei und als Belohnung fuers Fuettern kaum wahrnehmbar - der Avatar verschwand
     * praktisch im selben Moment, in dem er reagierte.
     */
    internal const val SETTLE_MS = 480L

    /** Ein Keyframe mit eigener Standzeit. */
    internal data class Beat(val points: List<Pair<Int, Int>>, val holdMs: Long = BEAT_MS)

    internal fun List<Pair<Int, Int>>.beat(holdMs: Long = BEAT_MS) = Beat(this, holdMs)

    /**
     * Fasst unmittelbar aufeinanderfolgende identische Posen zu einer zusammen und addiert dabei
     * ihre Standzeiten.
     *
     * Noetig, seit Reaktionen aus zwei Teilen zusammengesetzt werden (Handlung des Erinnerungs-
     * typs + Abschluss der Spezies, siehe [speciesFinish]): Die letzte Pose der Handlung kann
     * zufaellig genau der ersten Pose des Abschlusses entsprechen - bei FOCUS/GLOOP war das der
     * Fall. Zwei identische Frames hintereinander sind ein sichtbarer Stillstand mitten in der
     * Bewegung; genau darauf achtet auch AvatarAnimationsTest.everyReactionAdvancesOnEveryFrame.
     *
     * Von Hand liesse sich das nicht sicherstellen - es sind dreizehn Handlungen mal sechs
     * Spezies, und jede Aenderung an einer Handlung koennte eine neue Ueberschneidung erzeugen.
     * Zusammengefasst statt eine Pose zu verwerfen, damit die vorgesehene Gesamtdauer erhalten
     * bleibt: aus zwei gleichen Bildern wird eins, das entsprechend laenger steht.
     */
    private fun List<Beat>.mergeRepeats(): List<Beat> {
        val result = mutableListOf<Beat>()
        for (beat in this) {
            val previous = result.lastOrNull()
            if (previous != null && previous.points.toSet() == beat.points.toSet()) {
                result[result.lastIndex] = previous.copy(holdMs = previous.holdMs + beat.holdMs)
            } else {
                result += beat
            }
        }
        return result
    }

    /**
     * Eine fertige Sequenz: Frames plus die zugehoerigen Standzeiten (gleiche Laenge, gleiche
     * Reihenfolge). [MatrixAnimator.playTimed] laeuft sie damit im vorgesehenen Rhythmus statt
     * mit festem Takt ab - inzwischen sowohl fuer Reaktionen als auch fuer die Ruhe-Schleifen,
     * die ihren Charakter gerade aus dem ungleichmaessigen Timing beziehen.
     */
    data class AvatarSequence(val frames: List<IntArray>, val holdsMs: List<Long>)

    /**
     * Baut einen Frame der Kreatur: Silhouette minus Augen-/Mund-Luecken, optional
     * verschoben ([dx]/[dy], fuers Huepfen/Wippen/Laufen), mit Fuss-/Schwanz-/Akzent-Pose und
     * einem [prop] (das jeweilige Mini-Motiv, z.B. Glas, Buch).
     *
     * [propFollows] entscheidet, ob das Prop die Koerperbewegung mitmacht: was die Kreatur in
     * der Hand haelt (Glas, Tasse, Laptop, Buch, Tablette), soll beim Huepfen mitgehen - blieb
     * es starr stehen, wirkte es wie ein danebenliegender Aufkleber statt wie etwas Angefasstes.
     * Umgebung dagegen (Glocke, Mond, Aura, Herzen, Funken, Sucherwinkel) bleibt bewusst an
     * ihrer eigenen Stelle im Raster stehen.
     */
    internal fun creatureFrame(
        body: AvatarBody,
        eyeHoles: Set<Pair<Int, Int>> = body.eyesOpen,
        mouthHoles: Set<Pair<Int, Int>> = body.mouthNeutral,
        dx: Int = 0,
        dy: Int = 0,
        feetSpread: Int = 0,
        tailWag: Int = 0,
        accentPhase: Int = 0,
        prop: List<Pair<Int, Int>> = emptyList(),
        propFollows: Boolean = false
    ): List<Pair<Int, Int>> {
        val holes = eyeHoles + mouthHoles
        val silhouette = (body.silhouette - holes) + body.accessory
        val shifted = silhouette.map { (x, y) -> (x + dx) to (y + dy) }
        val feetShifted = body.feet(feetSpread).map { (x, y) -> (x + dx) to (y + dy) }
        val tailShifted = body.tail(tailWag).map { (x, y) -> (x + dx) to (y + dy) }
        val accentShifted = body.accent(accentPhase).map { (x, y) -> (x + dx) to (y + dy) }
        val propPlaced = if (propFollows) prop.map { (x, y) -> (x + dx) to (y + dy) } else prop
        // Alles um die Kopffreiheit nach unten versetzt: Die Koerper sind auf 16x16 gezeichnet,
        // das Raster ist aber hoeher (siehe AvatarGeometry.HEADROOM). Der Versatz passiert
        // zentral an dieser einen Stelle, damit saemtliche Posen, Requisiten und Sprunghoehen
        // unveraendert in ihren gewohnten Koordinaten geschrieben bleiben koennen.
        return (shifted + feetShifted + tailShifted + accentShifted + propPlaced)
            .map { (x, y) -> x to (y + AvatarGeometry.HEADROOM) }
    }

    // ---- Idle-Timing ----
    //
    // Vorher lief die Idle-Schleife mit starren 120ms und liess dabei zweimal je Durchlauf den
    // GANZEN Koerper um ein Pixel springen. Bei diesem Takt liest sich das nicht als Atmen,
    // sondern als Vibrieren - alle vier Achtelsekunden ein Ruck.
    //
    // Ruhe entsteht nicht dadurch, alles gleichmaessig zu verlangsamen, sondern durch das
    // Gegenteil von Gleichmass: lange Ruhelagen, unterbrochen von kurzen, kleinen Regungen. Der
    // Koerper bleibt deshalb ueberwiegend voellig still, und bewegt wird nur ein Detail - Ohr,
    // Fluegel, Lid, Schwanz. Das wirkt lebendig UND gelassen, waehrend eine Ganzkoerperbewegung
    // bei jeder Geschwindigkeit unruhig bleibt.
    /** Lange Ruhelage - traegt den Grossteil jeder Schleife. */
    private const val IDLE_REST_MS = 1000L
    /** Kurze Regung (Ohr zuckt, Fluegel schlaegt, Schwanz wedelt). */
    private const val IDLE_STIR_MS = 190L
    /** Schnelles Zwinkern - fuer die kleinen, einpixeligen Augen. */
    private const val IDLE_WINK_MS = 110L
    /** Getragenes Lid der grossaeugigen Spezies (STARLET, GLOOP). */
    private const val IDLE_LID_MS = 200L

    /**
     * Ruhe-Schleife des Avatars - **pro Spezies eine eigene**, nicht mehr eine gemeinsame fuer
     * alle sechs.
     *
     * Vorher unterschieden sich die Spezies ausschliesslich in ihrer Silhouette; sie atmeten,
     * wedelten und blinzelten alle im identischen Takt. Die Wahl des Avatars war damit eine
     * Frage der Form, nicht des Charakters. Jetzt hat jede ihren eigenen Rhythmus und ihre
     * eigene Leitbewegung - abgeleitet aus ihrer Anatomie, damit Aussehen und Verhalten
     * zusammenpassen:
     *
     * - **PUFFLING** - gutmuetig: wedelt zweimal mit dem Schwanz, atmet einmal sanft durch.
     * - **STARLET** - verträumt: fast regungslos, dafuer ein langes, schweres Blinzeln der
     *   riesigen Augen; der Sternzopf schwingt nach.
     * - **WYRMLING** - unruhige Kraft: kurzer Doppelschlag der Fluegel, danach lange Ruhe.
     * - **FENNEC** - wachsam: Koerper voellig still, die grossen Ohren drehen sich nach links
     *   und rechts, als lausche er.
     * - **GLOOP** - fluessig: die einzige Spezies, deren Koerper selbst arbeitet - quillt aus
     *   und zieht sich zusammen, ohne je zu huepfen (hat weder Fuesse noch Schwanz).
     * - **HOOTLET** - bedaechtig: sehr lange Stille, dann ein Doppelblinzeln, wie eine Eule.
     */
    /**
     * Wie stark [mood] die Ruhe-Schleife verlangsamt, und ob die Lider haengen.
     *
     * Der Zustand wird ueber Tempo und Blick ausgedrueckt, nicht ueber eine andere Silhouette:
     * die Figur bleibt dieselbe, ihr Verhalten aendert sich. Das ist derselbe Weg, auf dem auch
     * die Spezies ihren Charakter bekommen (siehe unten) - und es kommt ohne einen zweiten Satz
     * gezeichneter Koerper aus.
     */
    private fun moodTempo(mood: AvatarMood): Float = when (mood) {
        AvatarMood.HAPPY -> 0.85f      // etwas munterer
        AvatarMood.NEUTRAL -> 1.0f
        AvatarMood.CONTENT -> 1.0f
        AvatarMood.HUNGRY -> 1.35f     // spuerbar traeger
        AvatarMood.SAD -> 1.8f         // schwerfaellig
    }

    /** Bei gedrueckter Stimmung bleiben die Lider halb geschlossen - der Blick wirkt matt. */
    private fun moodEyes(body: AvatarBody, mood: AvatarMood): Set<Pair<Int, Int>> =
        if (mood == AvatarMood.SAD || mood == AvatarMood.HUNGRY) body.eyesHalf else body.eyesOpen

    fun idleSequence(species: AvatarSpecies, mood: AvatarMood = AvatarMood.NEUTRAL): AvatarSequence {
        val body = AvatarBodies.forSpecies(species)
        val beats = when (species) {
            AvatarSpecies.PUFFLING -> listOf(
                creatureFrame(body).beat(IDLE_REST_MS),
                creatureFrame(body, tailWag = -1, accentPhase = 1).beat(IDLE_STIR_MS),
                creatureFrame(body, tailWag = 1).beat(IDLE_STIR_MS),
                creatureFrame(body, tailWag = -1, accentPhase = 1).beat(IDLE_STIR_MS),
                creatureFrame(body).beat(IDLE_REST_MS),
                // Einmal pro Durchlauf ein weicher Atemzug - als einzige Ganzkoerperbewegung
                // dieser Spezies und mit langer Ruhe davor/danach, damit sie als Durchatmen
                // gelesen wird und nicht als Ruck.
                creatureFrame(body, dy = -1).beat(340L),
                creatureFrame(body).beat(IDLE_REST_MS),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(IDLE_WINK_MS),
                creatureFrame(body, eyeHoles = body.eyesClosed).beat(IDLE_LID_MS),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(IDLE_WINK_MS)
            )

            AvatarSpecies.STARLET -> listOf(
                creatureFrame(body).beat(1300L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(IDLE_LID_MS),
                creatureFrame(body, eyeHoles = body.eyesClosed).beat(300L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(IDLE_LID_MS),
                creatureFrame(body).beat(1100L),
                // Der Sternzopf schwingt aus und zurueck - kleine Regung, grosse Wirkung, weil
                // sonst nichts an dieser Spezies in Bewegung ist.
                creatureFrame(body, accentPhase = 1).beat(320L),
                creatureFrame(body, accentPhase = -1).beat(320L),
                creatureFrame(body).beat(700L)
            )

            AvatarSpecies.WYRMLING -> listOf(
                creatureFrame(body).beat(900L),
                // Doppelschlag: schnell hoch, schnell runter, noch einmal hoch - danach lange
                // Ruhe. Der Kontrast macht die Kraft aus, nicht die Dauer.
                creatureFrame(body, accentPhase = 1).beat(FAST_MS),
                creatureFrame(body, accentPhase = -1).beat(FAST_MS),
                creatureFrame(body, accentPhase = 1).beat(FAST_MS),
                creatureFrame(body).beat(1100L),
                creatureFrame(body, tailWag = -1).beat(360L),
                creatureFrame(body, tailWag = 1).beat(360L),
                creatureFrame(body).beat(800L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(IDLE_WINK_MS),
                creatureFrame(body, eyeHoles = body.eyesClosed).beat(IDLE_LID_MS),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(IDLE_WINK_MS)
            )

            AvatarSpecies.FENNEC -> listOf(
                creatureFrame(body).beat(1200L),
                // Ohren drehen sich einzeln zur Seite, der Koerper bleibt bewusst voellig
                // unbewegt - Aufmerksamkeit zeigt sich im Lauschen, nicht im Zappeln.
                creatureFrame(body, accentPhase = 1).beat(220L),
                creatureFrame(body).beat(160L),
                creatureFrame(body, accentPhase = -1).beat(220L),
                creatureFrame(body).beat(1400L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(90L),
                creatureFrame(body, eyeHoles = body.eyesClosed).beat(120L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(90L),
                creatureFrame(body).beat(600L),
                creatureFrame(body, tailWag = 1).beat(260L),
                creatureFrame(body).beat(900L)
            )

            AvatarSpecies.GLOOP -> listOf(
                creatureFrame(body).beat(800L),
                // Quellen und Zusammenziehen sind hier die Atmung selbst. Bewusst mit langen
                // Standzeiten: schnelles Quetschen sieht nach Zittern aus, langsames nach Masse.
                creatureFrame(body, accentPhase = 1).beat(420L),
                creatureFrame(body).beat(500L),
                creatureFrame(body, accentPhase = -1).beat(420L),
                creatureFrame(body).beat(700L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(IDLE_LID_MS),
                creatureFrame(body, eyeHoles = body.eyesClosed).beat(360L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(IDLE_LID_MS),
                creatureFrame(body).beat(600L)
            )

            AvatarSpecies.HOOTLET -> listOf(
                // Auffaellig lange Stille - Eulen sitzen regungslos und werden dann ploetzlich
                // aktiv. Die Stille ist hier die Charaktereigenschaft.
                creatureFrame(body).beat(1600L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(90L),
                creatureFrame(body, eyeHoles = body.eyesClosed).beat(130L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(90L),
                creatureFrame(body).beat(220L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(90L),
                creatureFrame(body, eyeHoles = body.eyesClosed).beat(130L),
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(90L),
                creatureFrame(body).beat(1300L),
                creatureFrame(body, accentPhase = 1).beat(240L),
                creatureFrame(body, accentPhase = -1).beat(240L),
                creatureFrame(body).beat(1000L)
            )
        }
        // Die Stimmung wirkt NACHTRAEGLICH auf die fertige Schleife, statt in jeder der sechs
        // Spezies-Sequenzen eingebaut zu sein: sie streckt nur das Tempo und senkt die Lider.
        // Dadurch bleibt der jeweilige Charakter (Ohren-Lauschen, Fluegelschlag, Quellen) in
        // jeder Stimmung erhalten - er laeuft nur schneller oder schwerfaelliger ab.
        val tempo = moodTempo(mood)
        val restingEyes = moodEyes(body, mood)
        val moodBeats = beats.map { beat ->
            // Nur die Ruhelagen bekommen den matten Blick; Frames, die ohnehin schon ein
            // veraendertes Auge zeigen (Blinzeln), bleiben unangetastet - sonst waere der
            // Lidschlag nicht mehr vom Grundzustand zu unterscheiden.
            val points = if (restingEyes !== body.eyesOpen && beat.points == creatureFrame(body)) {
                creatureFrame(body, eyeHoles = restingEyes)
            } else {
                beat.points
            }
            Beat(points, (beat.holdMs * tempo).toLong())
        }
        val frames = FrameCrossfade.withCrossfades(GRID, AvatarGeometry.HEIGHT, moodBeats.map { it.points }, steps = CROSSFADE_STEPS, loop = true)
        return AvatarSequence(frames, moodBeats.map { it.holdMs })
    }

    /** Ruhelage als Einzelbild - fuer Vorschaubilder (Einstellungs-Dialog) und Pruefungen. */
    fun idlePose(species: AvatarSpecies): IntArray = idleSequence(species).frames.first()

    /**
     * [libraryAnimationLabel] ist nur bei einer Bibliotheks-Animation gesetzt (siehe
     * ReminderAnimationEvent) - "Rocket" bekommt dadurch eine eigene, aufwendige Reaktion
     * ([rocketReactionKeyframes]) statt der generischen Fallback-Reaktion, die sonst jede
     * der 26 Bibliotheks-Animationen teilt (siehe DefaultLibraryAnimations).
     */
    /**
     * Verschiebung der Sprite-Box je Frame, als Bruchteil der Bildschirmgroesse - fuer Reaktionen,
     * bei denen der Avatar sein eigenes 16x16-Raster verlaesst.
     *
     * Leere Liste heisst: die Reaktion spielt an Ort und Stelle. Frueher fragte der Aufrufer
     * direkt auf `label == "Rocket"` ab; mit mehreren solchen Reaktionen gehoert die Entscheidung
     * hierher, wo auch die Keyframes liegen.
     */
    fun flightOffsetsFor(libraryAnimationLabel: String?): List<Pair<Float, Float>> =
        when (libraryAnimationLabel) {
            "Rocket" -> rocketFlightOffsets()
            else -> emptyList()
        }

    fun reactionFor(
        species: AvatarSpecies,
        type: AnimationType?,
        libraryAnimationLabel: String? = null
    ): AvatarSequence {
        val body = AvatarBodies.forSpecies(species)
        val signature = libraryAnimationLabel?.let { AvatarSignatureReactions.forLabel(it, body) }
        val beats = if (libraryAnimationLabel == "Rocket") {
            rocketReactionKeyframes(body)
        } else if (signature != null) {
            // Die 30 charakterspezifischen Animationen haben je eine eigene Antwort - siehe
            // [AvatarSignatureReactions]. mergeRepeats wie unten, weil auch hier zwei benachbarte
            // Posen zufaellig zusammenfallen koennen.
            signature.mergeRepeats()
        } else if (libraryAnimationLabel != null) {
            // Bibliotheks-Animationen ohne eigene Choreografie bekommen die spezies-typische
            // Freuden-Reaktion statt der generischen - dadurch faellt "TAMA" bei Gloop anders
            // aus als bei Fennec, obwohl beide dieselbe Erinnerung ausgeloest hat.
            speciesFlourish(species, body)
        } else {
            val story = when (type) {
                AnimationType.DRINK -> drinkKeyframes(body)
                AnimationType.MOVE -> moveKeyframes(body)
                AnimationType.GENERAL -> generalKeyframes(body)
                AnimationType.REST -> restKeyframes(body)
                AnimationType.WORK -> workKeyframes(body)
                AnimationType.MINDFULNESS -> mindfulnessKeyframes(body)
                AnimationType.LOVE -> loveKeyframes(body)
                AnimationType.SLEEP -> sleepKeyframes(body)
                AnimationType.MEDICINE -> medicineKeyframes(body)
                AnimationType.BOOK -> bookKeyframes(body)
                AnimationType.CREATIVITY -> creativityKeyframes(body)
                AnimationType.FOCUS -> focusKeyframes(body)
                null -> genericKeyframes(body)
            }
            // Der letzte Beat jeder Handlung war bisher ein allgemeiner Abschluss, bei jeder
            // Spezies derselbe. Er faellt weg und macht dem arteigenen Platz - siehe
            // [speciesFinish] fuer die Begruendung. Die Handlung endet damit in dem Moment, in
            // dem sie inhaltlich fertig ist (Glas leer, Buch zu, Tablette genommen), und wie
            // darauf reagiert wird, entscheidet der Avatar.
            // mergeRepeats NUR hier: Die Rakete oben darf nicht angetastet werden, weil
            // rocketFlightOffsets sich darauf verlaesst, dass Frames und Keyframes eins zu eins
            // bleiben - ein zusammengefasstes Bild wuerde die Flugbahn gegen die Animation
            // verschieben. Zusammengesetzt wird ohnehin nur hier.
            (story.dropLast(1) + speciesFinish(species, body, finishMoodFor(type))).mergeRepeats()
        }
        // steps = 0 heisst: genau ein Frame pro Keyframe - Frames und Standzeiten bleiben
        // dadurch 1:1 zueinander ausgerichtet (worauf sich auch rocketFlightOffsets stuetzt).
        val frames = FrameCrossfade.withCrossfades(GRID, AvatarGeometry.HEIGHT, beats.map { it.points }, steps = CROSSFADE_STEPS, loop = false)
        return AvatarSequence(frames, beats.map { it.holdMs })
    }

    /**
     * Ob eine Reaktion munter oder ruhig ausklingt.
     *
     * Nicht jede Erinnerung endet in Freude. Ausruhen, Schlafen und Achtsamkeit mit einem
     * Hopser abzuschliessen wuerde genau das zerstoeren, worum es dabei geht - man springt nicht
     * begeistert auf, nachdem man zur Ruhe gekommen ist. Diese drei bekommen deshalb einen
     * arteigenen Abschluss, der zurueckfaehrt statt hochzugehen.
     */
    private enum class FinishMood { LIVELY, CALM }

    private fun finishMoodFor(type: AnimationType?): FinishMood = when (type) {
        AnimationType.REST, AnimationType.SLEEP, AnimationType.MINDFULNESS -> FinishMood.CALM
        else -> FinishMood.LIVELY
    }

    /**
     * Der arteigene Abschluss jeder Reaktion - **das ist die Stelle, an der sich die sechs
     * Spezies voneinander unterscheiden.**
     *
     * Vorher endete jede Reaktion in demselben allgemeinen Schlusshopser; welchen Avatar man
     * gewaehlt hatte, war fuer den Ablauf belanglos. Die Wahl war eine Frage der Silhouette,
     * nicht des Charakters. Statt nun elf Handlungen mal sechs Spezies einzeln zu zeichnen -
     * 66 Sequenzen, die niemand konsistent halten kann - wird zusammengesetzt: **die Handlung
     * gehoert dem Erinnerungstyp, der Abschluss der Spezies.** Trinken bleibt Trinken, aber ein
     * Wyrmling hebt danach ab, waehrend ein Gloop nachwabbelt.
     *
     * Abgeleitet aus der jeweiligen Anatomie, damit Aussehen und Verhalten zusammenpassen -
     * dieselbe Grundlage wie bei [idleSequence] und [speciesFlourish]. Gloop etwa hat weder
     * Fuesse noch Schwanz; ein Abschluss aus Huepfen und Wedeln bliebe bei ihm wirkungslos.
     */
    private fun speciesFinish(species: AvatarSpecies, body: AvatarBody, mood: FinishMood): List<Beat> =
        when (mood) {
            FinishMood.LIVELY -> when (species) {
                // Wippt vor Freude seitlich, Schwanz wedelt gegenlaeufig.
                AvatarSpecies.PUFFLING -> listOf(
                    creatureFrame(body, dx = -1, dy = -1, tailWag = 1, feetSpread = 1, mouthHoles = body.mouthOpen).beat(FAST_MS),
                    creatureFrame(body, dx = 1, dy = -1, tailWag = -1, feetSpread = 1, mouthHoles = body.mouthOpen).beat(FAST_MS),
                    creatureFrame(body, tailWag = 1, accentPhase = 1).beat(SETTLE_MS)
                )
                // Der Sternzopf funkelt, die grossen Augen werden noch groesser.
                AvatarSpecies.STARLET -> listOf(
                    creatureFrame(body, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(3 to 2, 12 to 2)).beat(FAST_MS),
                    creatureFrame(body, dy = -1, accentPhase = -1, mouthHoles = body.mouthOpen, prop = listOf(2 to 4, 13 to 4)).beat(BEAT_MS),
                    creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
                )
                // Hebt kurz ab - Fluegelschlag mit Steigflug.
                AvatarSpecies.WYRMLING -> listOf(
                    creatureFrame(body, accentPhase = 1, dy = -2, mouthHoles = body.mouthOpen).beat(FAST_MS),
                    creatureFrame(body, accentPhase = -1, dy = -4, tailWag = 1, mouthHoles = body.mouthOpen).beat(BEAT_MS),
                    creatureFrame(body, accentPhase = 1, dy = -1, tailWag = -1).beat(SETTLE_MS)
                )
                // Schneller Doppelsprung, Ohren nach vorn.
                AvatarSpecies.FENNEC -> listOf(
                    creatureFrame(body, dy = -3, feetSpread = 1, accentPhase = 1, mouthHoles = body.mouthOpen).beat(FAST_MS),
                    creatureFrame(body, dy = 0, tailWag = 1).beat(FAST_MS),
                    creatureFrame(body, dy = -2, feetSpread = 1, accentPhase = -1, mouthHoles = body.mouthOpen).beat(SETTLE_MS)
                )
                // Kann nicht huepfen - quillt stattdessen auf und schwappt zur Seite.
                AvatarSpecies.GLOOP -> listOf(
                    creatureFrame(body, accentPhase = 1, dy = -1, mouthHoles = body.mouthOpen).beat(FAST_MS),
                    creatureFrame(body, accentPhase = 1, dx = 2, dy = -1, mouthHoles = body.mouthOpen).beat(BEAT_MS),
                    creatureFrame(body, accentPhase = -1, dx = -1).beat(SETTLE_MS)
                )
                // Plustert sich auf und wippt bedaechtig.
                AvatarSpecies.HOOTLET -> listOf(
                    creatureFrame(body, accentPhase = 1, dy = -1, mouthHoles = body.mouthOpen).beat(BEAT_MS),
                    creatureFrame(body, accentPhase = -1, mouthHoles = body.mouthOpen).beat(BEAT_MS),
                    creatureFrame(body, accentPhase = 1, eyeHoles = body.eyesHalf).beat(SETTLE_MS)
                )
            }

            // Ruhig: zurueckfahren statt hochgehen. Jede Spezies wird auf ihre Art still.
            FinishMood.CALM -> when (species) {
                AvatarSpecies.PUFFLING -> listOf(
                    creatureFrame(body, eyeHoles = body.eyesHalf, tailWag = 1).beat(SLOW_MS),
                    creatureFrame(body, eyeHoles = body.eyesClosed, tailWag = -1).beat(SETTLE_MS)
                )
                AvatarSpecies.STARLET -> listOf(
                    creatureFrame(body, eyeHoles = body.eyesHalf, accentPhase = -1).beat(SLOW_MS),
                    creatureFrame(body, eyeHoles = body.eyesClosed).beat(SETTLE_MS)
                )
                // Fluegel legen sich an.
                AvatarSpecies.WYRMLING -> listOf(
                    creatureFrame(body, eyeHoles = body.eyesHalf, accentPhase = -1, tailWag = 1).beat(SLOW_MS),
                    creatureFrame(body, eyeHoles = body.eyesClosed, tailWag = 0).beat(SETTLE_MS)
                )
                // Die grossen Ohren sinken.
                AvatarSpecies.FENNEC -> listOf(
                    creatureFrame(body, eyeHoles = body.eyesHalf, accentPhase = -1).beat(SLOW_MS),
                    creatureFrame(body, eyeHoles = body.eyesClosed, accentPhase = -1, dy = 1).beat(SETTLE_MS)
                )
                // Sackt in sich zusammen und wabbelt aus.
                AvatarSpecies.GLOOP -> listOf(
                    creatureFrame(body, eyeHoles = body.eyesHalf, accentPhase = -1, dy = 1).beat(SLOW_MS),
                    creatureFrame(body, eyeHoles = body.eyesClosed, accentPhase = 1).beat(SETTLE_MS)
                )
                // Langsames Doppelblinzeln wie eine Eule.
                AvatarSpecies.HOOTLET -> listOf(
                    creatureFrame(body, eyeHoles = body.eyesClosed).beat(SLOW_MS),
                    creatureFrame(body, eyeHoles = body.eyesHalf).beat(BEAT_MS),
                    creatureFrame(body, eyeHoles = body.eyesClosed).beat(SETTLE_MS)
                )
            }
        }

    /** Nur die Frames - fuer Aufrufer/Pruefungen, die das Timing nicht brauchen. */
    fun reactionFramesFor(species: AvatarSpecies, type: AnimationType?, libraryAnimationLabel: String? = null): List<IntArray> =
        reactionFor(species, type, libraryAnimationLabel).frames

    /**
     * Kurze, artspezifische Freuden-Reaktion ohne Bezug zu einer bestimmten Erinnerung - fuer ein
     * blosses Antippen des Avatars (Start- und Pflegebuch-Bildschirm) oder das Umkreisen-Easter-Egg
     * (siehe HomeScreen): keine Erinnerung wird dabei beantwortet, es gibt also keine Handlung
     * (Trinken, Bewegen, ...), auf die reagiert werden koennte - dieselbe Choreografie wie
     * [speciesFlourish], die sonst Bibliotheks-Animationen ohne eigene Reaktion bekommen.
     */
    fun tapReaction(species: AvatarSpecies): AvatarSequence {
        val body = AvatarBodies.forSpecies(species)
        val beats = speciesFlourish(species, body)
        val frames = FrameCrossfade.withCrossfades(GRID, AvatarGeometry.HEIGHT, beats.map { it.points }, steps = CROSSFADE_STEPS, loop = false)
        return AvatarSequence(frames, beats.map { it.holdMs })
    }

    /**
     * Geh-Zyklus fuer den Weg von einem Ort der Kulisse zum naechsten (siehe [PlayScene] und
     * ui/DockScreen.kt) - **die eine Schleife, die aus Verschieben ein Gehen macht.**
     *
     * Vorher lief waehrend eines Ortswechsels einfach die Ruhe-Schleife weiter, waehrend sich die
     * Sprite-Box verschob: die Figur GLITT, ganz gleich wie weich die Bewegung gerechnet war. Was
     * fehlt, ist nicht Bildrate, sondern ein Auf und Ab im Takt der Schritte - der Koerper muss
     * sich heben, wenn ein Fuss durchschwingt, und aufsetzen, wenn er landet.
     *
     * Vier Posen im Wechsel: zweimal Aufsetzen mit gespreizten Fuessen (der Koerper unten),
     * dazwischen jeweils die Schwungphase (Koerper eine Zeile hoeher, Fuesse zusammen). Schwanz
     * und arteigener Akzent laufen gegenlaeufig mit, damit der Zyklus nicht wie ein blosses
     * Huepfen aussieht.
     *
     * **Auch fuer Spezies ohne Fuesse tragfaehig.** GLOOP hat weder Fuesse noch Schwanz (siehe
     * [AvatarBodies]) - dort bleibt vom Zyklus das Heben und Senken des Koerpers plus das
     * Quellen des Akzents, was bei einem Schleimwesen ohnehin die richtige Fortbewegung ist.
     * Deshalb steckt die Bewegung bewusst in dy/accent und nicht allein in [AvatarBody.feet].
     */
    fun walkSequence(species: AvatarSpecies): AvatarSequence {
        val body = AvatarBodies.forSpecies(species)
        val beats = listOf(
            creatureFrame(body, feetSpread = 1, tailWag = 1).beat(WALK_STEP_MS),
            creatureFrame(body, dy = -1, accentPhase = 1).beat(WALK_STEP_MS),
            creatureFrame(body, feetSpread = 1, tailWag = -1).beat(WALK_STEP_MS),
            creatureFrame(body, dy = -1, accentPhase = -1).beat(WALK_STEP_MS)
        ).mergeRepeats()
        val frames = FrameCrossfade.withCrossfades(GRID, AvatarGeometry.HEIGHT, beats.map { it.points }, steps = CROSSFADE_STEPS, loop = true)
        return AvatarSequence(frames, beats.map { it.holdMs })
    }

    /**
     * Schritttakt. Deutlich schneller als der Grundtakt [BEAT_MS]: Ein Gang, dessen Schritte so
     * lange stehen wie eine Betonung in einer Reaktion, wirkt wie Zeitlupe. 110ms je Pose ergibt
     * gut vier Schritte pro Sekunde - zuegig, aber noch nicht hektisch.
     */
    private const val WALK_STEP_MS = 110L

    /**
     * Kleine Alltagsregungen zwischen den eigentlichen Handlungen - **das, was einen Bewohner von
     * einer Schleife unterscheidet.**
     *
     * Die Ruhe-Schleife jeder Spezies ist gut, aber sie ist EINE Schleife: Wer eine Weile
     * zuschaut, hat sie gesehen und erkennt ab dann ein Muster. Diese Regungen brechen das auf,
     * ohne dass dafuer eine ganze Handlung noetig waere - der Avatar tut nichts Bedeutsames, er
     * ist einfach da und lebt.
     *
     * Bewusst aus denselben Stellschrauben gebaut wie alles andere ([creatureFrame]: Versatz,
     * Fuesse, Schwanz, Akzent, Augen, Mund) statt aus neuer Pixel-Art. Damit gelten sie sofort
     * fuer alle sechs Grundformen, und wer eine siebte hinzufuegt, bekommt sie geschenkt.
     */
    enum class Fidget { LOOK_AROUND, STRETCH, YAWN, SHAKE }

    fun fidgetSequence(species: AvatarSpecies, fidget: Fidget): AvatarSequence {
        val body = AvatarBodies.forSpecies(species)
        val beats = when (fidget) {
            // Sieht sich um: lehnt sich zur einen Seite, schaut, dann zur anderen. Der Versatz um
            // eine Zelle ist auf diesem Raster genug - eine echte Kopfdrehung gaebe es nur mit
            // einem zweiten Satz gezeichneter Koepfe.
            Fidget.LOOK_AROUND -> listOf(
                creatureFrame(body, dx = -1).beat(320L),
                creatureFrame(body, dx = -1, eyeHoles = body.eyesHalf).beat(180L),
                creatureFrame(body).beat(260L),
                creatureFrame(body, dx = 1).beat(320L),
                creatureFrame(body, dx = 1, eyeHoles = body.eyesHalf).beat(180L),
                creatureFrame(body, tailWag = 1).beat(SETTLE_MS)
            )

            // Streckt sich: geht in die Hoehe, Fuesse gespreizt, Mund auf - und sackt langsam
            // zurueck. Der lange Halt oben ist die ganze Pointe.
            Fidget.STRETCH -> listOf(
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(240L),
                creatureFrame(body, dy = -1, feetSpread = 1, accentPhase = 1).beat(200L),
                creatureFrame(body, dy = -2, feetSpread = 1, accentPhase = 1, mouthHoles = body.mouthOpen).beat(460L),
                creatureFrame(body, dy = -1, feetSpread = 1).beat(220L),
                creatureFrame(body, tailWag = 1).beat(SETTLE_MS)
            )

            // Gaehnt: Lider sinken, Mund geht auf, kurzer Aufwaertsruck, dann blinzelt es sich frei.
            Fidget.YAWN -> listOf(
                creatureFrame(body, eyeHoles = body.eyesHalf).beat(280L),
                creatureFrame(body, eyeHoles = body.eyesClosed, mouthHoles = body.mouthOpen).beat(300L),
                creatureFrame(body, eyeHoles = body.eyesClosed, mouthHoles = body.mouthOpen, dy = -1).beat(440L),
                creatureFrame(body, eyeHoles = body.eyesHalf, mouthHoles = body.mouthOpen).beat(220L),
                creatureFrame(body).beat(SETTLE_MS)
            )

            // Schuettelt sich: schnelles Hin und Her, danach Ruhe. Das einzige Fidget im
            // Schnelltakt - der Kontrast zu den drei ruhigen macht es als eigene Regung erkennbar.
            Fidget.SHAKE -> listOf(
                creatureFrame(body, dx = -1, accentPhase = 1).beat(FAST_MS),
                creatureFrame(body, dx = 1, accentPhase = -1).beat(FAST_MS),
                creatureFrame(body, dx = -1, accentPhase = 1).beat(FAST_MS),
                creatureFrame(body, dx = 1, tailWag = 1).beat(FAST_MS),
                creatureFrame(body, tailWag = -1).beat(SETTLE_MS)
            )
        }.mergeRepeats()
        val frames = FrameCrossfade.withCrossfades(GRID, AvatarGeometry.HEIGHT, beats.map { it.points }, steps = CROSSFADE_STEPS, loop = false)
        return AvatarSequence(frames, beats.map { it.holdMs })
    }

    // ---- DRINK ----
    //
    // Erzaehlt jetzt einen Vorgang statt nur "Glas da, Kreatur huepft": Das Glas **leert sich
    // sichtbar** mit jedem Schluck, und der Koerper verarbeitet das Getrunkene - der Bauch
    // woelbt sich, sackt wieder zusammen, danach die Freude.
    //
    // Vorher waren beide Schlucke identisch (dasselbe volle Glas, derselbe Tropfen) - ohne
    // Fuellstand war nicht zu sehen, dass ueberhaupt etwas verschwindet, und ohne Reaktion des
    // Koerpers blieb es bei einer Geste neben einem Gegenstand.
    //
    // Der Tropfen sitzt weiterhin bei (7,4): einzige Zeile, die bei ALLEN sechs Spezies
    // nachweislich in der Luecke zwischen Glasboden (y=3) und Kopf/Kopfschmuck liegt. Weiter
    // unten verschwaende er in der gefuellten Silhouette. Deshalb faellt der Tropfen nicht bis
    // zum Mund - er erscheint einmal und ist im naechsten Bild weg, waehrend der Mund offen
    // steht. Das liest sich als Schlucken, ohne durch den Koerper hindurchzeichnen zu muessen.

    /**
     * Glas mit Fuellstand: Umriss (Waende bei x=6/9, Boden bei y=3) plus Inhalt, der von unten
     * nach oben steht. [fill] 3 = voll, 0 = leer.
     */
    private fun glass(fill: Int = 3): List<Pair<Int, Int>> {
        val outline = listOf(6 to 0, 9 to 0, 6 to 1, 9 to 1, 6 to 2, 9 to 2, 6 to 3, 7 to 3, 8 to 3, 9 to 3)
        // Innenraum ist x=7..8, y=0..2 - gefuellt wird vom Boden aufwaerts.
        val content = (0 until fill.coerceIn(0, 3)).flatMap { step ->
            val y = 2 - step
            listOf(7 to y, 8 to y)
        }
        return outline + content
    }

    /**
     * Voruebergehende Bauchwoelbung - der Koerper wird an seinen unteren, breitesten Zeilen um
     * [amount] Punkte je Seite breiter.
     *
     * Aus der Silhouette der jeweiligen Spezies berechnet statt fuer jede einzeln gezeichnet:
     * die sechs Formen sind unterschiedlich breit und hoch, ein fester Satz Zusatzpunkte saesse
     * bei der einen im Koerper und bei der anderen daneben in der Luft. So woelbt sich bei
     * jeder genau ihre eigene Bauchlinie nach aussen - auch bei GLOOP, der weder Fuesse noch
     * Schwanz hat und dessen Koerper ohnehin die einzige Bewegung ist.
     *
     * Wird als Prop mit [creatureFrame] uebergeben und folgt deshalb der Koerperbewegung.
     */
    private fun bellyBulge(body: AvatarBody, amount: Int): List<Pair<Int, Int>> {
        if (amount <= 0) return emptyList()
        val ys = body.silhouette.map { it.second }
        val minY = ys.min()
        val maxY = ys.max()
        // Untere Haelfte ohne die letzte Zeile: dort laeuft der Koerper spitz zu, eine
        // Verbreiterung saehe dort nach Fuessen aus, nicht nach Bauch.
        val bellyRows = ((minY + maxY) / 2)..(maxY - 1)
        return bellyRows.flatMap { y ->
            val row = body.silhouette.filter { it.second == y }
            if (row.isEmpty()) return@flatMap emptyList()
            val left = row.minOf { it.first }
            val right = row.maxOf { it.first }
            (1..amount).flatMap { d -> listOf((left - d) to y, (right + d) to y) }
        }
    }

    private fun drinkKeyframes(body: AvatarBody) = listOf(
        // Ansetzen: volles Glas, Mund noch zu.
        creatureFrame(body, prop = glass(3), propFollows = true).beat(),
        // Tropfen verlaesst das Glas - BEWUSST ohne Koerperversatz. Mit dy wanderte er von
        // (7,4) weg, der einzigen Zeile, die bei allen sechs Spezies frei liegt, und waere
        // wieder unsichtbar geworden (siehe AvatarAnimationsTest.drinkDropIsActuallyVisible).
        // Es liest sich ohnehin besser: erst sieht man den Tropfen, dann wird geschluckt.
        creatureFrame(body, prop = glass(3) + (7 to 4), propFollows = true).beat(FAST_MS),
        // Geschluckt: Kopf zurueck, Mund auf, Tropfen weg, Glas eine Stufe leerer.
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1, accentPhase = 1, prop = glass(2), propFollows = true).beat(SLOW_MS),
        // Absetzen.
        creatureFrame(body, prop = glass(2), propFollows = true).beat(FAST_MS),
        // Zweiter Schluck.
        creatureFrame(body, prop = glass(2) + (7 to 4), propFollows = true).beat(FAST_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1, accentPhase = -1, prop = glass(1), propFollows = true).beat(SLOW_MS),
        // Letzter Rest - danach ist das Glas leer.
        creatureFrame(body, prop = glass(1), propFollows = true).beat(FAST_MS),
        creatureFrame(body, prop = glass(1) + (7 to 4), propFollows = true).beat(FAST_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1, prop = glass(0), propFollows = true).beat(SLOW_MS),
        // Glas ist weg, der Bauch fuellt sich - in zwei Stufen, damit man das Anschwellen sieht.
        creatureFrame(body, prop = bellyBulge(body, 1), propFollows = true).beat(FAST_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen, prop = bellyBulge(body, 2), propFollows = true).beat(SLOW_MS),
        // ... und sackt wieder zusammen.
        creatureFrame(body, prop = bellyBulge(body, 1), propFollows = true).beat(FAST_MS),
        creatureFrame(body).beat(FAST_MS),
        // Zufrieden: Hopser mit Schwanzwedeln und offenem Mund.
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1, tailWag = 1, accentPhase = 1).beat(BEAT_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen, tailWag = -1, accentPhase = -1).beat(SETTLE_MS)
    )

    // ---- MOVE: Kreatur huepft seitlich hin und her (drei Hopser statt zwei), dann ein
    // grosser Abschluss-Sprung mit weiter gespreizten Fuessen ----
    // Durchgehend schnell (es ist Bewegung), nur der Schlusssprung klingt aus.
    /**
     * Staubwoelkchen beim Aufkommen, links und rechts neben den Fuessen.
     *
     * Liegt bei y=15 unterhalb jeder Silhouette (die tiefste Spezies reicht bis y=14) und ist
     * damit bei allen sechs sichtbar - dieselbe Falle wie beim Trink-Tropfen, der frueher
     * unsichtbar auf dem Koerper lag. [dx] verschiebt die Wolke mit dem Landepunkt.
     */
    private fun dust(dx: Int) = listOf((4 + dx) to 15, (11 + dx) to 15)

    private fun moveKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body).beat(FAST_MS),
        creatureFrame(body, dx = -2, dy = -2, feetSpread = 1, accentPhase = 1, mouthHoles = body.mouthOpen).beat(FAST_MS),
        // Aufkommen - Staub wirbelt auf.
        creatureFrame(body, dx = -2, prop = dust(-2)).beat(FAST_MS),
        creatureFrame(body, dx = 2, dy = -2, feetSpread = 1, accentPhase = -1, mouthHoles = body.mouthOpen).beat(FAST_MS),
        creatureFrame(body, dx = 2, prop = dust(2)).beat(FAST_MS),
        creatureFrame(body, dx = -1, dy = -2, feetSpread = 1, accentPhase = 1, mouthHoles = body.mouthOpen).beat(FAST_MS),
        creatureFrame(body, dx = -1, prop = dust(-1)).beat(FAST_MS),
        // Grosser Abschluss-Sprung, hoeher als die davor.
        creatureFrame(body, dy = -3, feetSpread = 2, accentPhase = -1, mouthHoles = body.mouthOpen).beat(BEAT_MS),
        creatureFrame(body, prop = dust(0)).beat(SETTLE_MS)
    )

    // ---- GENERAL: kleine Glocke schwingt mehrfach ueber dem Kopf, Kreatur nickt kurz
    // aufmerksam (dies ist die breite Fallback-Reaktion fuer alle Bibliotheks-Animationen
    // ohne eigenen AnimationType/Sonderfall - bewusst schlicht gehalten) ----
    private fun bell(dx: Int) =
        listOf(7 + dx to 0, 6 + dx to 1, 8 + dx to 1, 6 + dx to 2, 8 + dx to 2, 5 + dx to 3, 6 + dx to 3, 7 + dx to 3, 8 + dx to 3, 9 + dx to 3)
    // Die Glocke haengt in der Umgebung, nicht in der Hand - sie bleibt bewusst stehen. Das
    // Schwingen wird nach aussen hin langsamer (Ausschwingen).
    /**
     * Schallwellen neben der Glocke - laufen nach aussen und verklingen. Macht aus einer
     * schwingenden Form ein Laeuten: Bewegung allein sagt nicht, dass etwas zu hoeren ist.
     * [ring] 0 = eng an der Glocke, 2 = weit draussen und schon fast weg.
     */
    private fun bellWaves(ring: Int): List<Pair<Int, Int>> = when (ring) {
        0 -> listOf(4 to 1, 10 to 1)
        1 -> listOf(3 to 1, 11 to 1, 3 to 2, 11 to 2)
        else -> listOf(2 to 2, 12 to 2)
    }

    private fun generalKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, prop = bell(0)).beat(FAST_MS),
        // Erster Schlag - Wellen laufen nach aussen.
        creatureFrame(body, accentPhase = 1, prop = bell(-2) + bellWaves(0)).beat(FAST_MS),
        creatureFrame(body, accentPhase = -1, prop = bell(2) + bellWaves(1)).beat(FAST_MS),
        creatureFrame(body, accentPhase = 1, prop = bell(-1) + bellWaves(2)).beat(FAST_MS),
        // Zweiter, schwaecherer Schlag.
        creatureFrame(body, accentPhase = -1, prop = bell(1) + bellWaves(0)).beat(FAST_MS),
        creatureFrame(body, prop = bell(0) + bellWaves(1)).beat(BEAT_MS),
        // Aufmerksam werden: Kopf hoch, Mund auf.
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen, prop = bell(0)).beat(BEAT_MS),
        creatureFrame(body).beat(SETTLE_MS)
    )

    // ---- REST: Dampf steigt in zwei Stufen auf, Kreatur nippt, schliesst die Augen und
    // klingt mit einem langsamen Schwanz-Wiegen aus statt dem sonst ueblichen Hopser -
    // passt besser zu "entspannen" als ein energischer Sprung ----
    // Fehlerkorrektur: die letzten beiden Frames unterschieden sich nur durch tailWag
    // (1 vs -1) - bei Starlet/Gloop/Hootlet, die laut AvatarBodies GAR KEINEN Schwanz
    // haben (tail = noTail), war das dadurch ein kompletter Stillstand (0 Pixel
    // Unterschied, per Cross-Species-Check bestaetigt). Jetzt zusaetzlich ein sanftes
    // Ausatmen (mouthOpen) im letzten Frame - eine Mund-Aenderung wirkt sich bei JEDER
    // Spezies aus, unabhaengig von Fuessen/Schwanz.
    /**
     * Tasse mit Fuellstand und Henkel - dieselbe Idee wie beim Glas (siehe [glass]): Ohne
     * sinkenden Pegel war nicht zu sehen, dass ueberhaupt etwas getrunken wird; beide Nipper
     * zeigten exakt dasselbe Bild. [fill] 2 = voll, 0 = leer.
     */
    private fun mug(fill: Int = 2): List<Pair<Int, Int>> {
        val outline = listOf(6 to 1, 9 to 1, 6 to 2, 9 to 2, 6 to 3, 7 to 3, 8 to 3, 9 to 3, 10 to 2)
        val content = (0 until fill.coerceIn(0, 2)).flatMap { step ->
            val y = 2 - step
            listOf(7 to y, 8 to y)
        }
        return outline + content
    }

    /** Dampf ueber der Tasse - steigt auf und loest sich auf. [stage] 0 = dicht, 2 = fast weg. */
    private fun steam(stage: Int): List<Pair<Int, Int>> = when (stage) {
        0 -> listOf(7 to 0, 8 to 0)
        1 -> listOf(6 to 0, 9 to 0)
        else -> emptyList()
    }

    // Durchgehend langsam - "ausruhen" darf sich nicht hetzen anfuehlen. Die Tasse wird
    // gehalten und geht beim Nippen mit; der Dampf ist Umgebung und bleibt stehen.
    private fun restKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, prop = mug(2), propFollows = true).beat(SLOW_MS),
        creatureFrame(body, prop = mug(2) + steam(0), propFollows = true).beat(SLOW_MS),
        creatureFrame(body, prop = mug(2) + steam(1), propFollows = true).beat(SLOW_MS),
        // Erster Schluck - Tasse leert sich sichtbar.
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1, accentPhase = 1, prop = mug(1), propFollows = true).beat(SLOW_MS),
        creatureFrame(body, prop = mug(1) + steam(0), propFollows = true).beat(SLOW_MS),
        // Zweiter Schluck - Tasse leer.
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1, accentPhase = -1, prop = mug(0), propFollows = true).beat(SLOW_MS),
        // Abstellen, Augen fallen zu - der arteigene ruhige Abschluss uebernimmt danach.
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = mug(0), propFollows = true).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesClosed).beat(SETTLE_MS)
    )

    // ---- WORK: kleiner Laptop, mehrere Tipp-Frames mit blinkender Ziffer, dann
    // Streck-Pause (Fuesse gespreizt, Kreatur reckt sich) vor dem Schlusshopser ----
    // Fehlerkorrektur: Streck-Frame und Schlusshopser unterschieden sich nur durch
    // feetSpread/tailWag - fuer GLOOP (laut AvatarBodies WEDER Fuesse NOCH Schwanz: feet =
    // noFeet, tail = noTail) war das ein kompletter Stillstand. Jetzt zusaetzlich ein
    // zufriedenes "Ahh" (mouthOpen) im Schlussframe, wirkt bei jeder Spezies.
    private fun laptopShell() = listOf(
        5 to 0, 6 to 0, 7 to 0, 8 to 0, 9 to 0, 10 to 0,
        5 to 1, 10 to 1, 5 to 2, 10 to 2,
        5 to 3, 6 to 3, 7 to 3, 8 to 3, 9 to 3, 10 to 3
    )

    private fun laptop(digitDx: Int) = laptopShell() + listOf((7 + digitDx) to 1, (7 + digitDx) to 2)

    /**
     * Laptop mit Fortschrittsbalken auf dem Bildschirm - [done] 0..4 Pixel, von links nach
     * rechts. Die blinkende Ziffer allein zeigte Betrieb, aber kein Vorankommen; ein wachsender
     * Balken macht aus "es tippt" ein "es wird fertig".
     */
    private fun laptopProgress(done: Int): List<Pair<Int, Int>> =
        laptopShell() + (0 until done.coerceIn(0, 4)).map { (6 + it) to 2 }

    /** Erledigt: Haken auf dem Bildschirm statt Balken. */
    private fun laptopDone() = laptopShell() + listOf(6 to 2, 7 to 2, 8 to 1, 9 to 1)
    // Tippen ist hektisch (FAST), das Recken danach bewusst lang - der Kontrast macht die
    // Pause erst als Pause lesbar. Der Laptop wird gehalten und geht mit.
    // Tippen ist hektisch: der Koerper wippt im Takt um ein Pixel, waehrend der Balken waechst.
    // Das Wippen wirkt bei jeder Spezies, anders als ein Akzent allein.
    private fun workKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, prop = laptopProgress(0), propFollows = true).beat(FAST_MS),
        creatureFrame(body, dy = -1, accentPhase = 1, prop = laptopProgress(1), propFollows = true).beat(FAST_MS),
        creatureFrame(body, accentPhase = -1, prop = laptopProgress(1), propFollows = true).beat(FAST_MS),
        creatureFrame(body, dy = -1, accentPhase = 1, prop = laptopProgress(2), propFollows = true).beat(FAST_MS),
        creatureFrame(body, accentPhase = -1, prop = laptopProgress(3), propFollows = true).beat(FAST_MS),
        creatureFrame(body, dy = -1, accentPhase = 1, prop = laptopProgress(4), propFollows = true).beat(FAST_MS),
        // Fertig - der Haken steht kurz allein da, damit er als Ergebnis lesbar ist.
        creatureFrame(body, mouthHoles = body.mouthOpen, prop = laptopDone(), propFollows = true).beat(SLOW_MS),
        // Zurueckgelehnt, gestreckt - der arteigene Abschluss uebernimmt.
        creatureFrame(body, dy = -1, feetSpread = 1, accentPhase = 1).beat(SLOW_MS),
        creatureFrame(body).beat(SETTLE_MS)
    )

    // ---- MINDFULNESS: Aura "atmet" durch zwei volle Ein-/Ausatem-Zyklen (statt nur
    // einmal klein->gross), dann Augen zu und sanftes Ausklingen ohne harten Hopser ----
    private fun aura(big: Boolean) =
        if (!big) listOf(7 to 2, 8 to 2, 6 to 3, 9 to 3) else listOf(7 to 0, 8 to 0, 5 to 2, 10 to 2, 5 to 4, 10 to 4)
    // Atem-Tempo: durchgehend langsam, der Schlussframe haelt am laengsten. Die Aura ist
    // Umgebung und bleibt stehen.
    // Der Koerper atmet mit: beim Einatmen weitet er sich (dieselbe Berechnung wie die
    // Bauchwoelbung beim Trinken, siehe [bellyBulge]), beim Ausatmen faellt er zurueck. Vorher
    // pulsierte nur die Aura daneben, waehrend die Kreatur selbst stillstand - obwohl das Atmen
    // genau das ist, worum es bei Achtsamkeit geht. Augen von Anfang an geschlossen.
    private fun mindfulnessKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, eyeHoles = body.eyesClosed, prop = aura(false)).beat(SLOW_MS),
        // Einatmen - Koerper weitet sich, Aura geht mit auf.
        creatureFrame(body, eyeHoles = body.eyesClosed, dy = -1, prop = aura(true) + bellyBulge(body, 1)).beat(SLOW_MS),
        // Ausatmen.
        creatureFrame(body, eyeHoles = body.eyesClosed, prop = aura(false)).beat(SLOW_MS),
        // Zweiter Zug, tiefer.
        creatureFrame(body, eyeHoles = body.eyesClosed, dy = -1, prop = aura(true) + bellyBulge(body, 2)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesClosed, prop = aura(false)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesClosed).beat(SETTLE_MS)
    )

    // ---- LOVE: grosses Herz erscheint, mehrere kleine Herzen steigen nacheinander auf
    // (statt nur eines), Kreatur wackelt froh mit dem Schwanz, Schlusshopser ----
    // Fehlerkorrektur: die "aufsteigenden" Mini-Herzen sassen vorher bei (7,7)/(8,7) und
    // (7,5)/(8,5) - beides Zeilen, die durch den dy=-1-Versatz vom Koerper selbst schon
    // voll ausgefuellt sind (bei ALLEN sechs Spezies, per ASCII-Render bestaetigt), die
    // Herzen waren dadurch unsichtbar. Jetzt in der Zone y<=3, dort ist bei jeder Spezies
    // (auch nach dy=-1) nachweislich noch Platz frei.
    private fun heart(big: Boolean) = if (big) listOf(
        6 to 1, 9 to 1, 5 to 2, 7 to 2, 8 to 2, 10 to 2, 5 to 3, 10 to 3, 6 to 4, 9 to 4, 7 to 5, 8 to 5
    ) else listOf(6 to 2, 9 to 2, 6 to 3, 7 to 3, 8 to 3, 9 to 3, 7 to 4, 8 to 4)
    // Die Herzen sind Umgebung (sie steigen selbst auf) und bleiben deshalb stehen. Das
    // Aufsteigen wird nach oben hin langsamer, als wuerden sie leichter.
    // Das Herz schlaegt jetzt wirklich: gross-klein-gross-klein im Puls, bevor die kleinen
    // Herzen aufsteigen. Vorher erschien es einmal gross, einmal klein und war dann weg - das
    // las sich als Ein- und Ausblenden, nicht als Schlagen. Der Puls ist ausserdem das einzige
    // Element, das ohne jede Koerperbewegung auskommt und deshalb bei allen sechs Spezies exakt
    // gleich gut funktioniert.
    private fun loveKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, prop = heart(true)).beat(BEAT_MS),
        creatureFrame(body, prop = heart(false)).beat(FAST_MS),
        creatureFrame(body, prop = heart(true)).beat(FAST_MS),
        creatureFrame(body, prop = heart(false)).beat(FAST_MS),
        creatureFrame(body, accentPhase = 1, prop = heart(true)).beat(BEAT_MS),
        // Die Herzen loesen sich und steigen auf - nach oben hin langsamer, als wuerden sie
        // leichter. Alles in der Zone y<=3, wo bei jeder Spezies nachweislich Platz frei ist.
        creatureFrame(body, dy = -1, accentPhase = -1, prop = listOf(7 to 3, 8 to 3)).beat(BEAT_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen, accentPhase = 1, prop = listOf(6 to 2, 9 to 2)).beat(BEAT_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1, accentPhase = -1, prop = listOf(6 to 1, 9 to 1)).beat(SLOW_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen).beat(SETTLE_MS)
    )

    // ---- SLEEP: Zzz-Punkt steigt in drei Stufen, Augen fallen nach und nach zu, am Ende
    // hebt sich die Kreatur nur kurz und sinkt wieder zurueck (statt oben zu bleiben wie
    // bei den energischen Reaktionen) - sie soll ja einschlafen, nicht wegspringen ----
    private fun moon() = listOf(8 to 0, 9 to 0, 7 to 1, 7 to 2, 8 to 3, 9 to 3)
    // Wird zum Ende hin immer langsamer - das Einschlafen selbst ist die Bewegung.
    // Die Kreatur sackt Stufe um Stufe tiefer, waehrend die Zzz aufsteigen - die
    // Gegenlaeufigkeit ist der Kern: was einschlaeft, geht nach unten, was verfliegt, nach oben.
    // Vorher blieb der Koerper auf derselben Hoehe und hob sich am Ende sogar noch, was eher
    // nach Aufwachen aussah. Die Lider fallen dabei in drei Stufen (offen, halb, zu) statt
    // schlagartig.
    private fun sleepKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, accentPhase = 1, prop = moon() + (11 to 2)).beat(BEAT_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = moon() + (11 to 1)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesClosed, dy = 1, accentPhase = -1, prop = moon() + (11 to 0)).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesClosed, dy = 1, prop = moon() + (12 to 0)).beat(SLOW_MS),
        // Ganz weggesackt.
        creatureFrame(body, eyeHoles = body.eyesClosed, dy = 2, prop = moon()).beat(SLOW_MS),
        creatureFrame(body, eyeHoles = body.eyesClosed, dy = 2, accentPhase = -1, prop = moon()).beat(SETTLE_MS)
    )

    // ---- MEDICINE: kleines Kreuz wandert in zwei Stufen heran (statt einem Sprung),
    // Kreatur nimmt es, nickt kurz zustimmend und hopst zufrieden ----
    // Fehlerkorrektur: Nick-Frame und Schlusshopser unterschieden sich nur durch tailWag -
    // bei Starlet/Gloop/Hootlet (kein Schwanz) ein Stillstand. Jetzt zusaetzlich ein
    // zufriedenes "mmh" (mouthOpen) im Schlussframe.
    private fun cross(y: Int) =
        listOf(7 to y, 8 to y, 7 to (y + 1), 8 to (y + 1), 7 to (y + 2), 8 to (y + 2), 5 to (y + 1), 6 to (y + 1), 9 to (y + 1), 10 to (y + 1))
    // Das Kreuz schwebt heran (Umgebung, bleibt stehen), das Schlucken haelt kurz an.
    //
    // Danach das Schuetteln: Medizin schmeckt bitter, und ein kurzes Ruetteln um je ein Pixel
    // nach links und rechts erzaehlt das ohne ein einziges zusaetzliches Motiv. Es ist auch die
    // Bewegung, die bei jeder Spezies gleich gut funktioniert - der ganze Koerper verschiebt
    // sich, unabhaengig von Fuessen, Schwanz oder Fluegeln. Erst danach kommt die Erleichterung.
    private fun medicineKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, prop = cross(0)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1, prop = cross(2)).beat(FAST_MS),
        // Mund auf, Tablette faellt hinein.
        creatureFrame(body, mouthHoles = body.mouthOpen, accentPhase = -1, prop = cross(4)).beat(FAST_MS),
        // Geschluckt - das Kreuz ist weg, Kopf kurz zurueck.
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1).beat(SLOW_MS),
        // Bitter: schnelles Schuetteln, Augen zusammengekniffen.
        creatureFrame(body, eyeHoles = body.eyesClosed, dx = -1).beat(FAST_MS),
        creatureFrame(body, eyeHoles = body.eyesClosed, dx = 1).beat(FAST_MS),
        creatureFrame(body, eyeHoles = body.eyesClosed, dx = -1).beat(FAST_MS),
        // Vorbei - Augen wieder auf, durchatmen.
        creatureFrame(body, eyeHoles = body.eyesHalf).beat(BEAT_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen).beat(SETTLE_MS)
    )

    // ---- BOOK: kleines Buch klappt auf, Kreatur "liest" - dabei bleiben die Augen OFFEN
    // (vorher ein Logikfehler: die Lese-Frame hatte body.eyesClosed gesetzt, die Kreatur
    // "las" also mit geschlossenen Augen). Geschlossen werden die Augen erst ganz am Ende,
    // als zufriedenes Blinzeln NACHDEM das Buch schon wieder zugeklappt ist. Zusaetzlich
    // ein Seitenumblaettern zwischen den beiden Lese-Frames fuer mehr Eigenleben ----
    private fun bookClosed() = listOf(6 to 0, 7 to 0, 8 to 0, 9 to 0, 6 to 1, 9 to 1, 6 to 2, 9 to 2, 6 to 3, 7 to 3, 8 to 3, 9 to 3)
    private fun bookOpen(pageTurned: Boolean = false) = if (!pageTurned) listOf(
        4 to 0, 11 to 0, 4 to 1, 6 to 1, 9 to 1, 11 to 1,
        4 to 2, 6 to 2, 9 to 2, 11 to 2, 4 to 3, 5 to 3, 6 to 3, 9 to 3, 10 to 3, 11 to 3
    ) else listOf(
        4 to 0, 11 to 0, 4 to 1, 7 to 1, 8 to 1, 11 to 1,
        4 to 2, 7 to 2, 8 to 2, 11 to 2, 4 to 3, 5 to 3, 6 to 3, 9 to 3, 10 to 3, 11 to 3
    )
    // Fehlerkorrektur: der zweite "Lese"-Frame war zunaechst nur per tailWag von seinem
    // Vorgaenger unterschieden - bei Starlet/Gloop/Hootlet (kein Schwanz) blieb das Bild
    // dadurch 120ms lang komplett unveraendert (per Cross-Species-Check bestaetigt, auch
    // nachdem die urspruengliche tailWag-Ergaenzung schon eingebaut war). Jetzt zeigt
    // dieser Frame stattdessen schon das umgeblaetterte Blatt (bookOpen(pageTurned=true)) -
    // eine echte Prop-Aenderung, die bei JEDER Spezies wirkt.
    /**
     * Das Blatt auf halbem Weg - steht senkrecht in der Mitte, waehrend es umschlaegt. Ohne
     * diesen Zwischenschritt sprang die Seite von links nach rechts, was eher nach einem
     * Bildwechsel aussah als nach Umblaettern.
     */
    private fun bookPageInFlight() = listOf(
        4 to 0, 11 to 0, 4 to 1, 11 to 1, 4 to 2, 11 to 2,
        4 to 3, 5 to 3, 6 to 3, 9 to 3, 10 to 3, 11 to 3,
        7 to 0, 7 to 1, 7 to 2
    )

    // Das Buch wird gehalten und geht mit. Lesen braucht Zeit (SLOW), das Aufklappen und
    // Umblaettern ist schnell.
    //
    // Der Kopf wandert beim Lesen um ein Pixel nach links und rechts - das liest sich als
    // Blick, der den Zeilen folgt, und wirkt bei JEDER Spezies (anders als Schwanzwedeln,
    // das drei der sechs gar nicht haben). Zwei Seiten werden umgeblaettert statt einer, mit
    // einem Zwischenbild fuer das aufgestellte Blatt.
    private fun bookKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, prop = bookClosed(), propFollows = true).beat(BEAT_MS),
        creatureFrame(body, prop = bookOpen(), propFollows = true).beat(FAST_MS),
        // Lesen: Blick nach links, dann nach rechts.
        creatureFrame(body, dx = -1, prop = bookOpen(), propFollows = true).beat(SLOW_MS),
        creatureFrame(body, dx = 1, prop = bookOpen(), propFollows = true).beat(SLOW_MS),
        // Erste Seite umblaettern - mit aufgestelltem Blatt dazwischen.
        creatureFrame(body, accentPhase = 1, prop = bookPageInFlight(), propFollows = true).beat(FAST_MS),
        creatureFrame(body, prop = bookOpen(pageTurned = true), propFollows = true).beat(FAST_MS),
        // Weiterlesen.
        creatureFrame(body, dx = -1, prop = bookOpen(pageTurned = true), propFollows = true).beat(SLOW_MS),
        creatureFrame(body, dx = 1, prop = bookOpen(pageTurned = true), propFollows = true).beat(SLOW_MS),
        // Zweite Seite.
        creatureFrame(body, accentPhase = -1, prop = bookPageInFlight(), propFollows = true).beat(FAST_MS),
        creatureFrame(body, prop = bookOpen(), propFollows = true).beat(BEAT_MS),
        // Zuklappen - der arteigene Abschluss uebernimmt.
        creatureFrame(body, eyeHoles = body.eyesHalf, prop = bookClosed(), propFollows = true).beat(BEAT_MS),
        creatureFrame(body).beat(SETTLE_MS)
    )

    // ---- CREATIVITY: Funkenkreis waechst, dann ein einzelner heller "Geistesblitz" ueber
    // dem Kopf, Kreatur reagiert ueberrascht-froh (Mund auf) mit kleinen Funken, wackelt
    // zufrieden mit dem Schwanz ----
    // Fehlerkorrektur: dieselbe Unsichtbarkeits-Falle wie bei LOVE (siehe dortiger
    // Kommentar) - die Funken bei (7,7)/(8,7) und (6,6)/(9,6) verschwanden hinter dem
    // dy=-1-verschobenen Koerper. Gleiche Korrektur: y<=3.
    private fun sparkle(big: Boolean) =
        if (!big) listOf(7 to 2, 5 to 4, 10 to 4, 7 to 6) else listOf(7 to 0, 3 to 3, 12 to 3, 7 to 6, 4 to 6, 11 to 6)
    // Der "Geistesblitz" (Frame 3) steht bewusst KURZ und allein da - erst diese Pause vor der
    // Reaktion macht ihn als Einfall lesbar statt als weiteren Funken.
    /**
     * Der Einfall platzt auf - Funken fliegen faecherfoermig nach aussen.
     *
     * Bleibt vollstaendig in der Zone y<=3: nur dort ist bei jeder Spezies auch nach dem
     * dy=-1-Versatz nachweislich Platz frei (siehe Kommentar zu [sparkle]). Ein erster Entwurf
     * hatte Funken auf y=4/y=5 - die waeren bei angehobenem Koerper hinter der Silhouette
     * verschwunden, exakt die Falle, die AvatarAnimationsTest.creativitySparksAreActuallyVisible
     * abfaengt.
     */
    private fun sparkBurst() = listOf(
        7 to 0, 8 to 0,
        5 to 1, 10 to 1,
        3 to 2, 12 to 2,
        2 to 3, 7 to 3, 8 to 3, 13 to 3
    )

    // Der "Geistesblitz" (Frame 3) steht bewusst KURZ und allein da - erst diese Pause vor der
    // Reaktion macht ihn als Einfall lesbar statt als weiteren Funken. Danach platzt er auf:
    // vorher blieben es einzelne Punkte, jetzt fliegen die Funken sichtbar auseinander.
    private fun creativityKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, prop = sparkle(false)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1, prop = sparkle(true)).beat(BEAT_MS),
        // Alles verschwindet bis auf den einen Punkt - der Einfall.
        creatureFrame(body, prop = listOf(7 to 0)).beat(FAST_MS),
        // Aufplatzen.
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1, prop = sparkBurst()).beat(SLOW_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen, prop = sparkle(true)).beat(BEAT_MS),
        creatureFrame(body, prop = sparkle(false)).beat(SETTLE_MS)
    )

    // ---- FOCUS: Sucherwinkel ziehen sich in DREI Stufen zusammen (statt zwei) fuer ein
    // echtes "Zoom"-Gefuehl, dann "locked on" (Mund kurz auf) und Schlusshopser ----
    private fun focusCorners(stage: Int) = when (stage) {
        0 -> listOf(3 to 0, 4 to 0, 3 to 1, 11 to 0, 10 to 0, 11 to 1, 3 to 7, 4 to 7, 3 to 6, 11 to 7, 10 to 7, 11 to 6)
        1 -> listOf(5 to 1, 6 to 1, 5 to 2, 10 to 1, 9 to 1, 10 to 2, 5 to 6, 6 to 6, 5 to 5, 10 to 6, 9 to 6, 10 to 5)
        else -> listOf(6 to 2, 7 to 2, 6 to 3, 9 to 2, 10 to 2, 10 to 3, 6 to 6, 7 to 6, 6 to 5, 10 to 6, 9 to 6, 10 to 5)
    }
    // Der Zoom schnappt zu (FAST), der "locked on"-Moment haelt dagegen - Zuschnappen ohne
    // anschliessende Pause laese sich nicht als Einrasten lesen.
    // Nach dem Zuschnappen blitzen die Winkel einmal auf und verschwinden dann - das Aufblitzen
    // ist der Moment des Einrastens. Vorher verschwanden sie einfach, wodurch der Zoom ins Leere
    // lief statt zu einem Ergebnis zu kommen.
    private fun focusKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, prop = focusCorners(0)).beat(FAST_MS),
        creatureFrame(body, accentPhase = 1, prop = focusCorners(1)).beat(FAST_MS),
        creatureFrame(body, accentPhase = -1, prop = focusCorners(2)).beat(FAST_MS),
        // Eingerastet: Winkel blitzen zusammen mit den weiten auf.
        creatureFrame(body, prop = focusCorners(2) + focusCorners(0)).beat(FAST_MS),
        creatureFrame(body, prop = focusCorners(2)).beat(SLOW_MS),
        // Winkel weg, Blick nach vorn.
        creatureFrame(body, dy = -1, mouthHoles = body.mouthOpen).beat(SLOW_MS),
        creatureFrame(body).beat(SETTLE_MS)
    )

    // ---- ROCKET (individuelle Reaktion fuer die Bibliotheks-Animation "Rocket", siehe
    // DefaultLibraryAnimations.rocketFrames): statt nur ein Prop danebenzuzeigen, wird hier
    // der ganze Avatar zur Rakete - duckt sich, zuendet, steigt auf, dreht einen Looping und
    // fliegt dann endgueltig aus dem Bild.
    //
    // Die grosse Reise (hoch, Looping quer ueber den Bildschirm, oben raus) passiert NICHT
    // hier im 16x16-Sprite-Raster - das ist nur so gross wie der Avatar selbst, ein
    // Looping darin waere kaum als solches erkennbar und Punkte, die das Raster verlassen,
    // wuerden schlicht unsichtbar (Bugreport: "Avatar verlaesst ein kleines Viereck um sich
    // herum und ist dann weg", noch bevor er wirklich quer durchs Bild geflogen ist). Die
    // grosse Bewegung kommt stattdessen aus [rocketFlightOffsets] - Bruchteile der
    // tatsaechlichen Bildschirmgroesse, mit denen ui/DockScreen.kt die Sprite-Box selbst
    // (also die Position auf dem ganzen Handybildschirm, nicht nur im 16x16-Raster) parallel
    // zu diesen Keyframes verschiebt. Hier drin bleiben dx/dy deshalb klein (Wackeln/Neigen
    // der Figur waehrend des Flugs), das grosse Ausmass des Loopings entsteht erst durch die
    // Kombination mit dem Screen-Space-Pfad.
    private fun flameTrail(dx: Int, dy: Int, size: Int): List<Pair<Int, Int>> {
        if (size <= 0) return emptyList()
        val base = listOf(6 to 15, 7 to 15, 8 to 15, 9 to 15)
        val mid = listOf(6 to 16, 7 to 16, 8 to 16, 9 to 16)
        val tip = listOf(7 to 17, 8 to 17)
        val points = when {
            size <= 1 -> base
            size == 2 -> base + mid
            else -> base + mid + tip
        }
        return points.map { (x, y) -> (x + dx) to (y + dy) }
    }

    // Fehlerkorrektur: Frame 2->3 unterschieden sich nur durch tailWag (bei Starlet/Gloop/
    // Hootlet ohne Schwanz wirkungslos) UND eine Flammengroessen-Stufe, deren "Spitze"
    // bei dy=-1 exakt eine Zeile unterhalb des 16x16-Rasters liegt (also ebenfalls
    // unsichtbar) - beides zusammen ergab einen kompletten Stillstand fuer diese drei
    // Spezies (per Cross-Species-Check bestaetigt). Jetzt steigt dy hier schon einen
    // Schritt frueher auf -2 - eine echte, spezies-unabhaengige Verschiebung.
    // Timing erzaehlt den Start mit: das Ducken haelt an (Anspannung), das Zuenden schnappt
    // los, der Flug laeuft dann gleichmaessig schnell durch. Der leere Schlussframe braucht
    // keine Standzeit mehr - da ist der Avatar schon weg.
    private fun rocketReactionKeyframes(body: AvatarBody) = listOf(
        (creatureFrame(body, eyeHoles = body.eyesClosed, feetSpread = -1, dy = 1, accentPhase = -1)).beat(SLOW_MS),
        (creatureFrame(body, mouthHoles = body.mouthOpen, feetSpread = 1, dy = -1, accentPhase = 1) + flameTrail(0, -1, 1)).beat(FAST_MS),
        (creatureFrame(body, dy = -1, tailWag = 1, accentPhase = 1) + flameTrail(0, -1, 2)).beat(FAST_MS),
        (creatureFrame(body, dy = -2, tailWag = -1, accentPhase = -1) + flameTrail(0, -2, 3)).beat(FAST_MS),
        // -- Looping (die Kurve selbst kommt aus rocketFlightOffsets, hier nur Neigung) --
        (creatureFrame(body, dx = 1, dy = -1, tailWag = 1, accentPhase = 1) + flameTrail(1, -1, 3)).beat(FAST_MS),
        (creatureFrame(body, dx = 1, dy = -2, tailWag = -1, accentPhase = -1) + flameTrail(1, -2, 2)).beat(FAST_MS),
        (creatureFrame(body, dy = -2, tailWag = 1, accentPhase = 1) + flameTrail(0, -2, 2)).beat(FAST_MS),
        (creatureFrame(body, dx = -1, dy = -2, tailWag = -1, accentPhase = -1) + flameTrail(-1, -2, 2)).beat(FAST_MS),
        (creatureFrame(body, dx = -1, dy = -1, tailWag = 1, accentPhase = 1) + flameTrail(-1, -1, 3)).beat(FAST_MS),
        // -- aus dem Looping heraus, endgueltig weg --
        (creatureFrame(body, dy = -1, tailWag = -1, accentPhase = -1) + flameTrail(0, -1, 2)).beat(FAST_MS),
        (creatureFrame(body, dy = -1, tailWag = 1, accentPhase = 1) + flameTrail(0, -1, 1)).beat(FAST_MS),
        emptyList<Pair<Int, Int>>().beat(FAST_MS)
    )

    /**
     * Screen-Space-Flugbahn fuer die Rocket-Reaktion, ein Eintrag pro Keyframe aus
     * [rocketReactionKeyframes] (12 Eintraege, gleiche Reihenfolge/Laenge). Jeder Eintrag ist
     * (dxFraction, dyFraction) - Verschiebung relativ zur Startposition, ausgedrueckt als
     * Bruchteil der Bildschirmbreite/-hoehe (siehe ui/DockScreen.kt, wo das mit
     * maxWidthPx/maxHeightPx multipliziert und auf die Sprite-Box angewendet wird). Form:
     * hoch, Bogen nach rechts, ueber den Scheitel, Bogen nach links (das "Looping"), dann
     * weiter hoch bis deutlich ueber den oberen Bildschirmrand hinaus (dyFraction -1.6, damit
     * das Verschwinden unabhaengig von der zufaelligen Spawn-Position sicher ausserhalb des
     * sichtbaren Bereichs liegt statt nur knapp).
     *
     * Timing-Korrektur: die drei Werte nach dem Looping (Index 9-11) sprangen vorher
     * -0.45/-0.30/-0.40 pro Schritt - ein deutlicher Ausreisser direkt nach dem gleichmaessig
     * getakteten Looping (dort ~0.10-0.20 pro Schritt), das wirkte wie ein Ruckler genau beim
     * Verlassen der Schleife. Jetzt steigt der Schritt pro Frame gleichmaessig an
     * (-0.25/-0.35/-0.55, wie ein beschleunigender Abgang) statt gross-klein-gross zu springen.
     */
    fun rocketFlightOffsets(): List<Pair<Float, Float>> = listOf(
        0f to 0f,
        0f to -0.03f,
        0.05f to -0.15f,
        0.10f to -0.30f,
        0.18f to -0.45f,
        0.15f to -0.65f,
        0f to -0.75f,
        -0.15f to -0.65f,
        -0.12f to -0.45f,
        -0.05f to -0.70f,
        0f to -1.05f,
        0f to -1.60f
    )

    /**
     * Spezies-eigene Freuden-Reaktion - was diese Kreatur tut, wenn sie etwas bekommt, das keine
     * eigene Choreografie mitbringt.
     *
     * Vorher liefen alle Bibliotheks-Animationen in dieselbe generische Reaktion (Funke, Hopser).
     * Damit war die Wahl des Avatars fuer die Reaktion belanglos - obwohl gerade hier der
     * Charakter sichtbar werden koennte. Jede Spezies nutzt jetzt, was ihre Anatomie hergibt:
     *
     * - **GLOOP** hat weder Fuesse noch Schwanz und kann deshalb nicht huepfen - dafuer ist er
     *   der Einzige, dessen Koerper selbst arbeitet. Er quillt gross auf, schwappt seitlich
     *   ueber und faellt zurueck: eine grosse, alberne Bewegung genau dort, wo die anderen
     *   kleine machen.
     * - **PUFFLING** dreht sich vor Freude im Kreis (seitliches Wippen mit Schwanzwedeln).
     * - **STARLET** laesst den Sternzopf funkeln und macht grosse Augen.
     * - **WYRMLING** hebt kurz ab - Fluegelschlag mit Steigflug.
     * - **FENNEC** macht einen schnellen Doppelsprung, Ohren nach vorn.
     * - **HOOTLET** plustert sich auf und wippt bedaechtig.
     */
    private fun speciesFlourish(species: AvatarSpecies, body: AvatarBody): List<Beat> = when (species) {
        AvatarSpecies.GLOOP -> listOf(
            creatureFrame(body, mouthHoles = body.mouthOpen).beat(FAST_MS),
            // Aufquellen: Akzent (Seitenwuelste) plus gestreckter Koerper.
            creatureFrame(body, accentPhase = 1, dy = -1, mouthHoles = body.mouthOpen).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1, dx = -2, dy = -2, mouthHoles = body.mouthOpen).beat(BEAT_MS),
            // Ueberschwappen nach rechts - weit ueber die eigene Grundflaeche hinaus.
            creatureFrame(body, accentPhase = 1, dx = 3, dy = -1, mouthHoles = body.mouthOpen).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1, dx = -3, dy = 0, mouthHoles = body.mouthOpen).beat(BEAT_MS),
            // Zusammenfallen und nachwabbeln.
            creatureFrame(body, accentPhase = -1, dy = 1).beat(SLOW_MS),
            creatureFrame(body, accentPhase = 1, dy = 0).beat(FAST_MS),
            creatureFrame(body, eyeHoles = body.eyesHalf).beat(SETTLE_MS)
        )

        AvatarSpecies.PUFFLING -> listOf(
            creatureFrame(body, mouthHoles = body.mouthOpen).beat(FAST_MS),
            creatureFrame(body, dx = -2, dy = -1, tailWag = 1, feetSpread = 1, mouthHoles = body.mouthOpen).beat(FAST_MS),
            creatureFrame(body, dx = 2, dy = -1, tailWag = -1, feetSpread = 1, mouthHoles = body.mouthOpen).beat(FAST_MS),
            creatureFrame(body, dx = -2, dy = -1, tailWag = 1, feetSpread = 1).beat(FAST_MS),
            creatureFrame(body, dy = -2, tailWag = -1, accentPhase = 1).beat(BEAT_MS),
            creatureFrame(body, tailWag = 1).beat(SETTLE_MS)
        )

        AvatarSpecies.STARLET -> listOf(
            creatureFrame(body, mouthHoles = body.mouthOpen).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1, mouthHoles = body.mouthOpen, prop = listOf(3 to 2, 12 to 2)).beat(FAST_MS),
            creatureFrame(body, accentPhase = -1, mouthHoles = body.mouthOpen, prop = listOf(2 to 4, 13 to 4, 7 to 0, 8 to 0)).beat(FAST_MS),
            creatureFrame(body, dy = -2, accentPhase = 1, prop = listOf(3 to 2, 12 to 2, 7 to 0, 8 to 0)).beat(SLOW_MS),
            creatureFrame(body, eyeHoles = body.eyesHalf).beat(SETTLE_MS)
        )

        AvatarSpecies.WYRMLING -> listOf(
            creatureFrame(body, accentPhase = -1).beat(FAST_MS),
            creatureFrame(body, accentPhase = 1, dy = -2, mouthHoles = body.mouthOpen).beat(FAST_MS),
            creatureFrame(body, accentPhase = -1, dy = -4, tailWag = 1, mouthHoles = body.mouthOpen).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1, dy = -5, tailWag = -1).beat(BEAT_MS),
            creatureFrame(body, accentPhase = -1, dy = -2, tailWag = 1).beat(SLOW_MS),
            creatureFrame(body, tailWag = -1).beat(SETTLE_MS)
        )

        AvatarSpecies.FENNEC -> listOf(
            creatureFrame(body, accentPhase = 1).beat(FAST_MS),
            creatureFrame(body, dy = -3, feetSpread = 1, accentPhase = 1, mouthHoles = body.mouthOpen).beat(FAST_MS),
            creatureFrame(body, dy = 0, tailWag = 1).beat(FAST_MS),
            creatureFrame(body, dy = -3, feetSpread = 1, accentPhase = -1, mouthHoles = body.mouthOpen).beat(FAST_MS),
            creatureFrame(body, dy = 0, tailWag = -1).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
        )

        AvatarSpecies.HOOTLET -> listOf(
            creatureFrame(body, eyeHoles = body.eyesHalf).beat(BEAT_MS),
            // Aufplustern: Fluegel raus, Koerper gestreckt.
            creatureFrame(body, accentPhase = 1, dy = -1, mouthHoles = body.mouthOpen).beat(SLOW_MS),
            creatureFrame(body, accentPhase = -1, dy = 0, mouthHoles = body.mouthOpen).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1, dy = -1).beat(BEAT_MS),
            creatureFrame(body, eyeHoles = body.eyesHalf).beat(FAST_MS),
            creatureFrame(body).beat(SETTLE_MS)
        )
    }

    // ---- Generisch (Custom-Bibliotheksanimation ohne festen Typ): kleiner Funke waechst,
    // Kreatur schaut ueberrascht (Mund auf), doppelter Hopser ----
    // Fehlerkorrektur: die beiden Hopser-Frames unterschieden sich nur durch tailWag - bei
    // Starlet/Gloop/Hootlet (kein Schwanz) ein Stillstand. Der zweite Hopser hat jetzt
    // zusaetzlich ein kurzes Kichern (mouthOpen), wirkt bei jeder Spezies.
    /**
     * Ausrufezeichen ueber dem Kopf - "da ist etwas". [stage] 0 = nur ein Punkt, 2 = vollstaendig.
     *
     * Bewusst ein Zeichen statt eines Funkens: Diese Reaktion greift, wenn die Erinnerung
     * KEINEN bekannten Typ hat, es also nichts Bestimmtes zu zeigen gibt. Ein Ausrufezeichen
     * sagt genau das - etwas steht an, ohne zu behaupten, was. Bleibt in der Zone y<=3, wo bei
     * jeder Spezies auch nach dem dy-Versatz Platz frei ist.
     */
    private fun alertMark(stage: Int): List<Pair<Int, Int>> = when (stage) {
        0 -> listOf(7 to 3)
        1 -> listOf(7 to 1, 7 to 3)
        else -> listOf(7 to 0, 7 to 1, 7 to 3)
    }

    private fun genericKeyframes(body: AvatarBody) = listOf(
        creatureFrame(body, prop = alertMark(0)).beat(FAST_MS),
        creatureFrame(body, prop = alertMark(1)).beat(FAST_MS),
        creatureFrame(body, accentPhase = 1, prop = alertMark(2)).beat(BEAT_MS),
        // Bemerkt: Kopf hoch, Mund auf.
        creatureFrame(body, mouthHoles = body.mouthOpen, dy = -1, accentPhase = -1, prop = alertMark(2)).beat(SLOW_MS),
        // Zeichen verschwindet, der arteigene Abschluss uebernimmt.
        creatureFrame(body, mouthHoles = body.mouthOpen).beat(SETTLE_MS)
    )
}
