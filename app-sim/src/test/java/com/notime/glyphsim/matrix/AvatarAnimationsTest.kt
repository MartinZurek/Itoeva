package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.AvatarSignatureAnimations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests fuer die Avatar-Reaktionen (siehe [AvatarAnimations]).
 *
 * Hintergrund: die hier geprueften Invarianten sind allesamt aus echten, gemeldeten Fehlern
 * entstanden - der Avatar verschwand mitten in der Reaktion, Deko-Punkte lagen unsichtbar auf
 * der Koerpersilhouette, und einzelne Uebergaenge standen bei bestimmten Spezies komplett
 * still. Gefunden wurden sie damals nur, weil die Frames per Hand als Pixelraster gerendert
 * und verglichen wurden; genau das passiert hier automatisiert, ueber ALLE Kombinationen aus
 * Reaktion und Spezies (nicht nur die Standardform - drei der sechs Spezies haben keinen
 * Schwanz, GLOOP zusaetzlich keine Fuesse, weshalb Bewegungen, die sich allein auf
 * tailWag/feetSpread stuetzen, dort wirkungslos sind).
 *
 * Reine Punktrechnerei ohne Android-Abhaengigkeit, laeuft daher als schneller JVM-Unit-Test.
 */
class AvatarAnimationsTest {

    private val grid = AvatarGeometry.SIZE

    /**
     * Alle Reaktionen, die es gibt: die 12 festen Typen, der generische Fallback, Rocket und die
     * 30 charakterspezifischen Signatur-Reaktionen.
     *
     * Die Signatur-Reaktionen laufen bewusst gegen JEDE Spezies und nicht nur gegen die, fuer die
     * sie entworfen wurden: Die Zuordnung Animation → Avatar ist gestalterisch, technisch laesst
     * sich jede Animation fuer jeden Avatar waehlen. Eine Choreografie, die sich auf einen
     * Schwanz oder Fuesse verliesse, waere bei Gloop ein Standbild - genau das faengt das hier ab.
     */
    private fun allReactions(): List<ReactionCase> {
        val cases = mutableListOf<ReactionCase>()
        for (species in AvatarSpecies.entries) {
            for (type in AnimationType.entries) {
                cases += ReactionCase(species, type.name, AvatarAnimations.reactionFramesFor(species, type))
            }
            cases += ReactionCase(species, "GENERIC", AvatarAnimations.reactionFramesFor(species, null))
            cases += ReactionCase(species, "ROCKET", AvatarAnimations.reactionFramesFor(species, null, "Rocket"))
            for (label in AvatarSignatureReactions.labels) {
                cases += ReactionCase(species, label, AvatarAnimations.reactionFramesFor(species, null, label))
            }
        }
        return cases
    }

    /** Jede Signatur-Animation muss auch tatsaechlich eine eigene Choreografie haben - sonst
     *  faellt sie stillschweigend auf die allgemeine Freuden-Reaktion zurueck. */
    @Test
    fun everySignatureAnimationHasItsOwnReaction() {
        val body = AvatarBodies.forSpecies(AvatarSpecies.PUFFLING)
        for (label in AvatarSignatureReactions.labels) {
            assertTrue(
                "Fuer \"$label\" ist keine eigene Reaktion hinterlegt",
                AvatarSignatureReactions.forLabel(label, body) != null
            )
        }
    }

    /** Die Labels muessen zu den tatsaechlich vorhandenen Animationen passen - ein Tippfehler
     *  auf einer der beiden Seiten faellt sonst nirgends auf, die Reaktion greift nur nie. */
    @Test
    fun signatureReactionLabelsMatchTheAnimations() {
        val animationLabels = AvatarSignatureAnimations.seed().map { it.label }.toSet()
        for (label in AvatarSignatureReactions.labels) {
            assertTrue(
                "Reaktion \"$label\" hat keine passende Animation in AvatarSignatureAnimations",
                label in animationLabels
            )
        }
        assertEquals(
            "Es gibt Signatur-Animationen ohne eigene Reaktion",
            animationLabels.size,
            AvatarSignatureReactions.labels.size
        )
    }

    private data class ReactionCase(
        val species: AvatarSpecies,
        val label: String,
        val frames: List<IntArray>
    ) {
        override fun toString() = "$label / $species"
    }

    private fun IntArray.isBlank(): Boolean = none { it > 0 }

    private fun changedPixels(a: IntArray, b: IntArray): Int =
        a.indices.count { (a[it] > 0) != (b[it] > 0) }

