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
  "$CORE/FrameCrossfade.kt" "$CORE/FrameSprite.kt" "$CORE/ReminderFrameGrid.kt"
  "$SIM/AvatarAnimations.kt" "$SIM/AvatarBody.kt" "$SIM/AvatarGeometry.kt"
  "$SIM/AvatarReactions.kt" "$SIM/AvatarSignatureReactions.kt" "$SIM/AvatarMotifReactions.kt"
  "$SIM/AvatarSpecies.kt" "$SIM/AvatarMood.kt" "$SIM/ReactionTrigger.kt" "$SIM/GloopShape.kt"
  "$SIM/PlayScene.kt" "$SIM/PlayEffects.kt" "$SIM/PlayRoutine.kt" "$SIM/PlayInk.kt"
  "$SIM/PlayAmbientActivity.kt" "$SIM/PlayTimeLapse.kt" "$SIM/PlayWeather.kt"
  "$SIM/PlayMusicPlan.kt"
  "$SK/AvatarActivity.kt" "$SK/UnlockOffer.kt" "$SK/SkillTreeRows.kt"
  "$SK/SkillRepertoire.kt" "$SK/LevelUnlocks.kt"
  # Die Musik-Wiedergabeschicht: reines Kotlin bis auf MediaPlayer/AudioManager, fuer die
  # src/MediaStubs.kt daneben einspringt. Geprueft wird PlayMusic.decide, nicht die Ausgabe.
  "$ROOT/app-sim/src/main/java/com/notime/glyphsim/settings/SettingsCatalog.kt"
  "$ROOT/app-sim/src/main/java/com/notime/glyphsim/ui/PlayMusic.kt"
)

TEST_SRCS=(
  "$TEST/skilltree/AvatarActivityPlansTest.kt"
  "$TEST/skilltree/UnlockOfferTest.kt"
  "$TEST/skilltree/SkillTreeRowsTest.kt"
  "$TEST/skilltree/SkillRepertoireTest.kt"
  "$TEST/skilltree/LevelUnlocksTest.kt"
  "$TEST/matrix/ReactionDistinctnessTest.kt"
  "$TEST/matrix/ReactionTriggerTest.kt"
  "$TEST/matrix/AvatarReactionsTest.kt"
  "$TEST/matrix/MusicResolverTest.kt"
  "$TEST/ui/PlayMusicTest.kt"
  "$TEST/settings/SettingsCatalogTest.kt"
)

TEST_CLASSES=(
  com.notime.glyphsim.skilltree.AvatarActivityPlansTest
  com.notime.glyphsim.skilltree.UnlockOfferTest
  com.notime.glyphsim.skilltree.SkillTreeRowsTest
  com.notime.glyphsim.skilltree.SkillRepertoireTest
  com.notime.glyphsim.skilltree.LevelUnlocksTest
  com.notime.glyphsim.matrix.ReactionDistinctnessTest
  com.notime.glyphsim.matrix.ReactionTriggerTest
  com.notime.glyphsim.matrix.AvatarReactionsTest
  com.notime.glyphsim.matrix.MusicResolverTest
  com.notime.glyphsim.ui.PlayMusicTest
  com.notime.glyphsim.settings.SettingsCatalogTest
)

echo "Uebersetzen ..."
"$KOTLINC" -nowarn -d "$WORK/tests" -cp "$JUNIT:$HAMCREST" \
  "$HERE/src/Annotations.kt" "$HERE/src/AndroidStubs.kt" "$HERE/src/AndroidOsStubs.kt" \
  "$HERE/src/MediaStubs.kt" "$HERE/src/SettingsStoreStub.kt" "$HERE/src/LogStub.kt" \
  "$WORK"/gen/R_*.kt "${SRCS[@]}" "${TEST_SRCS[@]}"

echo "Laufen lassen ..."
java -cp "$WORK/tests:$WORK/kotlinc/lib/kotlin-stdlib.jar:$JUNIT:$HAMCREST" \
  org.junit.runner.JUnitCore "${TEST_CLASSES[@]}"
