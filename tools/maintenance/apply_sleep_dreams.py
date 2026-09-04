from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


routine = Path("app-sim/src/main/java/com/notime/glyphsim/matrix/PlayRoutine.kt")
text = routine.read_text(encoding="utf-8")
marker = "    /** Einen Moment nichts tun (Ruhe-Schleife laeuft weiter). */\n    data class Linger(val millis: Long) : RoutineStep\n"
replacement = """    /** Einen Moment nichts tun (Ruhe-Schleife laeuft weiter). */
    data class Linger(val millis: Long) : RoutineStep

    /** Exklusiver Schlaf: Im Bett bleiben, bis die Nacht endet. */
    data object SleepUntilMorning : RoutineStep
"""
if marker not in text:
    raise SystemExit("RoutineStep Linger marker not found")
text = text.replace(marker, replacement, 1)
start = text.index("        // ---- Schlafen: hingehen, hineinlegen, liegen bleiben ----")
end = text.index("        // ---- Ausruhen: aufs Sofa, Fernseher an ----", start)
sleep_block = """        // ---- Schlafen: einmal hineinlegen und bis zum Aufwachen dort bleiben ----
        AnimationType.SLEEP -> listOf(
            PlayRoutine(
                listOf(
                    RoutineStep.GoTo(PlayScene.Station.BED),
                    RoutineStep.Stir(AvatarAnimations.Fidget.YAWN),
                    RoutineStep.Occupy(PlayScene.Station.BED),
                    RoutineStep.Act(AnimationType.SLEEP),
                    RoutineStep.SleepUntilMorning,
                    RoutineStep.Rise,
                    RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH)
                )
            )
        )

"""
routine.write_text(text[:start] + sleep_block + text[end:], encoding="utf-8")