    /** Rendert einen Frame als Pixelraster - macht eine Fehlermeldung tatsaechlich lesbar. */
    private fun render(frame: IntArray): String =
        (0 until grid).joinToString("\n") { y ->
            (0 until grid).joinToString("") { x -> if (frame[y * grid + x] > 0) "#" else "." }
        }

    /**
     * Kern-Invariante: zwischen zwei aufeinanderfolgenden Frames muss sich IMMER etwas aendern.
     * Ein identisches Frame-Paar bedeutet bei [MatrixAnimator.AVATAR_FRAME_DELAY_MS] ein
     * sichtbares Standbild mitten in der Reaktion - genau der Fehler, der bei BOOK/REST/WORK/
     * MEDICINE/GENERIC und Rocket bei den schwanzlosen Spezies auftrat.
     */
    @Test
    fun everyReactionAdvancesOnEveryFrame() {
        for (case in allReactions()) {
            for (i in 1 until case.frames.size) {
                val changed = changedPixels(case.frames[i - 1], case.frames[i])
                assertTrue(
                    "Stillstand in $case: Frame ${i - 1} und $i sind identisch " +
                        "(0 geaenderte Pixel, also ${MatrixAnimator.AVATAR_FRAME_DELAY_MS}ms Standbild).\n" +
                        "Frame ${i - 1}:\n${render(case.frames[i - 1])}",
                    changed > 0
                )
            }
        }
    }

    /**
     * Dieselbe Invariante fuer den Geh-Zyklus (siehe [AvatarAnimations.walkSequence]) - und hier
     * besonders wichtig, weil der Zyklus auf Fuss-Spreizung und Schwanz-Wedel baut und drei der
     * sechs Spezies keinen Schwanz haben, GLOOP zusaetzlich keine Fuesse. Bliebe dort ein
     * Frame-Paar identisch, ginge der Avatar nicht, sondern glitte mit steifem Koerper ueber den
     * Boden - genau das Problem, das der Geh-Zyklus loesen soll.
     *
     * Geprueft wird auch der Uebergang vom letzten zurueck zum ersten Frame: die Schleife laeuft
     * endlos, ein Stillstand an der Nahtstelle waere bei jedem Durchlauf zu sehen.
     */
    @Test
    fun everyWalkCycleAdvancesOnEveryFrame() {
        for (species in AvatarSpecies.entries) {
            val walk = AvatarAnimations.walkSequence(species)
            assertTrue("$species hat keinen Geh-Zyklus", walk.frames.size > 1)
            for (i in walk.frames.indices) {
                val previous = walk.frames[if (i == 0) walk.frames.lastIndex else i - 1]
                assertTrue(
                    "Stillstand im Gang von $species bei Frame $i:\n${render(previous)}",
                    changedPixels(previous, walk.frames[i]) > 0
                )
            }
        }
    }

    /**
     * Dieselbe Stillstands-Invariante fuer die Alltagsregungen
     * ([AvatarAnimations.fidgetSequence]) - und hier aus demselben Grund heikel wie beim Gang:
     * Sie sind aus Fuss-, Schwanz- und Akzent-Stellungen gebaut, von denen einzelne Spezies
     * manche gar nicht besitzen. Bei GLOOP (weder Fuesse noch Schwanz) faellt zum Beispiel jede
     * Unterscheidung weg, die allein auf `tailWag` beruht.
     */
    @Test
    fun everyFidgetAdvancesOnEveryFrame() {
        for (species in AvatarSpecies.entries) {
            for (fidget in AvatarAnimations.Fidget.entries) {
                val sequence = AvatarAnimations.fidgetSequence(species, fidget)
                assertTrue("$species/$fidget ist leer", sequence.frames.size > 1)
                for (i in 1 until sequence.frames.size) {
                    assertTrue(
                        "Stillstand in $species/$fidget bei Frame $i:\n${render(sequence.frames[i])}",
                        changedPixels(sequence.frames[i - 1], sequence.frames[i]) > 0
                    )
                }
            }
        }
    }

    /**
     * Die vier Regungen muessen sich auch UNTEREINANDER unterscheiden - sonst waere die
     * Abwechslung nur in den Namen und nicht im Bild. Verglichen wird der Verlauf als Ganzes,
     * weil einzelne Posen (etwa die Ruhelage am Anfang) durchaus geteilt sein duerfen.
     */
    @Test
    fun fidgetsDifferFromEachOther() {
        for (species in AvatarSpecies.entries) {
            val shapes = AvatarAnimations.Fidget.entries.map { fidget ->
                AvatarAnimations.fidgetSequence(species, fidget).frames.map { it.toList() }
            }
            assertEquals(
                "$species zeigt fuer verschiedene Regungen dieselbe Bewegung",
                shapes.size,
                shapes.distinct().size
            )
        }
    }

