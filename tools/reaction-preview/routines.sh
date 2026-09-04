#!/usr/bin/env bash
# Gibt aus, welche PlayRoutine ein kontextueller Skill-Intent tatsaechlich erzeugt -
# je Ort und je Freischaltungsstand. Ohne Android, ohne Geraet, ohne Emulator.
#
# Damit laesst sich ein neuer vertikaler Schnitt (AvatarActivityPlans.resolve) pruefen,
# BEVOR er auf einem Geraet landet: Man sieht die Schrittfolge, statt sie zu erraten.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="${WORK:-$HERE/.work}"
KOTLIN_VERSION="${KOTLIN_VERSION:-2.2.20}"

KOTLINC="$WORK/kotlinc/bin/kotlinc"
if [ ! -x "$KOTLINC" ]; then
  echo "Kotlin-Compiler wird geholt (einmalig, ~80 MB) ..."
  mkdir -p "$WORK"
  curl -sSL -o "$WORK/kotlin-compiler.zip" \
    "https://github.com/JetBrains/kotlin/releases/download/v$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.zip"
  ( cd "$WORK" && unzip -q -o kotlin-compiler.zip )
fi

CORE="$ROOT/core/src/main/java/com/notime/glyphcore/data"
SIM="$ROOT/app-sim/src/main/java/com/notime/glyphsim/matrix"
SK="$ROOT/app-sim/src/main/java/com/notime/glyphsim/skilltree"

# R-Platzhalter wie bei render.sh: gelesen statt gepflegt.
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

SRCS=(
  "$CORE/AnimationType.kt" "$CORE/AnimationNode.kt" "$CORE/AnimationTree.kt"
  "$CORE/FrameCrossfade.kt" "$CORE/FrameSprite.kt" "$CORE/ReminderFrameGrid.kt"
  "$SIM/AvatarAnimations.kt" "$SIM/AvatarBody.kt" "$SIM/AvatarGeometry.kt"
  "$SIM/AvatarReactions.kt" "$SIM/AvatarSignatureReactions.kt" "$SIM/AvatarMotifReactions.kt"
  "$SIM/AvatarSpecies.kt" "$SIM/AvatarMood.kt" "$SIM/ReactionTrigger.kt" "$SIM/GloopShape.kt"
  "$SIM/PlayScene.kt" "$SIM/PlayEffects.kt" "$SIM/PlayRoutine.kt" "$SIM/PlayInk.kt"
  "$SIM/PlayAmbientActivity.kt" "$SIM/PlayTimeLapse.kt" "$SIM/PlayWeather.kt"
  "$SK/AvatarActivity.kt" "$SK/UnlockOffer.kt" "$SK/SkillTreeRows.kt" "$SK/SkillRepertoire.kt"
)

echo "Uebersetzen ..."
"$KOTLINC" -nowarn -d "$WORK/routines" \
  "$HERE/src/Annotations.kt" "$HERE/src/AndroidStubs.kt" "$HERE/src/AndroidOsStubs.kt" \
  "$HERE/src/RoutineDump.kt" "$WORK"/gen/R_*.kt "${SRCS[@]}"

java -cp "$WORK/routines:$WORK/kotlinc/lib/kotlin-stdlib.jar" SliceKt
