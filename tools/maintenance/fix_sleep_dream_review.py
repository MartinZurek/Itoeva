from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


# 1) Dream memory follows the same simulated day as the world.
time_path = "app-sim/src/main/java/com/notime/glyphsim/matrix/PlayTimeLapse.kt"
replace_once(
    time_path,
    "import java.time.LocalTime\n",
    "import java.time.LocalDate\nimport java.time.LocalTime\n",
)
replace_once(
    time_path,
    """    fun now(): LocalTime {
        val current = speed
        if (current == Speed.OFF) return LocalTime.now()
        val elapsedSeconds = (SystemClock.elapsedRealtime() - startedAtMillis) / 1000.0
        val dayFraction = (elapsedSeconds / current.daySeconds) % 1.0
        return lapseStart.plusSeconds((dayFraction * 24 * 60 * 60).toLong())
    }

""",
    """    fun now(): LocalTime {
        val current = speed
        if (current == Speed.OFF) return LocalTime.now()
        val elapsedSeconds = (SystemClock.elapsedRealtime() - startedAtMillis) / 1000.0
        val dayFraction = (elapsedSeconds / current.daySeconds) % 1.0
        return lapseStart.plusSeconds((dayFraction * 24 * 60 * 60).toLong())
    }

    /**
     * Derselbe Tag wie [now], aber als stabiler Schluessel fuer taggebundene Spielzustande.
     * Im Zeitraffer zaehlt jeder simulierte 24h-Durchlauf als neuer Tag; im Normalbetrieb gilt
     * das echte Kalenderdatum. Ein Wechsel der Zeitraffer-Stufe startet bewusst einen neuen
     * Testtag, genau wie die beschleunigte Uhr dabei wieder am Morgen beginnt.
     */
    fun dayKey(): String {
        val current = speed
        if (current == Speed.OFF) return "real:${LocalDate.now()}"
        val elapsedSeconds = (SystemClock.elapsedRealtime() - startedAtMillis) / 1000.0
        val simulatedDay = (elapsedSeconds / current.daySeconds).toLong()
        return "lapse:${current.name}:$startedAtMillis:$simulatedDay"
    }

""",
)

# 2) Memories belong to one companion and one PlayTimeLapse day.
dream_path = Path("app-sim/src/main/java/com/notime/glyphsim/matrix/PlayDreams.kt")
dream = dream_path.read_text(encoding="utf-8")
dream = dream.replace("import java.time.LocalDate\n", "")
dream = dream.replace(
    "fun remember(context: Context, topic: AnimationType, date: LocalDate = LocalDate.now()) {",
    "fun remember(context: Context, profileId: String, topic: AnimationType, dayKey: String = PlayTimeLapse.dayKey()) {",
)
dream = dream.replace(
    """        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val day = date.toString()
        val storedDay = prefs.getString(KEY_DATE, null)
        val topics = if (storedDay == day) {
            decode(prefs.getString(KEY_TOPICS, null))
""",
    """        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val dateKey = key(profileId, KEY_DATE)
        val topicsKey = key(profileId, KEY_TOPICS)
        val storedDay = prefs.getString(dateKey, null)
        val topics = if (storedDay == dayKey) {
            decode(prefs.getString(topicsKey, null))
""",
)
dream = dream.replace(
    """        prefs.edit()
            .putString(KEY_DATE, day)
            .putString(KEY_TOPICS, next.joinToString(",") { it.name })
            .apply()
    }

    fun today(context: Context, date: LocalDate = LocalDate.now()): List<AnimationType> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_DATE, null) != date.toString()) return emptyList()
        return decode(prefs.getString(KEY_TOPICS, null))
    }

""",
    """        prefs.edit()
            .putString(dateKey, dayKey)
            .putString(topicsKey, next.joinToString(",") { it.name })
            .apply()
    }

    fun today(context: Context, profileId: String, dayKey: String = PlayTimeLapse.dayKey()): List<AnimationType> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val dateKey = key(profileId, KEY_DATE)
        val topicsKey = key(profileId, KEY_TOPICS)
        if (prefs.getString(dateKey, null) != dayKey) return emptyList()
        return decode(prefs.getString(topicsKey, null))
    }

    private fun key(profileId: String, suffix: String): String = "$profileId:$suffix"

""",
)
if "LocalDate" in dream:
    raise SystemExit("PlayDreams still contains wall-clock LocalDate")
