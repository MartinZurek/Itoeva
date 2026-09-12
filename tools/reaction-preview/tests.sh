#!/usr/bin/env bash
# Fuehrt die reinen Kotlin-Unit-Tests wirklich AUS - ohne Android, ohne Gradle, ohne Geraet.
#
# Warum das hier steht: Die Werkzeuge daneben konnten den Code bisher nur uebersetzen und
# seine Ausgabe zeigen. Ob die vorhandenen Tests dazu gruen sind, liess sich lokal gar nicht
# beantworten - die Antwort kam erst Minuten spaeter aus der CI. Damit war jeder Push eine
# Wette. Ein Lauf hier dauert unter einer Sekunde.
#
# Was hier NICHT laufen kann: alles, was Android, Room, Compose oder einen Emulator braucht
# (`app-sim/src/androidTest/`, Datenbank-Migrationen, UI). Das bleibt Sache der CI. Die Liste
# TEST_CLASSES unten ist deshalb ausdruecklich und nicht geraten.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="${WORK:-$HERE/.work}"
KOTLIN_VERSION="${KOTLIN_VERSION:-2.2.20}"
JUNIT_VERSION="${JUNIT_VERSION:-4.13.2}"
HAMCREST_VERSION="${HAMCREST_VERSION:-1.3}"

KOTLINC="$WORK/kotlinc/bin/kotlinc"
if [ ! -x "$KOTLINC" ]; then
  echo "Kotlin-Compiler wird geholt (einmalig, ~80 MB) ..."
  mkdir -p "$WORK"
  curl -sSL -o "$WORK/kotlin-compiler.zip" \
    "https://github.com/JetBrains/kotlin/releases/download/v$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.zip"
  ( cd "$WORK" && unzip -q -o kotlin-compiler.zip )
fi

# JUnit 4 und Hamcrest aus Maven Central - dieselbe Fassung, die `:app-sim` benutzt.
JUNIT="$WORK/junit-$JUNIT_VERSION.jar"
HAMCREST="$WORK/hamcrest-core-$HAMCREST_VERSION.jar"
[ -f "$JUNIT" ] || curl -sSL -o "$JUNIT" \
  "https://repo1.maven.org/maven2/junit/junit/$JUNIT_VERSION/junit-$JUNIT_VERSION.jar"
[ -f "$HAMCREST" ] || curl -sSL -o "$HAMCREST" \
  "https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/$HAMCREST_VERSION/hamcrest-core-$HAMCREST_VERSION.jar"

CORE="$ROOT/core/src/main/java/com/notime/glyphcore/data"
SIM="$ROOT/app-sim/src/main/java/com/notime/glyphsim/matrix"
SK="$ROOT/app-sim/src/main/java/com/notime/glyphsim/skilltree"
LIV="$ROOT/app-sim/src/main/java/com/notime/glyphsim/living"
STREAM="$ROOT/app-sim/src/main/java/com/notime/glyphsim/stream"
TEST="$ROOT/app-sim/src/test/java/com/notime/glyphsim"