    /**
     * Der Koerper muss sich im Gang tatsaechlich HEBEN und SENKEN. Ohne dieses Auf und Ab bleibt
     * es ein Verschieben - egal wie weich die Position gerechnet wird. Gemessen an der obersten
     * belegten Zeile, die sich ueber den Zyklus veraendern muss.
     */
    /**
     * **Nichts darf oben aus dem Raster fallen.**
     *
     * Genau das war lange der Fall und niemandem aufgefallen: Die Koerper sind auf 16 Zeilen
     * gezeichnet und nutzen sie teilweise voll aus (FENNECs Ohren sitzen in Zeile 0). Jede
     * Aufwaertsbewegung schob Punkte darueber hinaus, und da der Frame-Bau alles ausserhalb des
     * Rasters stillschweigend verwirft, waren die Ohren im Sprung schlicht ab - ohne Absturz,
     * ohne Warnung, nur ein Stueck Figur weniger.
     *
     * Geprueft wird deshalb nicht die Raster-Groesse, sondern die WIRKUNG: Die Zahl der
     * leuchtenden Zellen eines Frames muss der Zahl der Punkte in der zugehoerigen Pose
     * entsprechen. Faellt dieser Test, ist die Kopffreiheit ([AvatarGeometry.HEADROOM]) zu klein
     * fuer eine neue Bewegung.
     */
    @Test
    fun noReactionLosesPixelsAtTheTop() {
        val height = AvatarGeometry.HEIGHT
        for (species in AvatarSpecies.entries) {
            // Die oberste Rasterzeile muss frei bleiben - beruehrt eine Bewegung sie, ist die
            // naechstgroessere schon abgeschnitten.
            val sequences = buildList {
                for (type in AnimationType.entries) add("$type" to AvatarAnimations.reactionFramesFor(species, type))
                for (f in AvatarAnimations.Fidget.entries) add("$f" to AvatarAnimations.fidgetSequence(species, f).frames)
                add("WALK" to AvatarAnimations.walkSequence(species).frames)
                add("IDLE" to AvatarAnimations.idleSequence(species).frames)
            }
            for ((label, frames) in sequences) {
                frames.forEachIndexed { i, frame ->
                    val topRowLit = (0 until grid).any { x -> frame[x] > 0 }
                    assertFalse(
                        "$species/$label Frame $i beruehrt die oberste Rasterzeile - die naechste " +
                            "Aufwaertsbewegung wuerde abgeschnitten. Kopffreiheit erhoehen.",
                        topRowLit
                    )
                    assertEquals(
                        "$species/$label Frame $i ist unvollstaendig gerastert",
                        frame.size,
                        grid * height
                    )
                }
            }
        }
    }

    @Test
    fun everyWalkCycleBobsUpAndDown() {
        for (species in AvatarSpecies.entries) {
            val topRows = AvatarAnimations.walkSequence(species).frames.map { frame ->
                (0 until grid).firstOrNull { y -> (0 until grid).any { x -> frame[y * grid + x] > 0 } }
            }
            assertTrue("$species geht ohne Auf und Ab (Koerper bleibt auf gleicher Hoehe)", topRows.distinct().size > 1)
        }
    }

    /**
     * Der Avatar darf nicht mitten in der Reaktion komplett verschwinden und danach wieder
     * auftauchen - das war der urspruengliche Fehlerbericht ("er springt nach oben, aber man
     * sieht ihn nicht mehr"). Ein leerer Frame am ENDE ist dagegen legitim: die Rocket-Reaktion
     * laesst den Avatar bewusst endgueltig aus dem Bild fliegen.
     */
    @Test
    fun noReactionGoesBlankAndComesBack() {
        for (case in allReactions()) {
            val lastVisible = case.frames.indexOfLast { !it.isBlank() }
            for (i in 0..lastVisible) {
                assertFalse(
                    "Avatar verschwindet in $case bei Frame $i und taucht spaeter wieder auf " +
                        "(letzter sichtbarer Frame: $lastVisible).",
                    case.frames[i].isBlank()
                )
            }
        }
    }

    /** Eine Reaktion, die zu kurz ist, ist als Belohnung fuers Fuettern nicht wahrnehmbar. */
    @Test
    fun everyReactionHasEnoughFrames() {
        for (case in allReactions()) {
            assertTrue(
                "$case hat nur ${case.frames.size} Frames - zu kurz, um sichtbar zu sein.",
                case.frames.size >= MIN_REACTION_FRAMES
            )
        }
    }

