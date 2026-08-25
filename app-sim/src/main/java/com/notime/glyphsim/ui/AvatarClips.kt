package com.notime.glyphsim.ui

import androidx.annotation.StringRes
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.AvatarSignatureAnimations
import com.notime.glyphcore.data.FrameCodec
import com.notime.glyphcore.data.FrameCrossfade
import com.notime.glyphcore.data.ReminderFrameGrid
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarClip
import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.CameraMove
import com.notime.glyphsim.matrix.ClipBeat
import com.notime.glyphsim.matrix.ClockCue
import com.notime.glyphsim.matrix.ClockMotion
import com.notime.glyphsim.matrix.MatrixAnimator
import com.notime.glyphsim.matrix.ReactionTrigger
import com.notime.glyphsim.matrix.ReminderAnimations

/**
 * Ordnet jedem Avatar seinen Vorstellungs-Clip zu (siehe [AvatarClip]) - dieselbe
 * Zuordnungs-Idee wie [ReminderRhythm.forType] oder [AvatarAnimations.reactionFor]: eine Stelle,
 * die je Eintrag etwas Fertiges liefert, statt dass Aufrufer selbst Animationsdaten zusammenbauen.
 *
 * Puffling war der erste vollstaendige Clip (siehe Ruecksprache: erst auf einem echten Geraet
 * pruefen, ob sich das Format gut anfuehlt, dann erst das Muster uebertragen) und bekommt deshalb
 * weiterhin seine eigene, handgebaute Choreografie mit der aufwendigeren Rocket-Reaktion. Die
 * anderen fuenf teilen sich [standardClip] - dieselbe Beat-Abfolge, nur mit arteigenem Text, der
 * arteigenen [AvatarSpecies.signatureTopic]-Reaktion und der arteigenen Freuden-Choreografie
 * statt Rocket als Finale.
 *
 * JEDER Clip zeigt vier verschiedene Gewohnheiten: einen ausfuehrlichen ersten Durchlauf mit dem
 * Schwerpunktthema des Avatars und danach eine Montage aus drei weiteren (siehe
 * [montageTopicsFor]). Mit nur einer Animation liesse sich der Kern der App - viele kleine
 * Alltagsgewohnheiten, jede mit eigenem Bild - nicht zeigen; der Clip saehe aus, als koenne der
 * Avatar genau an eine Sache erinnern.
 *
 * Jeder Beat spielt eine BEREITS VORHANDENE Animation (Ruhe-Sequenz, Themen-Reaktion, die echte
 * Erinnerungsanimation) - keine einzige neue Pixel-Animation war fuer die Clips noetig, siehe
 * [ClipBeat]-Klassendoku.
 */
object AvatarClips {
    /**
     * Die drei Zoomstufen aller Clips - bewusst flach gestaffelt.
     *
     * Frueher lief die Kamera von 0.85 bis 1.2 auf einem 220dp grossen Sprite. Der Avatar stand
     * dadurch am Hoehepunkt fast bildfuellend da, und aus den Pixeln wurden Kacheln: das
     * Filigrane der Figuren - der eigentliche Reiz dieser Darstellung - war genau in den
     * wichtigsten Momenten verschwunden. Zusammen mit der kleineren Grundgroesse
     * (AvatarClipPlayer.AVATAR_SIZE) ist der Avatar am Hoehepunkt jetzt rund ein Fuenftel
     * kleiner als vorher.
     *
     * Die Staffelung bleibt erhalten, nur enger: [CAMERA_ENTRY] ist das Heranfahren aus der
     * Ferne, [CAMERA_BASE] die Normalgroesse, in der die Montage laeuft, [CAMERA_CLOSE] der
     * naechste Stand fuer Hoehepunkte. Ein Zoom soll spuerbar sein, nicht dominant.
     */
    private const val CAMERA_ENTRY = 0.9f
    private const val CAMERA_BASE = 1f
    private const val CAMERA_CLOSE = 1.1f

    /**
     * Ruheposition der Uhr: rechts oben ueber dem Avatar, klar getrennt von ihm. Winkel und
     * Abstand liegen beide im Player (siehe [ClockMotion] fuer die Begruendung) - hier steht nur
     * noch, DASS sie stillsteht.
     */
    private fun clockRestMotion() = ClockMotion.Still()

    /**
     * Der Bogen ueber den Kopf hinweg - beginnt exakt an der Ruheposition, damit der Uebergang von
     * Beat 5 zu Beat 6 nahtlos ist (kein Sprung), und endet spiegelbildlich links oben.
     */
    private fun clockArcMotion() = ClockMotion.Arc()

    /** Der Anflug auf den Avatar - die Zustellung, siehe [ClockMotion.Deliver]. */
    private fun clockDeliverMotion() = ClockMotion.Deliver()

