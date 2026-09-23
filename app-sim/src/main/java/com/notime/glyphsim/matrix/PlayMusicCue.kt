package com.notime.glyphsim.matrix

/**
 * **Der Auftritt eines Wesens in der Musik** - wenn ein Gast von aussen ins Bild kommt, spielt
 * kurz SEIN Thema an, und danach kehrt der Score der Szene zurueck.
 *
 * ## Die Idee
 *
 * Im Film und im Spiel heisst das Leitmotiv: Man hoert, WER kommt, bevor man ihn genau erkannt
 * hat. Hier ist die Figur 16x16 Punkte gross; der Klang traegt die Identitaet deshalb staerker
 * als das Bild. Ein Gast, der mit seinem eigenen Motiv hereinkommt, ist sofort jemand - nicht
 * irgendein Pixelhaufen am Rand.
 *
 * ## Warum nur ein kurzes Anspielen und kein Rollenwechsel
 *
 * [PlayCharacterTheme] spielt das Stueck des EIGENEN Wesens einmal ganz, weil es dessen Tag
 * eroeffnet. Ein Gast eroeffnet nichts, er kommt vorbei. Sein Thema ist deshalb ein Einspieler
 * ueber der Szene: Die Szenenmusik taucht ab ([PlayMusic][com.notime.glyphsim.ui.PlayMusic]
 * duckt sie auf null, sie laeuft aber leise weiter), das Motiv des Gasts klingt auf, und wenn es
 * gesagt ist, taucht die Szene wieder auf - an der Stelle, an der sie ohnehin gerade waere, und
 * nicht von vorn. Die Welt hat nicht angehalten, nur weil jemand hereinkam.
 *
 * ## Die Dauer
 *
 * Alle sechs Themen sind so beschrieben, dass ihr Motiv spaetestens alle zwei Takte wiederkehrt
 * und das Stueck ohne Vorspiel anfaengt. Bei 66 bis 96 BPM sind acht Takte 20 bis 29 Sekunden -
 * eine vollstaendige Phrase. [MAX_HOLD_MS] laesst genau so viel klingen; [MIN_HOLD_MS] sorgt
 * dafuer, dass auch ein eiliger Gast sein Motiv mindestens zweimal hoeren laesst, statt nach
 * einem halben Takt wieder abzubrechen.
 *
 * ## Warum so sparsam
 *
 * Im Park laufen bis zu vier Gaeste gleichzeitig (siehe [LivingPopulationLayout.visitorCapFor]).
 * Bekaeme jeder einen Einspieler, spraenge die Musik alle halbe Minute zwischen fuenf Stuecken
 * hin und her - das ist kein Leitmotiv mehr, sondern Senderwechsel. Deshalb:
 *
 * - **Hoechstens ein Einspieler zugleich**, und kein zweiter wartet in einer Schlange.
 * - **Globale Pause [GLOBAL_COOLDOWN_MS]** zwischen zwei Einspielern.
 * - **Pause je Wesen [SPECIES_COOLDOWN_MS]** - wer gerade da war und wiederkommt, ist bekannt.
 *
 * Die Regeln stehen hier ohne Android, damit sie sich pruefen lassen; der Player und die Uhr
 * liegen bei `PlayMusic` und `DockScreen`.
 */
object PlayMusicCue {

    /** Wie schnell das Motiv des Gasts ueber der Szene aufklingt - ungefaehr ein Takt. */
    const val FADE_IN_MS = 2_500L

    /** Mindestens so lange klingt das Thema, auch wenn der Gast vorher geht. */
    const val MIN_HOLD_MS = 12_000L

    /** Hoechstens so lange - eine Phrase von acht Takten, siehe Klassendoku. */
    const val MAX_HOLD_MS = 24_000L

    /**
     * Wie langsam die Szene zurueckkehrt. Bewusst laenger als [FADE_IN_MS]: Ein Auftritt darf
     * ploetzlich sein, ein Abgang nicht - sonst klingt er wie ein abgerissenes Band.
     */
    const val FADE_OUT_MS = 5_000L

    /** Zwischen zwei Einspielern, egal von wem. */
    const val GLOBAL_COOLDOWN_MS = 120_000L