    /** Dieselbe Stillstands-Regel gilt fuer die Idle-Schleife, die dauerhaft laeuft. */
    @Test
    fun idleLoopAdvancesOnEveryFrame() {
        for (species in AvatarSpecies.entries) {
            val frames = AvatarAnimations.idleSequence(species).frames
            assertTrue("Idle-Schleife von $species ist leer", frames.isNotEmpty())
            for (i in 1 until frames.size) {
                assertTrue(
                    "Stillstand in Idle-Schleife von $species: Frame ${i - 1} und $i sind identisch.",
                    changedPixels(frames[i - 1], frames[i]) > 0
                )
            }
        }
    }

    /**
     * Jede Spezies muss eine EIGENE Ruhe-Schleife haben.
     *
     * Frueher teilten alle sechs denselben Keyframe-Satz und denselben Takt; sie unterschieden sich
     * nur in der Silhouette und wirkten dadurch wie dieselbe Figur in sechs Kostuemen. Dieser Test
     * haelt die Individualitaet fest - wuerde jemand die Schleifen wieder zu einer gemeinsamen
     * zusammenlegen, faellt es hier auf und nicht erst im fertigen Build.
     */
    @Test
    fun everySpeciesHasItsOwnIdleRhythm() {
        val sequences = AvatarSpecies.entries.associateWith { AvatarAnimations.idleSequence(it) }
        val speciesList = AvatarSpecies.entries.toList()
        for (i in speciesList.indices) {
            for (j in i + 1 until speciesList.size) {
                val a = speciesList[i]
                val b = speciesList[j]
                val sameRhythm = sequences.getValue(a).holdsMs == sequences.getValue(b).holdsMs
                val sameFrames = sequences.getValue(a).frames.size == sequences.getValue(b).frames.size &&
                    sequences.getValue(a).frames.zip(sequences.getValue(b).frames).all { (x, y) -> x.contentEquals(y) }
                assertTrue(
                    "$a und $b haben eine identische Ruhe-Schleife - jede Spezies braucht ihren eigenen Charakter.",
                    !(sameRhythm && sameFrames)
                )
            }
        }
    }

    /**
     * Die Ruhe-Schleife darf nicht hetzen: sie laeuft dauerhaft im Blickfeld, und eine schnelle
     * Abfolge liest sich dort als Zappeln statt als Atmen. Geprueft wird der Durchschnitt statt
     * jedes einzelnen Frames, weil kurze Betonungen (Zwinkern, Fluegelschlag) ausdruecklich
     * erwuenscht sind - nur eben eingebettet in lange Ruhelagen.
     */
    @Test
    fun idleLoopIsCalmNotHectic() {
        for (species in AvatarSpecies.entries) {
            val holds = AvatarAnimations.idleSequence(species).holdsMs
            assertTrue("Idle-Schleife von $species hat keine Standzeiten", holds.isNotEmpty())
            val average = holds.sum().toDouble() / holds.size
            assertTrue(
                "Idle-Schleife von $species ist zu hektisch: durchschnittlich ${average.toInt()}ms pro Frame.",
                average >= 300.0
            )
            val total = holds.sum()
            assertTrue(
                "Idle-Schleife von $species ist mit ${total}ms zu kurz - ein Durchlauf sollte ruhig wirken.",
                total >= 3000L
            )
        }
    }

    // ---- Rocket: die Flugbahn ueber den Bildschirm (siehe AvatarAnimations.rocketFlightOffsets) ----

    /**
     * Die Flugbahn wird in ui/DockScreen.kt und ui/HomeScreen.kt Frame fuer Frame parallel zu den
     * Keyframes abgelaufen - stimmen die Laengen nicht ueberein, bleibt der Avatar am Ende auf
     * dem letzten Offset haengen statt sauber auszufliegen.
     */
    @Test
    fun rocketFlightPathMatchesKeyframeCount() {
        val frames = AvatarAnimations.reactionFramesFor(AvatarSpecies.PUFFLING, null, "Rocket")
        assertEquals(
            "Flugbahn und Keyframes der Rocket-Reaktion muessen gleich lang sein",
            frames.size,
            AvatarAnimations.rocketFlightOffsets().size
        )
    }