dream_path.write_text(dream, encoding="utf-8")

# 3) Record once at the semantic routine boundary, including specialized routines, and suspend
# the ordinary idle loop for the entire sleep gate.
dock = "app-sim/src/main/java/com/notime/glyphsim/ui/DockScreen.kt"
replace_once(
    dock,
    """        suspend fun runRoutine(routine: PlayRoutine, species: AvatarSpecies) {
            val mood = AvatarMoodSnapshot.forSpecies(context, species)
            routineRunning = true
""",
    """        suspend fun runRoutine(routine: PlayRoutine, species: AvatarSpecies) {
            val mood = AvatarMoodSnapshot.forSpecies(context, species)
            routineRunning = true
            // Einmal an der semantischen Grenze merken, nicht nur bei RoutineStep.Act: Football,
            // Training, Music, Painting usw. bestehen aus spezialisierten Schritten und wuerden
            // sonst trotz sichtbarer Handlung nie als Tageserlebnis im Traum landen.
            currentTopic?.let { topic ->
                PlayDreamMemory.remember(context, presenceProfileId.toString(), topic)
            }
""",
)
replace_once(
    dock,
    """                    is RoutineStep.Act -> {
                        activeActivity = step.topic
                        PlayDreamMemory.remember(context, step.topic)
""",
    """                    is RoutineStep.Act -> {
                        activeActivity = step.topic
""",
)
replace_once(
    dock,
    """                    RoutineStep.SleepUntilMorning -> {
                        // Tagsueber bleibt eine ausdrueckliche Schlafhandlung ein kurzes Nickerchen.
""",
    """                    RoutineStep.SleepUntilMorning -> {
                        // `Act(SLEEP)` startet wie jede Act-Reaktion danach wieder die normale
                        // Idle-Schleife. Fuer echten Schlaf waere das falsch: offene Augen,
                        // Schwanzbewegung und Spezies-Regungen liefen sonst die ganze Nacht im Bett.
                        // Deshalb hier die Idle-Schleife stoppen und auf der letzten Schlafpose
                        // einfrieren, bis Rise/Stretch am Morgen wieder normales Idle startet.
                        avatarIdleJob?.cancel()
                        AvatarAnimations.reactionFor(species, AnimationType.SLEEP).frames.lastOrNull()?.let { sleepFrame ->
                            avatar = avatar?.copy(frame = sleepFrame)
                        }
                        // Tagsueber bleibt eine ausdrueckliche Schlafhandlung ein kurzes Nickerchen.
""",
)
replace_once(
    dock,
    "PlayDreams.choose(PlayDreamMemory.today(context))?.let { memory ->",
    "PlayDreams.choose(PlayDreamMemory.today(context, presenceProfileId.toString()))?.let { memory ->",
)