    /** Bis dasselbe Wesen wieder mit seinem Motiv hereinkommen darf. */
    const val SPECIES_COOLDOWN_MS = 8 * 60_000L

    /** Wann zuletzt ein Einspieler begann - insgesamt und je Wesen. */
    data class Memory(
        val lastCueAtMs: Long? = null,
        val lastBySpecies: Map<AvatarSpecies, Long> = emptyMap()
    )

    /** Das Ergebnis von [judge]. Nur [PLAY] spielt; die anderen benennen den Grund fuer Stille. */
    enum class Verdict {
        PLAY,

        /** Es laeuft keine Szenenmusik. Ein Einspieler schaltet Musik nie selbst ein. */
        NO_MUSIC,

        /** Das Stueck des eigenen Wesens laeuft gerade - das hat Vorrang vor jedem Gast. */
        GREETING_PLAYING,

        /** Der Gast ist vom selben Wesen wie der Bewohner. Sein Motiv sagte nicht "jemand anderes". */
        SAME_AS_HOST,

        /** Ein anderer Einspieler klingt noch. */
        CUE_RUNNING,

        /** Das Thema genau dieses Wesens liegt nicht im Paket - ein fremdes waere schlimmer. */
        NO_TRACK,

        /** Der letzte Einspieler ist noch nicht lange genug her. */
        COOLDOWN,

        /** Dieses Wesen hatte vor Kurzem schon seinen Auftritt. */
        SPECIES_COOLDOWN
    }

    /**
     * Ob der Gast [guest] beim Hereinkommen sein Thema anspielen darf.
     *
     * [playingRole] ist die Rolle, die der Player gerade traegt (`null` = still),
     * [themeVariants] die ausgelieferten Varianten von [MusicRole.CHARACTER_THEME].
     */
    fun judge(
        guest: AvatarSpecies,
        host: AvatarSpecies?,
        playingRole: MusicRole?,
        cueRunning: Boolean,
        themeVariants: Collection<Int>,
        memory: Memory,
        nowMs: Long
    ): Verdict = when {
        playingRole == null -> Verdict.NO_MUSIC
        playingRole == MusicRole.CHARACTER_THEME -> Verdict.GREETING_PLAYING
        guest == host -> Verdict.SAME_AS_HOST
        cueRunning -> Verdict.CUE_RUNNING
        MusicRole.characterThemeVariant(guest) !in themeVariants -> Verdict.NO_TRACK
        within(memory.lastCueAtMs, nowMs, GLOBAL_COOLDOWN_MS) -> Verdict.COOLDOWN
        within(memory.lastBySpecies[guest], nowMs, SPECIES_COOLDOWN_MS) -> Verdict.SPECIES_COOLDOWN
        else -> Verdict.PLAY
    }

    /** Haelt fest, dass [guest] um [nowMs] seinen Einspieler bekommen hat. */
    fun remember(memory: Memory, guest: AvatarSpecies, nowMs: Long): Memory =
        Memory(lastCueAtMs = nowMs, lastBySpecies = memory.lastBySpecies + (guest to nowMs))

    /**
     * Ob der Einspieler jetzt ausklingen soll.
     *
     * [elapsedMs] zaehlt ab seinem Beginn, [guestLeaving] wird wahr, sobald der Gast sich zum
     * Gehen wendet (oder der Besuch abbricht). Wer lange bleibt, behaelt sein Thema trotzdem
     * nicht laenger als [MAX_HOLD_MS] - danach gehoert die Szene wieder allen.
     */
    fun releaseDue(elapsedMs: Long, guestLeaving: Boolean): Boolean =
        elapsedMs >= MAX_HOLD_MS || (guestLeaving && elapsedMs >= MIN_HOLD_MS)

    /**
     * Eine Uhr, die rueckwaerts gestellt wurde, haelt keine Pause fest: Ein negativer Abstand
     * zaehlt als "lange her". Sonst sperrte eine Zeitumstellung den Einspieler stundenlang.
     */
    private fun within(sinceMs: Long?, nowMs: Long, windowMs: Long): Boolean {
        if (sinceMs == null) return false
        val elapsed = nowMs - sinceMs
        return elapsed in 0L until windowMs
    }
}