    /**
     * Die Flugbahn darf nie unter die Startposition sacken. Sie geht waehrend des Loopings
     * bewusst zwischendurch wieder abwaerts (das IST der Looping) - aber immer oberhalb des
     * Punktes, an dem der Avatar gestartet ist; alles andere saehe aus, als wuerde er nach unten
     * wegsacken statt eine Schleife zu fliegen.
     *
     * Achtung, haeufige Verwechslung: der urspruengliche Fehlerbericht ("springt hoch, aber man
     * sieht ihn nicht mehr") betraf NICHT diese Bildschirm-Flugbahn, sondern das 16x16-Raster
     * des Sprites - dort lief der Avatar aus dem Raster und kam kurz zurueck. Diese Regression
     * deckt [noReactionGoesBlankAndComesBack] ab.
     */
    @Test
    fun rocketFlightNeverSinksBelowStart() {
        val offsets = AvatarAnimations.rocketFlightOffsets()
        for ((index, offset) in offsets.withIndex()) {
            assertTrue(
                "Rocket-Flugbahn sackt bei Schritt $index unter die Startposition (dy=${offset.second}).",
                offset.second <= 0f
            )
        }
    }

    /**
     * Am Ende muss der Avatar sicher aus dem Bild sein - und zwar unabhaengig davon, wo er
     * zufaellig gespawnt ist (die Startposition ist im Dock-Modus zufaellig, siehe
     * DockScreen.randomAvatarOffset). Eine ganze Bildschirmhoehe Abstand deckt jede Startlage ab;
     * bliebe er knapp am Rand stehen, wuerde er einfach im Bild "einfrieren".
     */
    @Test
    fun rocketFlightEndsWellOffScreen() {
        val last = AvatarAnimations.rocketFlightOffsets().last()
        assertTrue(
            "Rocket-Flugbahn endet bei dy=${last.second} - zu nah am Bild, der Avatar bliebe " +
                "je nach Startposition sichtbar stehen.",
            last.second <= -1.0f
        )
    }

    /**
     * Keine Tempo-Ausreisser: die Schrittweite zwischen zwei Punkten der Flugbahn darf sich nicht
     * sprunghaft aendern, sonst stockt die Bewegung sichtbar (war beim Verlassen des Loopings der
     * Fall, wo die Schrittweite von ~0.10 auf 0.45 sprang und wieder zurueckfiel).
     */
    @Test
    fun rocketFlightHasNoSpeedOutlier() {
        val offsets = AvatarAnimations.rocketFlightOffsets()
        val steps = (1 until offsets.size).map { i ->
            val dx = offsets[i].first - offsets[i - 1].first
            val dy = offsets[i].second - offsets[i - 1].second
            kotlin.math.sqrt(dx * dx + dy * dy)
        }
        for (i in 1 until steps.size) {
            val previous = steps[i - 1]
            val current = steps[i]
            if (previous < MIN_STEP_FOR_RATIO_CHECK) continue
            assertTrue(
                "Rocket-Flugbahn: Schrittweite springt bei Schritt ${i + 1} von $previous auf " +
                    "$current (Faktor ${current / previous}) - das stockt sichtbar.",
                current / previous <= MAX_STEP_RATIO && previous / current <= MAX_STEP_RATIO
            )
        }
    }

    // ---- Gezielte Regressionstests fuer unsichtbare Deko-Punkte ----
    //
    // Diese Punkte lagen frueher exakt auf Pixeln, die die Koerpersilhouette ohnehin schon
    // beleuchtet - sie waren dadurch bei JEDER Spezies unsichtbar, ohne dass die Animation
    // dabei "stillstand" (der Koerper bewegte sich ja weiter). Die Pruefung haengt bewusst an
    // konkreten Koordinaten: sie haelt fest, dass der jeweilige Effekt tatsaechlich erscheint.

    /**
     * [y] wird in den Koordinaten der GEZEICHNETEN Koerper angegeben, so wie die Konstanten
     * unten und die Posen in AvatarAnimations sie verwenden. Das Raster ist inzwischen hoeher als
     * die Figur (Kopffreiheit, siehe AvatarGeometry.HEADROOM), deshalb wird hier umgerechnet -
     * damit die Pruefungen weiter in der Sprache der Pixel-Art lesbar bleiben statt in
     * Rasterzeilen.
     */
    private fun isLit(frame: IntArray, x: Int, y: Int): Boolean =
        frame[(y + AvatarGeometry.HEADROOM) * grid + x] > 0

    @Test
    fun drinkDropIsActuallyVisible() {
        for (species in AvatarSpecies.entries) {
            val frames = AvatarAnimations.reactionFramesFor(species, AnimationType.DRINK)
            assertFalse(
                "DRINK/$species: Tropfen-Position ist schon vor dem Fallen belegt - " +
                    "der Tropfen waere unsichtbar.",
                isLit(frames[0], DROP_X, DROP_Y)
            )
            assertTrue(
                "DRINK/$species: Tropfen erscheint nicht - er liegt vermutlich auf der " +
                    "Koerpersilhouette und ist dadurch unsichtbar.",
                isLit(frames[1], DROP_X, DROP_Y)
            )
        }
    }