# 4) Canonical evolution history: evidence, persistence/migration, rollback, tests, open decisions.
evo = Path("EVOLUTION.md")
text = evo.read_text(encoding="utf-8")
entry = """

### 2026-09-05 - Schlaf bleibt im Bett; Tageserlebnisse werden zu seltenen Traeumen

- **Version / Evidenzklasse:** Protokoll bleibt 0.5. `BEOBACHTET`: Im bisherigen Nachtablauf
  enthielt `AnimationType.SLEEP` absichtlich Varianten mit `Rise -> LOOK_AROUND -> Occupy(BED)`;
  dadurch stand das Wesen nachts wiederholt auf. `ABGELEITET`: Weil jede Schlafroutine danach
  endete, konnte die normale Ambient-Schleife erneut FIDGET/WANDER/PERFORM waehlen. `ENTSCHIEDEN`:
  Nach dem Hinlegen ist Schlaf nachts ein exklusiver Zustand bis zum Morgen; Traeume sind eine
  visuelle Projektion darueber und bewegen den realen Avatar nicht.
- **Architekturentscheidung:** Kein zweiter Avatar- oder Weltzustand. `PlayRoutine` bekommt nur den
  Gate-Schritt `SleepUntilMorning`; `DockScreen.runRoutine` bleibt waehrend der Nacht aktiv und
  blockiert damit die bereits vorhandene autonome Schleife. Die normale Avatar-Idle-Schleife wird
  am Gate angehalten, damit die Schlafpose nicht von offenen Augen/Fidgets ueberschrieben wird.
  Traumsequenzen verwenden vorhandene `AvatarAnimations.reactionFor`-Frames in `PlayDreamBubble`.
- **Erinnerungsmodell:** `PlayDreamMemory` speichert hoechstens zwoelf semantische
  `AnimationType`-Erlebnisse je Begleiter und simuliertem Tag. Es speichert keine Screenshots,
  Videos oder zweite Weltkopie. Erfasst wird einmal am gemeinsamen `runRoutine`-Eingang, damit
  auch spezialisierte Football-/Training-/Music-/Painting-/Fishing-/Kite-Routinen beruecksichtigt
  werden. `SLEEP` und `MEDICINE` bleiben ausgeschlossen.
- **Zeitsemantik:** Tagesschluessel kommen aus `PlayTimeLapse.dayKey()`. OFF folgt dem echten
  Kalenderdatum; FAST/TURBO zaehlen jeden simulierten 24h-Zyklus getrennt. Damit vermischen
  beschleunigte Testtage ihre Traumerinnerungen nicht.
- **Persistenz / Migration:** Keine Room-Migration. Neu sind SharedPreferences unter
  `play_dream_memory`; Schluessel sind mit der bestehenden `presenceProfileId` namespaced. Es gibt
  keine Alt-Daten aus einem Release zu migrieren. Unscoped Schluessel aus dem unveroeffentlichten
  ersten PR-Entwurf werden nicht mehr gelesen und sind damit harmlos verwaist.
- **Rollback:** `SleepUntilMorning`, `PlayDreamMemory`/`PlayDreams` und `PlayDreamBubble` koennen
  gemeinsam entfernt und die vorherigen SLEEP-Routinen wiederhergestellt werden; keine Room-Daten
  oder externen Formate muessen zurueckmigriert werden. Die Preference-Datei kann beim Rollback
  liegenbleiben, da kein anderer Pfad sie liest.
- **Tests / CI:** `SleepRoutineTest` schuetzt genau eine Schlafroutine sowie "kein Rise/LookAround
  zwischen Bettbelegung und Morgen-Gate". `PlayDreamsTest` schuetzt die Auswahlregeln. Der PR muss
  zusaetzlich den bestehenden `gradlew verify`, API-26-/API-35-Instrumentierung und Release-Gate
  bestehen.
- **Weiter offen:** Die erste Erinnerung ist absichtlich nur grob (`MOVE` statt z. B. konkretem
  Dribbling). Reichere Ereignis-IDs, mehrere verfremdete Ausschnitte pro Traum und das gewuenschte
  Luftballon-Easter-Egg bleiben `OPEN DECISION`/Folgeschnitte. Fuer den Ballon wird kein paralleles
  Item-System erfunden; er wird erst an einen echten Ballon-Node/Gegenstand angeschlossen.
"""
if "### 2026-09-05 - Schlaf bleibt im Bett; Tageserlebnisse werden zu seltenen Traeumen" not in text:
    evo.write_text(text.rstrip() + entry + "\n", encoding="utf-8")