dock = "app-sim/src/main/java/com/notime/glyphsim/ui/DockScreen.kt"
replace_once(
    dock,
    "import com.notime.glyphsim.matrix.PlayEffects\nimport com.notime.glyphsim.matrix.PlayPantry\n",
    "import com.notime.glyphsim.matrix.PlayEffects\nimport com.notime.glyphsim.matrix.PlayDreamMemory\nimport com.notime.glyphsim.matrix.PlayDreams\nimport com.notime.glyphsim.matrix.PlayPantry\n",
)
replace_once(
    dock,
    "        /** Sichtbare Phase der Angel-Szene am Teich; null ausserhalb dieses Ablaufs. */\n        var fishingPhase by remember { mutableStateOf<PlayEffects.FishingPhase?>(null) }\n",
    """        /** Sichtbare Phase der Angel-Szene am Teich; null ausserhalb dieses Ablaufs. */
        var fishingPhase by remember { mutableStateOf<PlayEffects.FishingPhase?>(null) }
        /** Rein visuelle Traumprojektion. Der echte Avatar bleibt waehrenddessen im Bett. */
        var dreamFrame by remember { mutableStateOf<IntArray?>(null) }
        var dreamProgress by remember { mutableFloatStateOf(0f) }
""",
)
helper_marker = """        /**
         * Fuehrt einen Tagesablauf aus (siehe [PlayRoutine]) - **die Stelle, an der aus Posen
"""
helper = """        /** Spielt eine vorhandene Reaktion als kleine Traumprojektion, ohne den Avatar zu bewegen. */
        suspend fun playDream(topic: AnimationType, species: AvatarSpecies) {
            val reaction = AvatarAnimations.reactionFor(species, topic)
            val first = reaction.frames.firstOrNull() ?: return
            dreamFrame = first
            dreamProgress = 0f
            try {
                coroutineScope {
                    launch {
                        MatrixAnimator.playTimed(reaction.frames, reaction.holdsMs) { f -> dreamFrame = f }
                    }
                    animate(0f, 1f, animationSpec = tween(DREAM_BUBBLE_MS, easing = FastOutSlowInEasing)) { value, _ ->
                        dreamProgress = value
                    }
                }
            } finally {
                dreamFrame = null
                dreamProgress = 0f
            }
        }

""" + helper_marker
replace_once(dock, helper_marker, helper)
replace_once(
    dock,
    "                    is RoutineStep.Act -> {\n                        activeActivity = step.topic\n",
    "                    is RoutineStep.Act -> {\n                        activeActivity = step.topic\n                        PlayDreamMemory.remember(context, step.topic)\n",
)
linger_marker = """                    // Verweilen ist vergehende ZEIT und wird im Zeitraffer entsprechend gekuerzt -
                    // sonst haetten die Pausen zwischen den Regungen ein anderes Tempo als die
"""
sleep_handler = """                    RoutineStep.SleepUntilMorning -> {
                        // Tagsueber bleibt eine ausdrueckliche Schlafhandlung ein kurzes Nickerchen.
                        // Nachts dagegen bleibt diese Routine aktiv und sperrt damit autonome
                        // Fidgets, Wanderungen und neue Perform-Aktionen bis zum Morgen.
                        if (PlayAmbientActivity.currentDayPhase() != PlayAmbientActivity.DayPhase.NIGHT) {
                            delay((8_000L * PlayTimeLapse.paceFactor()).toLong().coerceAtLeast(800L))
                        } else {
                            while (PlayAmbientActivity.currentDayPhase() == PlayAmbientActivity.DayPhase.NIGHT) {
                                var remaining = (PlayDreams.nextPauseMillis() * PlayTimeLapse.paceFactor())
                                    .toLong().coerceAtLeast(1_000L)
                                while (remaining > 0L && PlayAmbientActivity.currentDayPhase() == PlayAmbientActivity.DayPhase.NIGHT) {
                                    val sleeping = avatar ?: return
                                    if (sleeping.fed || sleeping.occurrenceId != null) return
                                    val slice = minOf(remaining, DREAM_SLEEP_CHECK_MS)
                                    delay(slice)
                                    remaining -= slice
                                }
                                if (PlayAmbientActivity.currentDayPhase() != PlayAmbientActivity.DayPhase.NIGHT) break
                                val sleeping = avatar ?: return
                                if (sleeping.fed || sleeping.occurrenceId != null) return
                                if (PlayDreams.shouldDream()) {
                                    PlayDreams.choose(PlayDreamMemory.today(context))?.let { memory ->
                                        playDream(memory, species)
                                    }
                                }
                            }
                        }
                    }

""" + linger_marker
replace_once(dock, linger_marker, sleep_handler)
front_marker = "        // Getragener Gegenstand und Zugriffs-Blitz - beide VOR dem Avatar, weil er sie in der Hand\n"
dream_render = """        // Traumblase ueber dem Bett. Sie ist eine Projektion; der reale Avatar bleibt im Bett.
        if (playMode && occupiedStation == PlayScene.Station.BED) {
            val projected = dreamFrame
            val sleeping = avatar
            if (projected != null && sleeping != null && !avatarHidden) {
                PlayDreamBubble(projected, sleeping.offset, sleeping.sizeDp, dreamProgress, maxWidthPx)
            }
        }

""" + front_marker
replace_once(dock, front_marker, dream_render)
replace_once(
    dock,
    "                fishingPhase = null\n                // Und zurueck auf den Boden:",
    "                fishingPhase = null\n                dreamFrame = null\n                dreamProgress = 0f\n                // Und zurueck auf den Boden:",
)
replace_once(
    dock,
    "private const val SETTLE_INTO_MS = 420\n",
    """private const val SETTLE_INTO_MS = 420
private const val DREAM_BUBBLE_MS = 6_200
private const val DREAM_SLEEP_CHECK_MS = 800L
""",
)

evolution = Path("EVOLUTION.md")
evo = evolution.read_text(encoding="utf-8")
note = """

### Schlaf & Traeume – exklusiver Nachtzustand

- Schlaf ist nachts exklusiv: Nach dem Hinlegen bleibt der reale Avatar bis zum Morgen im Bett. Die fruehere Routine mit Aufstehen/Umsehen/Wieder-Hinlegen wurde entfernt.
- Echte Erinnerungen oder ausdrueckliche Nutzeraktionen duerfen Schlaf unterbrechen; autonome Fidgets/Wanderungen nicht.
- Tageshandlungen werden als kleine semantische `AnimationType`-Erinnerungen gespeichert, nicht als Screenshots oder Videos.
- Seltene Traumgelegenheiten spielen eine vorhandene Avatar-Reaktion in einer wachsenden, aufsteigenden Traumblase. Die Traumfigur ist nur Projektion; Weltposition und Bettbelegung bleiben unveraendert.
- `SLEEP` und `MEDICINE` werden nie als Traumerinnerung gespeichert.
- Spaeter: reichere Erinnerungs-IDs und interaktive Dream-Easter-Eggs. Der Luftballon wird erst an einen echten Ballon-Node/Item angeschlossen; aktuell existiert im Repository noch keiner, deshalb wird kein Parallel-Item erfunden.
"""
if "### Schlaf & Traeume – exklusiver Nachtzustand" not in evo:
    evolution.write_text(evo.rstrip() + note + "\n", encoding="utf-8")