    /**
     * Bewusst ueber ALLE Frames statt an einem festen Bildindex: Geprueft werden soll, dass das
     * aufsteigende Herz ueberhaupt sichtbar wird - nicht, an welcher Stelle der Choreografie.
     * Die feste Indexpruefung schlug fehl, sobald dem Herzen ein Pulsschlag vorangestellt wurde,
     * obwohl daran nichts kaputt war. Ein Test, der bei jeder Umgestaltung anschlaegt, ohne dass
     * ein Fehler vorliegt, wird irgendwann weggeklickt statt gelesen.
     */
    @Test
    fun loveRisingHeartsAreActuallyVisible() {
        for (species in AvatarSpecies.entries) {
            val frames = AvatarAnimations.reactionFramesFor(species, AnimationType.LOVE)
            assertTrue(
                "LOVE/$species: aufsteigendes Herz erscheint in keinem Bild - es verschwindet " +
                    "vermutlich hinter dem angehobenen Koerper.",
                frames.any { isLit(it, HEART_X, HEART_Y) }
            )
        }
    }

    @Test
    fun creativitySparksAreActuallyVisible() {
        for (species in AvatarSpecies.entries) {
            val frames = AvatarAnimations.reactionFramesFor(species, AnimationType.CREATIVITY)
            assertTrue(
                "CREATIVITY/$species: Funke erscheint nicht - er verschwindet vermutlich " +
                    "hinter dem angehobenen Koerper.",
                isLit(frames[3], SPARK_X, SPARK_Y)
            )
        }
    }

    // ---- Timing und Spezies-Akzent ----

    /**
     * Zu jedem Frame muss es eine Standzeit geben. Waeren die Listen unterschiedlich lang, liefe
     * der Rest der Reaktion still auf einem Rueckfallwert - der sorgfaeltig gesetzte Rhythmus
     * waere dann teilweise wirkungslos, ohne dass irgendetwas sichtbar kaputt geht.
     */
    @Test
    fun everyFrameHasItsOwnHoldTime() {
        for (species in AvatarSpecies.entries) {
            for (type in AnimationType.entries) {
                val reaction = AvatarAnimations.reactionFor(species, type)
                assertEquals(
                    "$type / $species: Frames und Standzeiten unterschiedlich lang",
                    reaction.frames.size,
                    reaction.holdsMs.size
                )
                assertTrue(
                    "$type / $species: Standzeit <= 0 gefunden - der Frame waere unsichtbar.",
                    reaction.holdsMs.all { it > 0 }
                )
            }
        }
    }

    /**
     * Jede Reaktion muss Tempowechsel enthalten. Ein durchgehend gleicher Takt war genau der
     * Zustand vor dieser Ueberarbeitung: ruhige Reaktionen hetzten im selben Rhythmus wie ein
     * Hopser, Betonungen gingen unter.
     */
    @Test
    fun everyReactionVariesItsTempo() {
        for (species in AvatarSpecies.entries) {
            for (type in AnimationType.entries) {
                val holds = AvatarAnimations.reactionFor(species, type).holdsMs
                assertTrue(
                    "$type / $species laeuft mit durchgehend gleichem Takt (${holds.first()}ms) - " +
                        "ohne Tempowechsel wirkt die Reaktion mechanisch.",
                    holds.distinct().size > 1
                )
            }
        }
    }

    /**
     * Der letzte Frame muss deutlich laenger stehen als der Durchschnitt. Ohne diesen Nachklang
     * waren die Reaktionen in unter einer Sekunde vorbei - als Belohnung fuers Fuettern kaum
     * wahrnehmbar, der Avatar verschwand praktisch im selben Moment, in dem er reagierte.
     *
     * Rocket ist bewusst ausgenommen: dort ist der letzte Frame leer, weil der Avatar aus dem
     * Bild geflogen ist - da gibt es nichts mehr nachklingen zu lassen.
     */
    @Test
    fun everyReactionEndsOnASettleBeat() {
        for (species in AvatarSpecies.entries) {
            for (type in AnimationType.entries) {
                val holds = AvatarAnimations.reactionFor(species, type).holdsMs
                val average = holds.average()
                assertTrue(
                    "$type / $species endet ohne Nachklang (letzter Frame ${holds.last()}ms bei " +
                        "Durchschnitt ${average.toInt()}ms) - die Reaktion verpufft.",
                    holds.last() > average
                )
            }
        }
    }