    /**
     * Wie lange die Erinnerungsanimation nach ihrem vollen Durchlauf noch stehen bleibt, bevor die
     * Uhr losfaehrt. Ohne diesen Moment setzte die Zustellung im selben Augenblick ein, in dem die
     * Animation endet - man saehe sie zwar ganz, haette aber keine Gelegenheit, sie anzusehen.
     */
    private const val REMINDER_LINGER_MS = 450L

    /** Dauer des Zustell-Beats: Anflug plus der kurze Moment, in dem die Uhr angekommen und weg ist. */
    private const val DELIVER_DURATION_MS = 1200L

    /**
     * Wie viele der drei Gewohnheiten aus [montageTopicsFor] noch in die Montage kommen.
     *
     * Zwei statt drei, weil danach zwei Signatur-Wellen folgen: Mit allen dreien plus den beiden
     * neuen Wellen - und jede Welle ist seit der Zustellung einen Beat laenger - waere der Clip
     * deutlich ueber eine Minute lang geworden. Die Bandbreite der App tragen zwei Beispiele
     * genauso; was danach kommt, gehoert der Figur.
     */
    private const val MONTAGE_HABIT_COUNT = 2

    /**
     * Eine einzelne Erinnerungs-"Welle" fuer die Montage (siehe [montageTopicsFor]) - drei Beats,
     * die genau den Ablauf der echten App nachspielen:
     *
     * 1. **Die Uhr steht da und zeigt die Zeit.** Der Normalzustand, in dem sie fast immer ist.
     *    Ohne Text, ohne Bewegung - das ist die Ruhe zwischen zwei Wellen.
     * 2. **In derselben Uhr laeuft die Erinnerungsanimation** (der Player zeigt dazu einen kurzen
     *    Pop, siehe clockContentKeyFor), begleitet von der Zeile aus [montageLineFor]. Die Uhr
     *    wird nicht ersetzt und wandert nicht - es ist dieselbe Uhr, in der jetzt etwas passiert.
     * 3. **Der Avatar reagiert, die Uhr ist weg.** Kein Clock-Feld, dieselbe Begruendung wie im
     *    echten Dock (siehe DockScreen): sobald die Reaktion laeuft, ist die Erinnerung
     *    angenommen. Der Player blendet sie dafuer schon am Ende von Beat 2 aus (siehe
     *    CLOCK_FADE_OUT_MS), sodass sie sanft verschwindet, statt an der Beatgrenze zu springen.
     *
     * Frueher fehlte Beat 1 und die Uhr sprang direkt mit laufender Animation ins Bild, dicht
     * gefolgt von der Reaktion. Dadurch las sich die Montage als schnelle Abfolge von Effekten -
     * gerade nicht als das, was die App tut: in ruhigen Abstaenden sanft an etwas erinnern.
     */
    private fun montageBeats(
        species: AvatarSpecies,
        idle: AvatarAnimations.AvatarSequence,
        type: AnimationType,
        restDurationMs: Long = 800L,
        reactionDurationMs: Long = 1500L
    ): List<ClipBeat> {
        val frames = ReminderAnimations.framesFor(type)
        val holds = List(frames.size) { MatrixAnimator.CLOCK_FRAME_DELAY_MS }
        val reaction = AvatarAnimations.reactionFor(species, type)
        return wave(
            idle = idle,
            clockFrames = frames,
            clockHolds = holds,
            reaction = reaction,
            textRes = montageLineFor(type),
            restDurationMs = restDurationMs,
            reactionDurationMs = reactionDurationMs
        )
    }

