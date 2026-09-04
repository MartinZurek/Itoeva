from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str):
    file = Path(path)
    text = file.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    file.write_text(text.replace(old, new, 1))


play_effects = "app-sim/src/main/java/com/notime/glyphsim/matrix/PlayEffects.kt"
replace_once(
    play_effects,
    "enum class FootballPhase { DRIBBLE, AIM, KICK, TRICK }",
    "enum class FootballPhase { TOUCH, DRIBBLE, AIM, KICK, TRICK }",
    "football TOUCH enum"
)
replace_once(
    play_effects,
    """        val centerX = when (phase) {
            FootballPhase.DRIBBLE -> avatarCellX + 15 + direction * 2
            FootballPhase.AIM -> avatarCellX + 17
""",
    """        val centerX = when (phase) {
            // Einfacher Ballkontakt fuer Anfaenger: nah am Fuss und ohne das seitliche
            // Hin-und-her des echten Dribblings. So ist DRIBBLE tatsaechlich ein freischaltbarer
            // Verhaltensschritt statt nur ein anderer Name fuer die Basisaktion.
            FootballPhase.TOUCH -> avatarCellX + 15
            FootballPhase.DRIBBLE -> avatarCellX + 15 + direction * 2
            FootballPhase.AIM -> avatarCellX + 17
""",
    "football TOUCH x"
)
replace_once(
    play_effects,
    """        val centerY = when (phase) {
            FootballPhase.DRIBBLE, FootballPhase.AIM -> groundY - 1
""",
    """        val centerY = when (phase) {
            FootballPhase.TOUCH, FootballPhase.DRIBBLE, FootballPhase.AIM -> groundY - 1
""",
    "football TOUCH y"
)
replace_once(
    play_effects,
    """        return (goal.render(grounded = false) + ball.render(grounded = false) + trail.render(carve = false))
            .filter { it.x in 0 until widthCells }
            .distinctBy { it.x to it.y }
""",
    """        // Ein Tor gehoert erst zur Schussabsicht. Beim lokalen Ballkontakt oder Dribbling
        // im Park/Wiese wuerde ein ploetzlich eingeblendetes Tor den eben gewonnenen Ortskontext
        // wieder unglaubwuerdig machen.
        val goalCells = if (phase == FootballPhase.AIM || phase == FootballPhase.KICK) {
            goal.render(grounded = false)
        } else {
            emptyList()
        }
        return (goalCells + ball.render(grounded = false) + trail.render(carve = false))
            .filter { it.x in 0 until widthCells }
            .distinctBy { it.x to it.y }
""",
    "contextual football goal"
)

activity = "app-sim/src/main/java/com/notime/glyphsim/skilltree/AvatarActivity.kt"
replace_once(
    activity,
    """     * Dribbling-Sequenz; ohne Schuss-Knoten weder Zielen noch Schuss. Die eine kurze
     * `Football(DRIBBLE)`-Phase am Anfang ist dabei der bereits vorhandene Renderer fuer eine
     * einfache Ballberuehrung. Erst Wiederholung und Ortswechsel bilden das gelernte Dribbling.
""",
    """     * Dribbling-Sequenz; ohne Schuss-Knoten weder Zielen noch Schuss. Die neue, kleine
     * `Football(TOUCH)`-Phase am Anfang erweitert dabei nur den vorhandenen Fussball-Renderer um
     * einfachen Ballkontakt; `DRIBBLE` bleibt dadurch ausschliesslich dem freigeschalteten Skill.
""",
    "activity gating comment"
)
replace_once(
    activity,
    "add(RoutineStep.Football(PlayEffects.FootballPhase.DRIBBLE))\n            add(RoutineStep.Linger(if (ballsportLearned) 4_000L else 2_500L))",
    "add(RoutineStep.Football(PlayEffects.FootballPhase.TOUCH))\n            add(RoutineStep.Linger(if (ballsportLearned) 4_000L else 2_500L))",
    "beginner TOUCH phase"
)

tests = "app-sim/src/test/java/com/notime/glyphsim/skilltree/AvatarActivityPlansTest.kt"
replace_once(
    tests,
    "assertEquals(listOf(PlayEffects.FootballPhase.DRIBBLE), footballPhases(resolved))",
    "assertEquals(listOf(PlayEffects.FootballPhase.TOUCH), footballPhases(resolved))",
    "park beginner TOUCH test"
)
replace_once(
    tests,
    "assertEquals(1, footballPhases(beginner).count { it == PlayEffects.FootballPhase.DRIBBLE })\n        assertEquals(3, footballPhases(learned).count { it == PlayEffects.FootballPhase.DRIBBLE })",
    "assertEquals(0, footballPhases(beginner).count { it == PlayEffects.FootballPhase.DRIBBLE })\n        assertEquals(2, footballPhases(learned).count { it == PlayEffects.FootballPhase.DRIBBLE })",
    "dribbling strict gating test"
)
replace_once(
    tests,
    "assertEquals(2, footballPhases(levelOne).count { it == PlayEffects.FootballPhase.DRIBBLE })\n        assertEquals(3, footballPhases(levelThree).count { it == PlayEffects.FootballPhase.DRIBBLE })",
    "assertEquals(1, footballPhases(levelOne).count { it == PlayEffects.FootballPhase.DRIBBLE })\n        assertEquals(2, footballPhases(levelThree).count { it == PlayEffects.FootballPhase.DRIBBLE })",
    "level dribbling counts"
)