# R-Platzhalter wie bei den Nachbarskripten: gelesen statt gepflegt.
mkdir -p "$WORK/gen"
grep -rho "R\.string\.[a-zA-Z_0-9]*" "$CORE"/*.kt "$SIM"/*.kt \
  | sed 's/.*R\.string\.//' | sort -u > "$WORK/gen/names.txt"
for pkg in com.notime.glyphcore com.notime.glyphsim; do
  {
    echo "package $pkg"
    echo "object R { object string {"
    awk '{ print "  const val " $0 " = " NR }' "$WORK/gen/names.txt"
    echo "} }"
  } > "$WORK/gen/R_${pkg##*.}.kt"
done

# Ausdrueckliche Listen statt Platzhalter - aus demselben Grund wie in render.sh: Ein `*.kt`
# zoege Dateien mit herein, deren Abhaengigkeiten hier gar nicht uebersetzt werden, und der
# Lauf waere kaputt, ohne dass jemand etwas an ihm geaendert haette. Die Listen der drei
# Skripte unterscheiden sich absichtlich; render.sh braucht die Welt-Dateien nicht.
SRCS=(
  "$CORE/AnimationType.kt" "$CORE/AnimationNode.kt" "$CORE/AnimationTree.kt"
  "$CORE/LibraryAnimation.kt" "$CORE/DefaultLibraryAnimations.kt" "$CORE/FrameCodec.kt"
  "$CORE/AvatarSignatureAnimations.kt" "$CORE/SkillTreeAnimations.kt"
  "$CORE/FrameCrossfade.kt" "$CORE/FrameSprite.kt" "$CORE/ReminderFrameGrid.kt"
  "$SIM/AvatarShading.kt" "$SIM/AvatarPalette.kt" "$SIM/MatrixColors.kt" "$SIM/AvatarAnimations.kt" "$SIM/AvatarBody.kt" "$SIM/AvatarGeometry.kt"
  "$SIM/AvatarReactions.kt" "$SIM/AvatarSignatureReactions.kt" "$SIM/AvatarMotifReactions.kt"
  "$SIM/AvatarSpecies.kt" "$SIM/AvatarMood.kt" "$SIM/ReactionTrigger.kt" "$SIM/GloopShape.kt"
  "$SIM/PlayScene.kt" "$SIM/PlayEffects.kt" "$SIM/PlayRoutine.kt" "$SIM/PlayInk.kt"
  "$SIM/LivingRuntimeAdapter.kt"
  "$SIM/PlayAmbientActivity.kt" "$SIM/PlayTimeLapse.kt" "$SIM/PlayWeather.kt"
  "$SIM/PlayMusicPlan.kt" "$SIM/PlayMusicRotation.kt" "$SIM/PlayCharacterTheme.kt" "$SIM/PlayOutdoorStay.kt" "$SIM/PlayAfterglow.kt" "$SIM/PlayVisitWindow.kt" "$SIM/PlayDreams.kt"
  "$SK/AvatarActivity.kt" "$SK/UnlockOffer.kt" "$SK/SkillTreeRows.kt"
  "$SK/SkillRepertoire.kt" "$SK/LevelUnlocks.kt"
  # Der reine Living-Agent-Kern (NT-063/NT-067) - kein Android, keine Uhr, kein Zufall.
  # Genau deshalb laeuft seine Verhaltensstrecke hier und nicht erst in der CI.
  "$LIV/LivingWorld.kt" "$LIV/LivingNeed.kt" "$LIV/LivingAction.kt"
  "$LIV/LivingPlanner.kt" "$LIV/LivingAgent.kt" "$LIV/LivingSymbols.kt"
  "$ROOT/app-sim/src/main/java/com/notime/glyphsim/ui/ActionSlotState.kt"
  # Die Symbole ueber dem Kopf (NT-069): Bedeutung im living-Paket, Motiv hier. Beide
  # sind reines Kotlin - ActionSlotSymbols schlaegt nur in ReminderAnimations nach.
  "$SIM/ReminderAnimations.kt" "$SIM/MatrixGeometry.kt"
  "$ROOT/app-sim/src/main/java/com/notime/glyphsim/ui/ActionSlotSymbols.kt"
  "$ROOT/app-sim/src/main/java/com/notime/glyphsim/ui/LivingSymbolFrames.kt"
  "$STREAM/LivingObservationSource.kt" "$STREAM/StreamInteraction.kt"
  # Die Zuschauer-Eingangsschicht (NT-070). Befehlssyntax, Abstandspruefung und das
  # Twitch-Protokoll sind bewusst reines Kotlin - deshalb laufen sie hier und brauchen weder
  # Netz noch Twitch-Konto. Die beiden Provider fehlen absichtlich: Sie haengen an
  # kotlinx-coroutines, das dieser Harness nicht auf dem Klassenpfad hat, und werden in der CI
  # uebersetzt.
  "$STREAM/StreamCommands.kt" "$STREAM/StreamCommandGate.kt" "$STREAM/TwitchIrc.kt"
  # Android sitzt nur hinter LivingAgentStorage; Codec und Store bekommen die Zeit explizit.
  "$ROOT/app-sim/src/main/java/com/notime/glyphsim/data/LivingAgentStore.kt"
  # Die Musik-Wiedergabeschicht: reines Kotlin bis auf MediaPlayer/AudioManager, fuer die
  # MediaStubs.kt und AnimatorStubs.kt daneben einspringen. Geprueft werden Entscheidung und
  # Lautstaerke-Kurve, nicht Androids tatsaechliche Audioausgabe.
  "$ROOT/app-sim/src/main/java/com/notime/glyphsim/settings/SettingsCatalog.kt"
  "$ROOT/app-sim/src/main/java/com/notime/glyphsim/ui/PlayMusic.kt"
  # XP nach Wirkung (NT-072) - reine Zahlenarbeit, kein Android.
  "$ROOT/app-sim/src/main/java/com/notime/glyphsim/ui/PlayModeXp.kt"
)

TEST_SRCS=(
  "$TEST/skilltree/AvatarActivityPlansTest.kt"
  "$TEST/skilltree/UnlockOfferTest.kt"
  "$TEST/skilltree/SkillTreeRowsTest.kt"
  "$TEST/skilltree/SkillRepertoireTest.kt"
  "$TEST/skilltree/LevelUnlocksTest.kt"
  "$TEST/matrix/AvatarShadingTest.kt"
  "$TEST/matrix/AvatarPaletteTest.kt"
  "$TEST/matrix/FacadeTest.kt"
  "$TEST/matrix/ReactionDistinctnessTest.kt"
  "$TEST/matrix/ReactionTriggerTest.kt"
  "$TEST/matrix/AvatarReactionsTest.kt"
  "$TEST/matrix/MusicResolverTest.kt"
  "$TEST/matrix/PlayMusicRotationTest.kt"
  "$TEST/matrix/PlayCharacterThemeTest.kt"
  "$TEST/matrix/ScenePreview.kt"
  "$TEST/matrix/PlayRoutineTest.kt"
  "$TEST/matrix/LivingRuntimeAdapterTest.kt"
  "$TEST/matrix/ReminderActionsTest.kt"
  "$TEST/matrix/OutdoorsAndDreamsTest.kt"
  "$TEST/matrix/PlayVisitWindowTest.kt"
  "$TEST/matrix/SleepRoutineTest.kt"
  "$TEST/matrix/PlayDreamsTest.kt"
  "$TEST/matrix/PlayAmbientActivityTest.kt"
  "$TEST/matrix/PlayVarietyGuardTest.kt"
  "$TEST/matrix/PlayOutdoorStayTest.kt"
  "$TEST/matrix/PlayAfterglowTest.kt"
  "$TEST/matrix/ReactionDwellTest.kt"
  "$TEST/matrix/PlayMotifLegibilityTest.kt"
  "$TEST/matrix/ReactionFingerprintTest.kt"
  "$TEST/ui/PlayMusicTest.kt"
  "$TEST/living/LivingAgentTest.kt"
  "$TEST/living/LivingSymbolsTest.kt"
  "$TEST/living/LivingDriveTest.kt"
  "$TEST/ui/LivingSymbolFramesTest.kt"
  "$TEST/stream/LivingObservationSourceTest.kt"
  "$TEST/stream/StreamInteractionTest.kt"
  "$TEST/stream/StreamCommandParserTest.kt"
  "$TEST/stream/StreamCommandGateTest.kt"
  "$TEST/stream/TwitchIrcTest.kt"
  "$TEST/stream/StreamViewerChainTest.kt"
  "$TEST/stream/StreamBoundaryTest.kt"
  "$TEST/stream/StreamRunbookTest.kt"
  "$TEST/data/LivingAgentStoreTest.kt"
  "$TEST/data/LivingMemoryContinuityTest.kt"
  "$TEST/settings/SettingsCatalogTest.kt"
)

TEST_CLASSES=(
  com.notime.glyphsim.skilltree.AvatarActivityPlansTest
  com.notime.glyphsim.skilltree.UnlockOfferTest
  com.notime.glyphsim.skilltree.SkillTreeRowsTest
  com.notime.glyphsim.skilltree.SkillRepertoireTest
  com.notime.glyphsim.skilltree.LevelUnlocksTest
  com.notime.glyphsim.matrix.AvatarShadingTest
  com.notime.glyphsim.matrix.AvatarPaletteTest
  com.notime.glyphsim.matrix.FacadeTest
  com.notime.glyphsim.matrix.ReactionDistinctnessTest
  com.notime.glyphsim.matrix.ReactionTriggerTest
  com.notime.glyphsim.matrix.AvatarReactionsTest
  com.notime.glyphsim.matrix.MusicResolverTest
  com.notime.glyphsim.matrix.PlayMusicRotationTest
  com.notime.glyphsim.matrix.PlayCharacterThemeTest
  com.notime.glyphsim.matrix.PlayRoutineTest
  com.notime.glyphsim.matrix.LivingRuntimeAdapterTest
  com.notime.glyphsim.matrix.ReminderActionsTest
  com.notime.glyphsim.matrix.OutdoorsAndDreamsTest
  com.notime.glyphsim.matrix.PlayVisitWindowTest
  com.notime.glyphsim.matrix.SleepRoutineTest
  com.notime.glyphsim.matrix.PlayDreamsTest
  com.notime.glyphsim.matrix.PlayAmbientActivityTest
  com.notime.glyphsim.matrix.PlayVarietyGuardTest
  com.notime.glyphsim.matrix.PlayOutdoorStayTest
  com.notime.glyphsim.matrix.PlayAfterglowTest
  com.notime.glyphsim.matrix.ReactionDwellTest
  com.notime.glyphsim.matrix.PlayMotifLegibilityTest
  com.notime.glyphsim.matrix.ReactionFingerprintTest
  com.notime.glyphsim.ui.PlayMusicTest
  com.notime.glyphsim.living.LivingAgentTest
  com.notime.glyphsim.living.LivingSymbolsTest
  com.notime.glyphsim.living.LivingDriveTest
  com.notime.glyphsim.ui.LivingSymbolFramesTest
  com.notime.glyphsim.stream.LivingObservationSourceTest
  com.notime.glyphsim.stream.StreamInteractionTest
  com.notime.glyphsim.stream.StreamCommandParserTest
  com.notime.glyphsim.stream.StreamCommandGateTest
  com.notime.glyphsim.stream.TwitchIrcTest
  com.notime.glyphsim.stream.StreamViewerChainTest
  com.notime.glyphsim.stream.StreamBoundaryTest
  com.notime.glyphsim.stream.StreamRunbookTest
  com.notime.glyphsim.data.LivingAgentStoreTest
  com.notime.glyphsim.data.LivingMemoryContinuityTest
  com.notime.glyphsim.settings.SettingsCatalogTest
)

echo "Uebersetzen ..."
"$KOTLINC" -nowarn -d "$WORK/tests" -cp "$JUNIT:$HAMCREST" \
  "$HERE/src/Annotations.kt" "$HERE/src/RoomStubs.kt" "$HERE/src/AndroidStubs.kt" "$HERE/src/AndroidOsStubs.kt" \
  "$HERE/src/MediaStubs.kt" "$HERE/src/AnimatorStubs.kt" \
  "$HERE/src/SettingsStoreStub.kt" "$HERE/src/LogStub.kt" \
  "$WORK"/gen/R_*.kt "${SRCS[@]}" "${TEST_SRCS[@]}"

echo "Laufen lassen ..."
# Aus :app-sim heraus, weil ReactionFingerprintTest seine Golden-Datei unter
# src/test/reaction-fingerprint.txt sucht - relativ zum Modul, wie Gradle es aufruft. Alle
# Klassenpfade oben sind absolut und bleiben davon unberuehrt.
cd "$ROOT/app-sim"
java -cp "$WORK/tests:$WORK/kotlinc/lib/kotlin-stdlib.jar:$JUNIT:$HAMCREST" \
  org.junit.runner.JUnitCore "${TEST_CLASSES[@]}"