    /**
     * Eine Welle aus Ruhe → Erinnerung → **Zustellung** → Reaktion, gemeinsam genutzt von der
     * Gewohnheiten-Montage ([montageBeats]) und den Signatur-Animationen ([signatureBeats]).
     *
     * **Die Zustellung ist neu und der eigentliche Punkt.** Frueher lief die Animation auf der Uhr,
     * dann schnitt der Clip auf die Reaktion - die Uhr verschwand einfach. Damit fehlte die Geste,
     * um die sich die App dreht: die Erinnerung auf den Avatar schieben. Jetzt laeuft die Animation
     * erst **einmal ganz durch** (Beat 2, Dauer aus der Animation selbst abgeleitet statt geraten),
     * und dann faehrt dieselbe Uhr auf den Avatar zu und geht in ihm auf (Beat 3, siehe
     * [ClockMotion.Deliver]). Erst danach kommt die Reaktion.
     *
     * Alle Reaktions-Beats laufen mit [ClipBeat.playToEnd], damit ihre Choreografie nicht mitten im
     * Sprung abgeschnitten wird.
     */
    private fun wave(
        idle: AvatarAnimations.AvatarSequence,
        clockFrames: List<IntArray>,
        clockHolds: List<Long>,
        reaction: AvatarAnimations.AvatarSequence,
        @StringRes textRes: Int?,
        restDurationMs: Long = 1000L,
        reactionDurationMs: Long = 1500L,
        /**
         * Ob der Welle ein ruhiger Uhrzeit-Beat vorausgeht. In der Gewohnheiten-Montage ja - er ist
         * dort die Aussage "dazwischen ist einfach nur eine Uhr". Bei den Signatur-Wellen nein: Sie
         * folgen direkt aufeinander, und ein vierter identischer Ruhe-Beat haette den Clip nur in
         * die Laenge gezogen, ohne noch etwas zu sagen.
         */
        includeRest: Boolean = true
    ): List<ClipBeat> {
        // Genau ein voller Durchlauf der Erinnerungsanimation, plus etwas Nachklang - nicht mehr
        // eine feste Zahl, die je nach Animation zu kurz war (die Bibliotheks-Animationen sind
        // unterschiedlich lang, von sechs bis neun Keyframes).
        val oneCycleMs = clockHolds.sum()
        val reminderDurationMs = oneCycleMs + REMINDER_LINGER_MS
        return listOfNotNull(
            if (includeRest) {
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = restDurationMs,
                    clock = ClockCue.LiveTime(motion = clockRestMotion())
                )
            } else {
                null
            },
            ClipBeat(
                frames = idle.frames,
                frameHoldsMs = idle.holdsMs,
                durationMs = reminderDurationMs,
                textRes = textRes,
                clock = ClockCue.Animation(clockFrames, clockHolds, motion = clockRestMotion())
            ),
            ClipBeat(
                frames = idle.frames,
                frameHoldsMs = idle.holdsMs,
                durationMs = DELIVER_DURATION_MS,
                clock = ClockCue.Animation(clockFrames, clockHolds, motion = clockDeliverMotion())
            ),
            ClipBeat(
                frames = reaction.frames,
                frameHoldsMs = reaction.holdsMs,
                durationMs = reactionDurationMs,
                playToEnd = true
            )
        )
    }

    /**
     * Die zwei Signatur-Animationen, die im Clip dieses Avatars tatsaechlich vorkommen (siehe
     * [com.notime.glyphcore.data.AvatarSignatureAnimations] fuer alle fuenf je Figur).
     *
     * Nur zwei und nicht alle fuenf: Ein Vorstellungs-Clip soll einen Eindruck geben, keinen
     * Katalog durchgehen - fuenf Wellen hintereinander waeren laenger als der gesamte uebrige Clip.
     * Gewaehlt sind jeweils die beiden Motive, die sich optisch am staerksten voneinander
     * unterscheiden, damit die zwei Wellen nicht wie eine wirken.
     */
    private fun signatureLabelsFor(species: AvatarSpecies): List<String> = when (species) {
        // Etwas Schwebendes und etwas Steigendes - beides Dinge, denen man nachschaut.
        AvatarSpecies.PUFFLING -> listOf("Bubble", "Kite")
        // Ein heller Streifen und ein leises Sinken.
        AvatarSpecies.STARLET -> listOf("Comet", "Feather")
        // Der Schlag und der Aufstieg.
        AvatarSpecies.WYRMLING -> listOf("Bolt", "Summit")
        // Wachen und Halten - seine beiden Seiten.
        AvatarSpecies.FENNEC -> listOf("Lighthouse", "Anchor")
        // Langsam am Boden und langsam in der Luft.
        AvatarSpecies.GLOOP -> listOf("Turtle", "Balloon")
        // Warten und Verstehen.
        AvatarSpecies.HOOTLET -> listOf("Hourglass", "Key")
    }

    /**
     * Eine Welle mit einer Signatur-Animation auf der Uhr und der dazu eigens gebauten Reaktion
     * (siehe `AvatarSignatureReactions`). Anders als in der Montage traegt nur die erste dieser
     * Wellen eine Textzeile - die zweite laesst man einfach zusehen.
     */
    private fun signatureBeats(
        species: AvatarSpecies,
        idle: AvatarAnimations.AvatarSequence,
        label: String,
        @StringRes textRes: Int?
    ): List<ClipBeat> {
        val frames = signatureFrames(label)
        val holds = List(frames.size) { MatrixAnimator.CLOCK_FRAME_DELAY_MS }
        val reaction = AvatarAnimations.reactionFor(species, ReactionTrigger.of(null, label))
        return wave(
            idle = idle,
            clockFrames = frames,
            clockHolds = holds,
            reaction = reaction,
            textRes = textRes,
            reactionDurationMs = 1400L,
            includeRest = false
        )
    }

    /**
     * Loest eine Signatur-Animation in abspielbare Frames auf - derselbe Weg wie
     * [com.notime.glyphcore.data.LibraryAnimationRepository.framesFor], nur ohne Datenbank: Die
     * Punktdaten liegen ohnehin im Code, und ein Clip soll sich nicht darauf verlassen muessen,
     * dass die Bibliothek schon eingespielt wurde.
     */
    private val signatureFrameCache = mutableMapOf<String, List<IntArray>>()

    private fun signatureFrames(label: String): List<IntArray> = signatureFrameCache.getOrPut(label) {
        val data = AvatarSignatureAnimations.seed().firstOrNull { it.label == label }?.framesData
            ?: return@getOrPut emptyList()
        FrameCrossfade.withCrossfades(ReminderFrameGrid.SIZE, FrameCodec.decode(data))
    }

    /**
     * Der Ausklang der Montage: die Uhr kommt noch einmal zurueck und zeigt einfach wieder die
     * Zeit. Damit endet die Reihe dort, wo sie angefangen hat - die Welle laeuft aus, statt dass
     * nach der letzten Reaktion unvermittelt das Finale einsetzt. Zugleich ist es der ehrlichste
     * Schluss-Satz ueber die App: die meiste Zeit ist da nur eine Uhr.
     */
    private fun montageOutroBeat(idle: AvatarAnimations.AvatarSequence) = ClipBeat(
        frames = idle.frames,
        frameHoldsMs = idle.holdsMs,
        durationMs = 1600L,
        clock = ClockCue.LiveTime(motion = clockRestMotion())
    )

    /**
     * Die drei WEITEREN Gewohnheiten, die der Clip nach dem Hoehepunkt vorfuehrt - zusammen mit
     * der bereits gezeigten [AvatarSpecies.signatureTopic] kommt so jeder Clip auf vier
     * unterschiedliche Erinnerungsanimationen. Genau darum geht es: sichtbar zu machen, dass es
     * um ALLTAEGLICHE Gewohnheiten geht und nicht um das eine Thema, auf das der Avatar
     * spezialisiert ist - mit einer einzigen Animation liesse sich das nicht zeigen.
     *
     * Die Auswahl ist bewusst je Avatar anders und erzaehlt einen kleinen Tagesbogen aus seiner
     * Sicht (Wyrmling: bewegen, trinken, konzentrieren, ausruhen), statt ueberall dieselben drei
     * Fuellanimationen zu zeigen. Der eigene [AvatarSpecies.signatureTopic] taucht hier nie
     * erneut auf - er lief schon als ausfuehrlicher erster Durchlauf.
     *
     * Kein neues Pixelmaterial: jeder Eintrag hat laengst seine Erinnerungsanimation
     * ([ReminderAnimations.framesFor]) und seine arteigene Reaktion
     * ([AvatarAnimations.reactionFor]) - siehe Klassendoku.
     */
    private fun montageTopicsFor(species: AvatarSpecies): List<AnimationType> = when (species) {
        // Der Allrounder: Grundgewohnheit, Selbstfreundlichkeit, Ruhe - die Bandbreite der App.
        AvatarSpecies.PUFFLING -> listOf(AnimationType.DRINK, AnimationType.LOVE, AnimationType.MINDFULNESS)
        // Die Traeumerin: freundlich zu sich sein, ein paar Seiten lesen, Pause - lauter leise Dinge.
        AvatarSpecies.STARLET -> listOf(AnimationType.LOVE, AnimationType.BOOK, AnimationType.REST)
        // Der Motivator: nach der Bewegung trinken, dann konzentrieren, dann ausruhen.
        AvatarSpecies.WYRMLING -> listOf(AnimationType.DRINK, AnimationType.FOCUS, AnimationType.REST)
        // Der Beschuetzer: die Dinge, die man am ehesten vergisst, wenn niemand aufpasst.
        AvatarSpecies.FENNEC -> listOf(AnimationType.MEDICINE, AnimationType.MOVE, AnimationType.REST)
        // Der Entschleuniger: von trinken ueber durchatmen bis schlafen - immer ruhiger werdend.
        AvatarSpecies.GLOOP -> listOf(AnimationType.DRINK, AnimationType.MINDFULNESS, AnimationType.SLEEP)
        // Der Beobachter: lesen, arbeiten, danach Pause - sein konzentrierter Tag.
        AvatarSpecies.HOOTLET -> listOf(AnimationType.BOOK, AnimationType.WORK, AnimationType.REST)
    }

    /**
     * Die kurze Zeile, die der Avatar zu einer Gewohnheit sagt, waehrend deren Animation auf der
     * Uhr laeuft.
     *
     * Frueher stand hier schlicht [AnimationType.labelRes] ("Trinken") - der Name der Animation
     * neben der Animation selbst, also doppelt dasselbe. Stattdessen jetzt ein kleiner Satz in
     * derselben Stimme wie die uebrigen Cliptexte ("Ein Schluck Wasser?"), damit die Montage
     * nicht wie eine Aufzaehlung wirkt, sondern wie der Avatar, der einen durch den Tag begleitet.
     *
     * Bewusst typ- statt artgebunden und vollstaendig ueber alle [AnimationType]: die Zeile
     * beschreibt die Gewohnheit, den arteigenen Ton traegt die Reaktion daneben (siehe
     * [montageBeats]). So laesst sich [montageTopicsFor] frei umstellen, ohne dass Text fehlt.
     */
    @StringRes
    private fun montageLineFor(type: AnimationType): Int = when (type) {
        AnimationType.FOCUS -> R.string.clip_habit_focus
        AnimationType.DRINK -> R.string.clip_habit_drink
        AnimationType.MOVE -> R.string.clip_habit_move
        AnimationType.GENERAL -> R.string.clip_habit_general
        AnimationType.REST -> R.string.clip_habit_rest
        AnimationType.WORK -> R.string.clip_habit_work
        AnimationType.MINDFULNESS -> R.string.clip_habit_mindfulness
        AnimationType.LOVE -> R.string.clip_habit_love
        AnimationType.SLEEP -> R.string.clip_habit_sleep
        AnimationType.MEDICINE -> R.string.clip_habit_medicine
        AnimationType.BOOK -> R.string.clip_habit_book
        AnimationType.CREATIVITY -> R.string.clip_habit_creativity
    }

    fun forSpecies(species: AvatarSpecies): AvatarClip? = when (species) {
        AvatarSpecies.PUFFLING -> pufflingClip()
        AvatarSpecies.STARLET -> standardClip(
            AvatarSpecies.STARLET,
            R.string.clip_starlet_line_1, R.string.clip_starlet_line_2, R.string.clip_starlet_line_3,
            R.string.clip_starlet_line_4, R.string.clip_starlet_line_5, R.string.clip_starlet_line_6
        )
        AvatarSpecies.WYRMLING -> standardClip(
            AvatarSpecies.WYRMLING,
            R.string.clip_wyrmling_line_1, R.string.clip_wyrmling_line_2, R.string.clip_wyrmling_line_3,
            R.string.clip_wyrmling_line_4, R.string.clip_wyrmling_line_5, R.string.clip_wyrmling_line_6
        )
        AvatarSpecies.FENNEC -> standardClip(
            AvatarSpecies.FENNEC,
            R.string.clip_fennec_line_1, R.string.clip_fennec_line_2, R.string.clip_fennec_line_3,
            R.string.clip_fennec_line_4, R.string.clip_fennec_line_5, R.string.clip_fennec_line_6
        )
        AvatarSpecies.GLOOP -> standardClip(
            AvatarSpecies.GLOOP,
            R.string.clip_gloop_line_1, R.string.clip_gloop_line_2, R.string.clip_gloop_line_3,
            R.string.clip_gloop_line_4, R.string.clip_gloop_line_5, R.string.clip_gloop_line_6
        )
        AvatarSpecies.HOOTLET -> standardClip(
            AvatarSpecies.HOOTLET,
            R.string.clip_hootlet_line_1, R.string.clip_hootlet_line_2, R.string.clip_hootlet_line_3,
            R.string.clip_hootlet_line_4, R.string.clip_hootlet_line_5, R.string.clip_hootlet_line_6
        )
    }

    /**
     * Dieselbe Beat-Abfolge wie [pufflingClip] (Heranzoom-Gruss, zwei ruhige Saetze, die Uhr
     * erscheint mit der arteigenen Erinnerungsanimation und zieht ueber den Kopf, Reaktion darauf,
     * Ueberleitung, Gewohnheiten-Montage, Finale, Leitsatz), aber datengetrieben ueber [species]:
     * der erste ausfuehrliche Durchlauf zeigt IMMER die arteigene
     * [AvatarSpecies.signatureTopic]-Reaktion statt einer Rocket-artigen Sonderchoreografie -
     * deshalb auch keine [ClipBeat.flightOffsets], die Reaktion spielt an Ort und Stelle.
     *
     * Das Finale ist [AvatarAnimations.tapReaction], die arteigene Freuden-Choreografie (Starlet
     * funkelt, Gloop schwappt ueber, ...) - der Platz, den bei Puffling die Rakete einnimmt. Sie
     * beantwortet keine Erinnerung und darf deshalb nach der Montage stehen, ohne dass "Glocke
     * rein, Rakete raus" entsteht; zugleich wiederholt sie nicht die Signature-Reaktion von Beat 7.
     */
    private fun standardClip(
        species: AvatarSpecies,
        @StringRes line1: Int,
        @StringRes line2: Int,
        @StringRes line3: Int,
        @StringRes line4: Int,
        @StringRes line5: Int,
        @StringRes line6: Int
    ): AvatarClip {
        val idle = AvatarAnimations.idleSequence(species, AvatarMood.NEUTRAL)
        val highlight = AvatarAnimations.reactionFor(species, species.signatureTopic)
        val finale = AvatarAnimations.tapReaction(species)

        val reminderFrames = ReminderAnimations.framesFor(species.signatureTopic)
        val reminderHolds = List(reminderFrames.size) { MatrixAnimator.CLOCK_FRAME_DELAY_MS }

        val beats = buildList {
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = 1300L,
                    camera = CameraMove.zoomIn(from = CAMERA_ENTRY, to = CAMERA_BASE),
                    textRes = line1
                )
            )
            add(ClipBeat(idle.frames, idle.holdsMs, 2000L, textRes = line2))
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = 1800L,
                    camera = CameraMove(fromOffsetFraction = 0f to 0f, toOffsetFraction = 0.04f to 0f),
                    textRes = line3
                )
            )
            // Erster funktionaler Moment: die Uhr erscheint - zeigt zunaechst die ECHTE,
            // aktuelle Uhrzeit (siehe ClockCue.LiveTime), genau wie sie im Alltag die meiste
            // Zeit dasteht.
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = 1600L,
                    textRes = line4,
                    clock = ClockCue.LiveTime(motion = clockRestMotion())
                )
            )
            // Zweiter funktionaler Moment: an derselben Position wechselt sie auf die echte
            // Erinnerungsanimation der Spezies - der Moment, in dem "etwas passiert" (der
            // Player zeigt hier einen kurzen Pop, siehe clockContentKeyFor dort). Die Dauer kommt
            // aus der Animation selbst, damit sie garantiert einmal ganz durchlaeuft.
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = reminderHolds.sum() + REMINDER_LINGER_MS,
                    textRes = line5,
                    clock = ClockCue.Animation(reminderFrames, reminderHolds, motion = clockRestMotion())
                )
            )
            // Dritter funktionaler Moment: dieselbe Animation (identische Frame-Liste, siehe
            // clockContentKeyFor - deshalb kein erneuter Pop) faehrt jetzt auf den Avatar zu und
            // geht in ihm auf (siehe ClockMotion.Deliver). Frueher zog sie nur in einem Bogen
            // ueber seinen Kopf hinweg und verschwand daneben - das las sich als "Uhr geht
            // vorbei" statt als "Erinnerung kommt an".
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = DELIVER_DURATION_MS,
                    camera = CameraMove.zoomIn(from = CAMERA_BASE, to = CAMERA_CLOSE),
                    textRes = line6,
                    clock = ClockCue.Animation(reminderFrames, reminderHolds, motion = clockDeliverMotion())
                )
            )
            // Reaktion auf die eben zugestellte Erinnerung, bewusst ohne eigenen Text - dieselbe
            // Stelle und Begruendung wie im Puffling-Clip. playToEnd, damit die Choreografie nicht
            // mitten im Sprung abgeschnitten wird.
            add(
                ClipBeat(
                    frames = highlight.frames,
                    frameHoldsMs = highlight.holdsMs,
                    durationMs = 1600L,
                    playToEnd = true,
                    camera = CameraMove(fromScale = CAMERA_CLOSE, toScale = CAMERA_CLOSE)
                )
            )
            // Ueberleitung zur Montage: dieselben zwei Saetze wie bei Puffling (siehe
            // clip_montage_intro_*) - sie erklaeren, was gleich zu sehen ist, und sind bewusst
            // nicht arteigen formuliert, weil sie ueber die App sprechen, nicht ueber die Figur.
            add(
                ClipBeat(
                    idle.frames, idle.holdsMs, 1300L,
                    camera = CameraMove.zoomOut(from = CAMERA_CLOSE, to = CAMERA_BASE),
                    textRes = R.string.clip_montage_intro_1
                )
            )
            add(ClipBeat(idle.frames, idle.holdsMs, 1300L, textRes = R.string.clip_montage_intro_2))
            // Zwei weitere Gewohnheiten, jede als eigene Welle aus Ruhe, Erinnerung, Zustellung
            // und Reaktion (siehe montageBeats/montageTopicsFor).
            montageTopicsFor(species).take(MONTAGE_HABIT_COUNT)
                .forEach { type -> addAll(montageBeats(species, idle, type)) }
            // Danach zwei Motive, die nur DIESEM Avatar gehoeren (siehe signatureLabelsFor) -
            // erst hier verlaesst der Clip die allgemeinen Gewohnheiten und wird zur Vorstellung
            // der Figur selbst. Nur die erste Welle traegt eine Zeile.
            signatureLabelsFor(species).forEachIndexed { index, label ->
                addAll(
                    signatureBeats(
                        species = species,
                        idle = idle,
                        label = label,
                        textRes = if (index == 0) R.string.clip_signature_intro else null
                    )
                )
            }
            add(montageOutroBeat(idle))
            // Finale an der Stelle, an der Puffling die Rakete zeigt: die arteigene
            // Freuden-Choreografie (siehe Funktionsdoku oben), ohne Text - hier spricht nur die
            // Bewegung. Kamera zieht aus der Montage-Normalgroesse wieder heran.
            add(
                ClipBeat(
                    frames = finale.frames,
                    frameHoldsMs = finale.holdsMs,
                    durationMs = 1800L,
                    playToEnd = true,
                    camera = CameraMove.zoomIn(from = CAMERA_BASE, to = CAMERA_CLOSE)
                )
            )
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = 2400L,
                    camera = CameraMove.zoomOut(from = CAMERA_CLOSE, to = CAMERA_BASE),
                    textRes = species.taglineRes
                )
            )
        }

        return AvatarClip(beats)
    }

    private fun pufflingClip(): AvatarClip {
        val idle = AvatarAnimations.idleSequence(AvatarSpecies.PUFFLING, AvatarMood.NEUTRAL)
        val rocketTrigger = ReactionTrigger.of(null, "Rocket")
        val rocket = AvatarAnimations.reactionFor(AvatarSpecies.PUFFLING, rocketTrigger)
        val rocketFlight = AvatarAnimations.flightOffsetsFor(rocketTrigger)

        // Dieselbe Erinnerungsanimation, die auch im echten Betrieb auf der Uhr laeuft (Puffling
        // = GENERAL, siehe AvatarSpecies.signatureTopic) - kein eigens fuer den Clip gebautes
        // "huebscheres" Duplikat, sonst weicht der Clip von dem ab, was Nutzer taeglich sehen.
        val reminderFrames = ReminderAnimations.framesFor(AnimationType.GENERAL)
        val reminderHolds = List(reminderFrames.size) { MatrixAnimator.CLOCK_FRAME_DELAY_MS }

        val beats = buildList {
            // "Hallo." - sanfter Heranzoom, die Kreatur wird erst allmaehlich zum Blickfang statt
            // sofort gross im Bild zu stehen.
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = 1300L,
                    camera = CameraMove.zoomIn(from = CAMERA_ENTRY, to = CAMERA_BASE),
                    textRes = R.string.clip_puffling_line_1
                )
            )
            add(ClipBeat(idle.frames, idle.holdsMs, 2000L, textRes = R.string.clip_puffling_line_2))
            // Leichter Pan statt weiterem Zoom - Abwechslung in der Kamerafuehrung, ohne dass die
            // Kreatur dabei aus dem Bild wandert (Bruchteil bleibt klein).
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = 1800L,
                    camera = CameraMove(fromOffsetFraction = 0f to 0f, toOffsetFraction = 0.04f to 0f),
                    textRes = R.string.clip_puffling_line_3
                )
            )
            // Erster funktionaler Moment: die Uhr erscheint - zeigt zunaechst die ECHTE, aktuelle
            // Uhrzeit (siehe ClockCue.LiveTime), genau wie sie im Alltag die meiste Zeit dasteht.
            // Ruhig, seitlich, kein Kamera-Sprung.
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = 1600L,
                    textRes = R.string.clip_puffling_line_4,
                    clock = ClockCue.LiveTime(motion = clockRestMotion())
                )
            )
            // Zweiter funktionaler Moment: an derselben Position wechselt sie auf die echte
            // Erinnerungsanimation - der Moment, in dem "etwas passiert" (der Player zeigt hier
            // einen kurzen Pop, siehe clockContentKeyFor dort).
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = reminderHolds.sum() + REMINDER_LINGER_MS,
                    textRes = R.string.clip_puffling_line_5,
                    clock = ClockCue.Animation(reminderFrames, reminderHolds, motion = clockRestMotion())
                )
            )
            // Dritter funktionaler Moment: dieselbe Animation (identische Frame-Liste, siehe
            // clockContentKeyFor - deshalb kein erneuter Pop) faehrt auf Puffling zu und geht in
            // ihm auf (siehe ClockMotion.Deliver) - die Erinnerung kommt tatsaechlich an, statt
            // wie frueher nur in einem Bogen daran vorbeizuziehen.
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = DELIVER_DURATION_MS,
                    camera = CameraMove.zoomIn(from = CAMERA_BASE, to = CAMERA_CLOSE),
                    textRes = R.string.clip_puffling_line_6,
                    clock = ClockCue.Animation(reminderFrames, reminderHolds, motion = clockDeliverMotion())
                )
            )
            // Reaktion auf die GENERAL-Erinnerung von eben, bewusst ohne eigenen Text: die Uhr ist
            // gerade "angekommen", jetzt spricht nur noch die Reaktion. TYPE-basiert (GENERAL)
            // statt der Rocket-Sonderreaktion - die gehoert erst zum Abspann (siehe unten), sonst
            // passt "Glocke rein, Rakete raus" inhaltlich nicht zusammen. Kamera haelt die Groesse
            // von eben, damit kein Sprung entsteht.
            val generalReaction = AvatarAnimations.reactionFor(AvatarSpecies.PUFFLING, AnimationType.GENERAL)
            add(
                ClipBeat(
                    frames = generalReaction.frames,
                    frameHoldsMs = generalReaction.holdsMs,
                    durationMs = 1600L,
                    playToEnd = true,
                    camera = CameraMove(fromScale = CAMERA_CLOSE, toScale = CAMERA_CLOSE)
                )
            )
            // Ueberleitung zur Montage unten: erklaert in zwei kurzen Saetzen (derselbe
            // Zwei-Beat-Satzbau wie oben bei den Zeilen 4/5), dass es fuer jede Gewohnheit eine
            // eigene Animation gibt und die Haeufigkeit frei waehlbar ist - genau die zwei Punkte,
            // die sich rein visuell nicht von selbst erklaeren wuerden. Kamera zoomt dabei zurueck
            // auf Normalgroesse, damit die folgende Montage nicht "zu nah dran" wirkt.
            add(
                ClipBeat(
                    idle.frames, idle.holdsMs, 1300L,
                    camera = CameraMove.zoomOut(from = CAMERA_CLOSE, to = CAMERA_BASE),
                    textRes = R.string.clip_montage_intro_1
                )
            )
            add(ClipBeat(idle.frames, idle.holdsMs, 1300L, textRes = R.string.clip_montage_intro_2))
            // Die eigentliche Montage: drei weitere, inhaltlich klar unterschiedene Erinnerungen,
            // jede als eigene Welle aus Ruhe, Erinnerung und Reaktion - siehe
            // [montageTopicsFor]/[montageBeats]. Bewusst keine Ernaehrungs-/Trink-Dopplung und ein
            // ruhiger Ausklang (MINDFULNESS, danach die Uhr allein) vor dem Rocket-Finale.
            montageTopicsFor(AvatarSpecies.PUFFLING).take(MONTAGE_HABIT_COUNT).forEach { type ->
                addAll(montageBeats(AvatarSpecies.PUFFLING, idle, type))
            }
            // Danach seine eigenen zwei Motive - Blase und Drachen, siehe signatureLabelsFor.
            signatureLabelsFor(AvatarSpecies.PUFFLING).forEachIndexed { index, label ->
                addAll(
                    signatureBeats(
                        species = AvatarSpecies.PUFFLING,
                        idle = idle,
                        label = label,
                        textRes = if (index == 0) R.string.clip_signature_intro else null
                    )
                )
            }
            add(montageOutroBeat(idle))
            // Der eigentliche Hoehepunkt, bewusst ohne eigenen Text: Rocket statt einer weiteren
            // typ-gebundenen Reaktion - deutlich aufwendiger produziert, der Effekt "und manchmal
            // etwas ganz Besonderes" nach der Themen-Uebersicht. Kamera zoomt aus der Montage-
            // Normalgroesse wieder heran, waehrend die Flugbewegung selbst (flightOffsets) die
            // Dynamik traegt.
            add(
                ClipBeat(
                    frames = rocket.frames,
                    frameHoldsMs = rocket.holdsMs,
                    durationMs = 1800L,
                    camera = CameraMove.zoomIn(from = CAMERA_BASE, to = CAMERA_CLOSE),
                    flightOffsets = rocketFlight
                )
            )
            // Abschluss mit dem Leitsatz der Spezies (siehe AvatarSpecies.taglineRes) statt
            // eigenem, neu erfundenem Text - derselbe Satz taucht so konsistent ueberall auf, wo
            // der Charakter sich vorstellt.
            add(
                ClipBeat(
                    frames = idle.frames,
                    frameHoldsMs = idle.holdsMs,
                    durationMs = 2400L,
                    camera = CameraMove.zoomOut(from = CAMERA_CLOSE, to = CAMERA_BASE),
                    textRes = AvatarSpecies.PUFFLING.taglineRes
                )
            )
        }

        return AvatarClip(beats)
    }
}
