#!/usr/bin/env bash
# Rendert die Avatar-Reaktionen als Kontaktboegen - ohne Android, ohne Geraet, ohne Emulator.
# Siehe README.md daneben fuer das Warum.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="${WORK:-$HERE/.work}"
KOTLIN_VERSION="${KOTLIN_VERSION:-2.2.20}"
SPECIES="${1:-PUFFLING}"
OUT="${2:-$WORK/sheets}"

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
SRCS=(
  "$CORE/AnimationType.kt" "$CORE/AnimationNode.kt" "$CORE/AnimationTree.kt"
  "$CORE/FrameCrossfade.kt" "$CORE/FrameSprite.kt" "$CORE/ReminderFrameGrid.kt"
  "$SIM/AvatarAnimations.kt" "$SIM/AvatarBody.kt" "$SIM/AvatarGeometry.kt"
  "$SIM/AvatarReactions.kt" "$SIM/AvatarSignatureReactions.kt" "$SIM/AvatarMotifReactions.kt"
  "$SIM/AvatarSpecies.kt" "$SIM/AvatarMood.kt" "$SIM/ReactionTrigger.kt" "$SIM/GloopShape.kt"
)

# Die R-Klassen erzeugt sonst das Android-Gradle-Plugin, und genau das ist hier nicht
# erreichbar. Die Namen werden deshalb aus den Quellen gelesen statt gepflegt - eine
# handgeschriebene Liste liefe beim naechsten neuen String stillschweigend aus dem Tritt.
echo "R-Platzhalter erzeugen ..."
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

echo "Uebersetzen ..."
"$KOTLINC" -nowarn -d "$WORK/out" "$HERE"/src/*.kt "$WORK"/gen/R_*.kt "${SRCS[@]}"

CP="$WORK/out:$WORK/kotlinc/lib/kotlin-stdlib.jar"
mkdir -p "$OUT"
java -cp "$CP" -Djava.awt.headless=true RenderKt "$SPECIES" "$OUT"
echo
java -cp "$CP" -Djava.awt.headless=true DupesKt
echo
echo "Kontaktboegen: $OUT"
