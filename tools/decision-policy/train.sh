#!/usr/bin/env bash
# Trainiert die Decision Policy reproduzierbar von Grund auf - Datensatz, Netz, ONNX, Pruefung.
#
#   bash tools/decision-policy/train.sh            # alles
#   bash tools/decision-policy/train.sh compare    # nur Baseline gegen Modell vergleichen
#   bash tools/decision-policy/train.sh bench      # nur Laufzeit messen
#
# Braucht keinen Android-SDK und kein Gradle: Die Kotlin-Seite laeuft auf den Klassen der
# Offline-Tests (tools/reaction-preview/tests.sh), die Python-Seite nur mit numpy, onnx und
# onnxruntime (siehe requirements.txt). PyTorch ist nicht noetig - das Netz ist klein genug fuer
# eine handgeschriebene Rueckwaertsrechnung, und das macht den Lauf bitgenau wiederholbar.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="$HERE/.work"
RP="$ROOT/tools/reaction-preview/.work"
MODEL="$ROOT/app-sim/src/main/assets/decision_policy_v1.onnx"
mkdir -p "$WORK"

echo "Kotlin-Klassen uebersetzen ..."
COMPILE_ONLY=1 bash "$ROOT/tools/reaction-preview/tests.sh" >/dev/null
"$RP/kotlinc/bin/kotlinc" -nowarn -d "$WORK/tool" -cp "$RP/tests" "$HERE/src/DecisionPolicyTool.kt"
run_tool() {
  java -Xmx2g -cp "$WORK/tool:$RP/tests:$RP/kotlinc/lib/kotlin-stdlib.jar" \
    com.notime.glyphsim.decision.tool.DecisionPolicyToolKt "$@"
}

case "${1:-all}" in
  compare)
    run_tool compare "$MODEL" "$HERE/baseline-vs-policy.md" "${2:-14}"
    exit 0 ;;
  bench)
    run_tool bench "$MODEL"
    exit 0 ;;
esac

echo "Datensatz erzeugen (fester Startwert) ..."
run_tool dataset "$WORK/dataset" 24 3
echo "Netz trainieren und als ONNX schreiben ..."
python3 "$HERE/train_policy.py" --data "$WORK/dataset" --out "$MODEL" \
  --reference "$ROOT/app-sim/src/test/decision-policy-reference.csv" \
  --report "$HERE/training-report.json"
echo "Baseline gegen Modell vergleichen ..."
run_tool compare "$MODEL" "$HERE/baseline-vs-policy.md" 14
echo "Laufzeit messen ..."
run_tool bench "$MODEL" | tee "$HERE/benchmark.txt"