    /**
     * **Jede Spezies muss sich bei einer Bewegungsphase sichtbar veraendern.**
     *
     * Hintergrund: STARLET/GLOOP/HOOTLET haben keinen Schwanz, GLOOP zusaetzlich keine Fuesse.
     * Solange sich Bewegungen allein darauf stuetzten, wirkten deren Reaktionen systematisch
     * matter als die von PUFFLING - messbar an deutlich weniger geaenderten Pixeln pro Uebergang.
     *
     * **Geprueft wird jetzt die WIRKUNG, nicht mehr das Mittel.** Die vorherige Fassung verlangte
     * einen nicht leeren [AvatarBody.accent] - also genau das eine Mittel, mit dem das damals
     * geloest war. Seit GLOOPs Koerper seine ganze Form aus der Phase rechnet (siehe
     * [GloopShape]), braucht er bei kleinen Phasen gar keinen Akzent mehr: Es bewegt sich der
     * Koerper selbst, und zwar staerker als jede Zusatzzelle es koennte. Der Test schlug an,
     * obwohl die Sache besser geworden war - ein sicheres Zeichen dafuer, dass er das Falsche
     * gemessen hat.
     */
    @Test
    fun everySpeciesHasAVisibleAccent() {
        for (species in AvatarSpecies.entries) {
            val body = AvatarBodies.forSpecies(species)
            val rest = AvatarAnimations.creatureFrame(body).toSet()
            val up = AvatarAnimations.creatureFrame(body, accentPhase = 1).toSet()
            val down = AvatarAnimations.creatureFrame(body, accentPhase = -1).toSet()

            assertTrue(
                "$species veraendert sich bei Phase +1 nicht - die Spezies bliebe bewegungsarm.",
                up != rest
            )
            assertTrue(
                "$species veraendert sich bei Phase -1 nicht.",
                down != rest
            )
            assertTrue(
                "$species: Phase +1 und -1 sehen gleich aus - es bewegt sich also nichts.",
                up != down
            )
        }
    }

    @Test
    fun `nur GLOOP veraendert seine Form, die anderen behalten sie`() {
        // Die Gegenprobe zur neuen Freiheit: Eine gerechnete Silhouette ist ein maechtiges
        // Werkzeug und waere bei einem Koerper aus Fell und Knochen genau falsch. Rutscht sie je
        // versehentlich auf eine andere Spezies, faellt das hier auf.
        for (species in AvatarSpecies.entries) {
            val body = AvatarBodies.forSpecies(species)
            val deforms = body.silhouetteFor(2).toSet() != body.silhouetteFor(0).toSet()
            if (species == AvatarSpecies.GLOOP) {
                assertTrue("GLOOP verformt sich nicht mehr", deforms)
            } else {
                assertTrue("$species verformt plötzlich seinen Koerper", !deforms)
            }
        }
    }

    /**
     * Die Staubwolke beim Landen muss tatsaechlich erscheinen.
     *
     * Dieselbe Falle wie beim Trink-Tropfen: Deko unterhalb des Koerpers ist schnell von der
     * Silhouette verdeckt und dann bei jeder Spezies unsichtbar, ohne dass die Animation
     * stillsteht. Zeile 15 liegt unter der tiefsten Silhouette (y=14), dort kann nur Staub
     * leuchten.
     */
    @Test
    fun moveDustIsActuallyVisible() {
        for (species in AvatarSpecies.entries) {
            val frames = AvatarAnimations.reactionFramesFor(species, AnimationType.MOVE)
            val framesWithDust = frames.count { frame ->
                (0 until grid).any { x -> isLit(frame, x, DUST_Y) }
            }
            assertTrue(
                "MOVE/$species: in keinem Bild ist Staub zu sehen - die Wolke liegt vermutlich " +
                    "unter der Silhouette und ist dadurch unsichtbar.",
                framesWithDust > 0
            )
        }
    }

    /**
     * Tasse und Glas muessen sich sichtbar leeren.
     *
     * Vorher zeigten beide Schlucke exakt dasselbe volle Gefaess - dass ueberhaupt etwas
     * getrunken wird, war nicht zu erkennen. Geprueft wird der Requisiten-Bereich (die vier
     * obersten Zeilen, in denen bei keiner Spezies Koerper liegt): Dort muss es einen Moment
     * geben, in dem weniger leuchtet als am Anfang.
     */
    @Test
    fun drinkingVesselsEmptyVisibly() {
        for (type in listOf(AnimationType.DRINK, AnimationType.REST)) {
            for (species in AvatarSpecies.entries) {
                val frames = AvatarAnimations.reactionFramesFor(species, type)
                val propPixels = frames.map { frame ->
                    (0..PROP_ZONE_BOTTOM_Y).sumOf { y -> (0 until grid).count { x -> isLit(frame, x, y) } }
                }
                assertTrue(
                    "$type/$species: im Requisiten-Bereich nimmt nie etwas ab - das Gefaess " +
                        "leert sich nicht sichtbar. Verlauf: $propPixels",
                    propPixels.min() < propPixels.first()
                )
            }
        }
    }

    // ---- Der Avatar muss einen Unterschied machen ----

    /**
     * Jede Reaktion muss bei jeder Spezies **anders enden**.
     *
     * Frueher schloss jede Reaktion mit demselben allgemeinen Hopser ab; welchen Avatar man
     * gewaehlt hatte, war fuer den Ablauf belanglos - die Wahl war eine Frage der Silhouette,
     * nicht des Charakters. Seit [AvatarAnimations.speciesFinish] gehoert die Handlung dem
     * Erinnerungstyp und der Abschluss der Spezies.
     *
     * Verglichen werden nur die letzten Frames, und zwar als **Form**, nicht als Pixelmenge:
     * die sechs Silhouetten sind ohnehin verschieden, ein Vergleich der rohen Bilder wuerde
     * also immer bestehen und nichts beweisen. Geprueft wird stattdessen die Bewegung - wie
     * viele Pixel sich zum vorangehenden Frame aendern. Machen zwei Spezies exakt dieselbe
     * Schlussbewegung, ist der Abschluss nicht arteigen.
     */
    @Test
    fun reactionsEndDifferentlyPerSpecies() {
        for (type in AnimationType.entries) {
            val signatures = AvatarSpecies.entries.associateWith { species ->
                val frames = AvatarAnimations.reactionFramesFor(species, type)
                // Bewegungsprofil der letzten drei Uebergaenge.
                (maxOf(1, frames.size - 3) until frames.size).map { i ->
                    changedPixels(frames[i - 1], frames[i])
                }
            }
            val distinct = signatures.values.toSet()
            assertTrue(
                "$type: die Schlussbewegung ist bei mehreren Spezies identisch - der Avatar " +
                    "macht dort keinen Unterschied. Profile: $signatures",
                distinct.size >= 4
            )
        }
    }

    /**
     * Ruhige Reaktionen duerfen nicht in einem Freudensprung enden.
     *
     * Ausruhen, Schlafen und Achtsamkeit fuehren zur Ruhe hin - ein energischer Hopser am Ende
     * wuerde genau das zunichtemachen. Gemessen an der Bewegung im Schlussbild: sie muss
     * kleiner sein als der lebhafteste Uebergang derselben Reaktion.
     */
    @Test
    fun calmReactionsSettleInsteadOfJumping() {
        val calm = listOf(AnimationType.REST, AnimationType.SLEEP, AnimationType.MINDFULNESS)
        for (type in calm) {
            for (species in AvatarSpecies.entries) {
                val frames = AvatarAnimations.reactionFramesFor(species, type)
                val steps = (1 until frames.size).map { changedPixels(frames[it - 1], frames[it]) }
                val last = steps.last()
                val busiest = steps.max()
                assertTrue(
                    "$type/$species: der Schlussuebergang bewegt $last Pixel und ist damit der " +
                        "lebhafteste der ganzen Reaktion ($busiest) - das wirkt wie Aufspringen, " +
                        "nicht wie Zurruhekommen.",
                    last < busiest
                )
            }
        }
    }

    private companion object {
        const val MIN_REACTION_FRAMES = 4

        /** Unterhalb dieser Schrittweite ist das Verhaeltnis zweier Schritte nicht aussagekraeftig. */
        const val MIN_STEP_FOR_RATIO_CHECK = 0.05f
        const val MAX_STEP_RATIO = 2.5f

        // Siehe AvatarAnimations.drinkKeyframes / loveKeyframes / creativityKeyframes.
        const val DROP_X = 7
        const val DROP_Y = 4
        const val HEART_X = 7
        const val HEART_Y = 3
        const val SPARK_X = 7
        const val SPARK_Y = 3

        /** Siehe AvatarAnimations.dust - liegt unter der tiefsten Silhouette (y=14). */
        const val DUST_Y = 15

        /**
         * Bis hierher reicht der Requisiten-Bereich. Darunter beginnt bei der hoechsten Spezies
         * der Kopfschmuck; oberhalb liegt bei keiner Spezies Koerper, dort steht nur das Prop.
         */
        const val PROP_ZONE_BOTTOM_Y = 3
    }
}
