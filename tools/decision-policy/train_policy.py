#!/usr/bin/env python3
"""Trainiert die Decision Policy (Phase B) und schreibt sie als ONNX.

Eingabe ist der Datensatz aus `DecisionPolicyTool dataset` (siehe train.sh): je Entscheidung alle
zulaessigen Kandidaten mit Merkmalsvektor und Lehrerwert. Gelernt wird **listenweise**: Das Netz
soll je Entscheidung dieselbe Softmax-Verteilung ergeben wie der Lehrer - genau so waehlt spaeter
die App. Ein kleiner Zusatzterm haelt auch die Abstaende der Bewertungen (zentriert) nahe am
Lehrer.

Bewusst ohne PyTorch: Drei vollverbundene Schichten brauchen keine Autograd-Bibliothek, und eine
handgeschriebene Rueckwaertsrechnung in numpy ist mit festem Startwert und einem Rechenfaden
bitgenau wiederholbar. Exportiert wird ueber das offizielle `onnx`-Paket; onnxruntime prueft
danach, dass die Datei dasselbe rechnet wie das trainierte Netz.
"""
import os

# Ein Rechenfaden: Mehrfaedige BLAS-Summen koennen in der letzten Stelle schwanken, und dann waere
# das Modell nicht mehr byte-gleich reproduzierbar.
os.environ.setdefault("OPENBLAS_NUM_THREADS", "1")
os.environ.setdefault("OMP_NUM_THREADS", "1")
os.environ.setdefault("MKL_NUM_THREADS", "1")

import argparse
import hashlib
import json

import numpy as np

MODEL_VERSION = 1
SEED = 20260926


def load(data_dir):
    meta = json.load(open(os.path.join(data_dir, "meta.json")))
    names = meta["names"]
    x = np.fromfile(os.path.join(data_dir, "features.f32"), dtype="<f4").reshape(-1, len(names))
    t = np.fromfile(os.path.join(data_dir, "teacher.f32"), dtype="<f4")
    g = np.fromfile(os.path.join(data_dir, "groups.i32"), dtype="<i4")
    species = np.fromfile(os.path.join(data_dir, "species.i32"), dtype="<i4")
    assert x.shape[0] == t.shape[0] == g.shape[0] == meta["rows"]
    assert np.all(np.diff(g) >= 0), "rows must be grouped by decision"
    return meta, names, x.astype(np.float64), t.astype(np.float64), g, species


def group_offsets(g):
    starts = np.flatnonzero(np.r_[True, g[1:] != g[:-1]])
    ends = np.r_[starts[1:], len(g)]
    return starts, ends


def seg_softmax(v, seg_starts, seg_index):
    mx = np.maximum.reduceat(v, seg_starts)
    e = np.exp(v - mx[seg_index])
    s = np.add.reduceat(e, seg_starts)
    return e / s[seg_index]


class Mlp:
    def __init__(self, sizes, rng):
        self.w = []
        self.b = []
        for i in range(len(sizes) - 1):
            fan_in = sizes[i]
            self.w.append(rng.normal(0.0, np.sqrt(2.0 / fan_in), size=(sizes[i], sizes[i + 1])))
            self.b.append(np.zeros(sizes[i + 1]))

    @property
    def params(self):
        return self.w + self.b

    def parameter_count(self):
        return int(sum(p.size for p in self.params))

    def forward(self, x):
        acts = [x]
        h = x
        for i, (w, b) in enumerate(zip(self.w, self.b)):
            z = h @ w + b
            h = np.maximum(z, 0.0) if i < len(self.w) - 1 else z
            acts.append(h)
        return h[:, 0], acts

    def backward(self, acts, grad_out):
        grads_w = [None] * len(self.w)
        grads_b = [None] * len(self.b)
        d = grad_out[:, None]
        for i in reversed(range(len(self.w))):
            grads_w[i] = acts[i].T @ d
            grads_b[i] = d.sum(axis=0)
            if i > 0:
                d = (d @ self.w[i].T) * (acts[i] > 0.0)
        return grads_w + grads_b


def batch_loss_grad(model, x, t, seg_starts, seg_index, n_groups, mse_weight):
    s, acts = model.forward(x)
    p_t = seg_softmax(t, seg_starts, seg_index)
    log_p_s = s - (np.log(np.add.reduceat(np.exp(s - np.maximum.reduceat(s, seg_starts)[seg_index]), seg_starts))
                   + np.maximum.reduceat(s, seg_starts))[seg_index]
    p_s = np.exp(log_p_s)
    ce = -np.sum(p_t * log_p_s) / n_groups
    counts = np.diff(np.r_[seg_starts, len(s)])
    mean_s = np.add.reduceat(s, seg_starts) / counts
    mean_t = np.add.reduceat(t, seg_starts) / counts
    e = (s - mean_s[seg_index]) - (t - mean_t[seg_index])
    mse = np.sum(e * e / counts[seg_index]) / n_groups
    grad = (p_s - p_t) / n_groups + mse_weight * 2.0 * e / counts[seg_index] / n_groups
    return ce + mse_weight * mse, model.backward(acts, grad)


def evaluate(model, x, t, g, temperature=1.0):
    starts, ends = group_offsets(g)
    seg_index = np.repeat(np.arange(len(starts)), ends - starts)
    s, _ = model.forward(x)
    p_t = seg_softmax(t, starts, seg_index)
    p_s = seg_softmax(s / temperature, starts, seg_index)
    kl = np.add.reduceat(p_t * (np.log(p_t + 1e-12) - np.log(p_s + 1e-12)), starts)
    top_t = np.array([np.argmax(t[a:b]) for a, b in zip(starts, ends)])
    top_s = np.array([np.argmax(s[a:b]) for a, b in zip(starts, ends)])
    counts = ends - starts
    mean_s = np.add.reduceat(s, starts) / counts
    mean_t = np.add.reduceat(t, starts) / counts
    cs = s - mean_s[seg_index]
    ct = t - mean_t[seg_index]
    r2 = 1.0 - np.sum((cs - ct) ** 2) / np.sum(ct ** 2)
    tv = 0.5 * np.add.reduceat(np.abs(p_t - p_s), starts)
    return {
        "decisions": int(len(starts)),
        "mean_kl": float(kl.mean()),
        "top1_agreement": float(np.mean(top_t == top_s)),
        "centered_r2": float(r2),
        "mean_total_variation": float(tv.mean()),
    }


def train(x, t, g, train_groups, hidden, epochs, rng, lr=3e-3, batch_groups=128, mse_weight=0.05):
    starts, ends = group_offsets(g)
    model = Mlp([x.shape[1]] + list(hidden) + [1], rng)
    m = [np.zeros_like(p) for p in model.params]
    v = [np.zeros_like(p) for p in model.params]
    step = 0
    b1, b2, eps = 0.9, 0.999, 1e-8
    for epoch in range(epochs):
        order = rng.permutation(train_groups)
        rate = lr * (0.5 * (1 + np.cos(np.pi * epoch / epochs)))
        for k in range(0, len(order), batch_groups):
            chosen = np.sort(order[k:k + batch_groups])
            rows = np.concatenate([np.arange(starts[c], ends[c]) for c in chosen])
            sizes = ends[chosen] - starts[chosen]
            seg_starts = np.r_[0, np.cumsum(sizes)[:-1]]
            seg_index = np.repeat(np.arange(len(chosen)), sizes)
            _, grads = batch_loss_grad(model, x[rows], t[rows], seg_starts, seg_index, len(chosen), mse_weight)
            step += 1
            for i, (p, gr) in enumerate(zip(model.params, grads)):
                m[i] = b1 * m[i] + (1 - b1) * gr
                v[i] = b2 * v[i] + (1 - b2) * gr * gr
                mh = m[i] / (1 - b1 ** step)
                vh = v[i] / (1 - b2 ** step)
                p -= rate * mh / (np.sqrt(vh) + eps)
    return model


def export_onnx(model, names, path, temperature, extra_meta):
    import onnx
    from onnx import TensorProto, helper, numpy_helper

    inits = []
    nodes = []
    current = "features"
    layers = len(model.w)
    for i, (w, b) in enumerate(zip(model.w, model.b)):
        wn, bn = f"dense{i}_weight", f"dense{i}_bias"
        # Gemm mit transB=1: Gewicht als [aus, ein], wie es jede ONNX-Laufzeit erwartet.
        inits.append(numpy_helper.from_array(w.T.astype(np.float32), wn))
        inits.append(numpy_helper.from_array(b.astype(np.float32), bn))
        out = "score" if i == layers - 1 else f"dense{i}"
        nodes.append(helper.make_node("Gemm", [current, wn, bn], [out], name=f"dense{i}", transB=1))
        if i < layers - 1:
            nodes.append(helper.make_node("Relu", [out], [f"relu{i}"], name=f"relu{i}"))
            current = f"relu{i}"
    graph = helper.make_graph(
        nodes,
        "itoeva_decision_policy",
        [helper.make_tensor_value_info("features", TensorProto.FLOAT, ["candidates", len(names)])],
        [helper.make_tensor_value_info("score", TensorProto.FLOAT, ["candidates", 1])],
        initializer=inits,
    )
    onnx_model = helper.make_model(
        graph,
        producer_name="itoeva-decision-policy",
        producer_version=str(MODEL_VERSION),
        opset_imports=[helper.make_opsetid("", 13)],
    )
    onnx_model.ir_version = 8
    meta = {
        "policy_model_version": str(MODEL_VERSION),
        "feature_schema_version": str(extra_meta["feature_schema_version"]),
        "feature_names": ",".join(names),
        "temperature": repr(float(temperature)),
        "architecture": extra_meta["architecture"],
        "training_seed": str(SEED),
        "dataset": extra_meta["dataset"],
    }
    for k, val in meta.items():
        entry = onnx_model.metadata_props.add()
        entry.key = k
        entry.value = val
    onnx.checker.check_model(onnx_model)
    data = onnx_model.SerializeToString()
    with open(path, "wb") as f:
        f.write(data)
    return data


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--reference", required=True)
    ap.add_argument("--report", required=True)
    ap.add_argument("--epochs", type=int, default=40)
    args = ap.parse_args()

    meta, names, x, t, g, species = load(args.data)
    rng = np.random.default_rng(SEED)
    n_groups = int(g.max()) + 1
    perm = rng.permutation(n_groups)
    holdout = np.sort(perm[: n_groups // 6])
    train_groups = np.sort(perm[n_groups // 6:])
    starts, ends = group_offsets(g)
    hold_rows = np.concatenate([np.arange(starts[c], ends[c]) for c in holdout])
    train_rows = np.concatenate([np.arange(starts[c], ends[c]) for c in train_groups])

    kandidaten = {}
    for hidden in [(64, 32), (32, 16)]:
        model = train(x, t, g, train_groups, hidden, args.epochs, np.random.default_rng(SEED + sum(hidden)))
        kandidaten[hidden] = {
            "model": model,
            "parameters": model.parameter_count(),
            "train": evaluate(model, x[train_rows], t[train_rows], g[train_rows]),
            "holdout": evaluate(model, x[hold_rows], t[hold_rows], g[hold_rows]),
        }
        print(hidden, kandidaten[hidden]["parameters"], kandidaten[hidden]["holdout"])

    gross, klein = kandidaten[(64, 32)], kandidaten[(32, 16)]
    # "Falls kleiner ausreichend ist, kleiner bauen": Das kleinere Netz gewinnt, wenn es die
    # Verteilung des Lehrers auf ungesehenen Entscheidungen fast genauso gut trifft.
    klein_reicht = (klein["holdout"]["mean_kl"] <= 1.15 * gross["holdout"]["mean_kl"] + 0.002
                    and klein["holdout"]["top1_agreement"] >= gross["holdout"]["top1_agreement"] - 0.02)
    hidden = (32, 16) if klein_reicht else (64, 32)
    chosen = kandidaten[hidden]
    model = chosen["model"]
    temperature = 1.0

    data = export_onnx(
        model, names, args.out, temperature,
        {
            "feature_schema_version": meta["feature_schema_version"],
            "architecture": f"{len(names)}-{hidden[0]}-{hidden[1]}-1 relu",
            "dataset": f"{meta['decisions']} decisions / {meta['rows']} rows, {meta['days']} days x {meta['seeds']} seeds x 6 species",
        },
    )

    import onnxruntime as ort
    sess = ort.InferenceSession(args.out, providers=["CPUExecutionProvider"])
    probe = x[hold_rows].astype(np.float32)
    ort_out = sess.run(["score"], {"features": probe})[0][:, 0]
    np_out, _ = model.forward(probe.astype(np.float64))
    parity = float(np.max(np.abs(ort_out - np_out)))
    assert parity < 1e-3, f"onnxruntime disagrees with numpy by {parity}"

    # Referenzwerte fuer OnnxDecisionPolicyTest: dieselben Zeilen, dieselbe Datei, andere Laufzeit.
    ref_rows = hold_rows[:: max(1, len(hold_rows) // 40)][:40]
    ref_in = x[ref_rows].astype(np.float32)
    ref_out = sess.run(["score"], {"features": ref_in})[0][:, 0]
    with open(args.reference, "w") as f:
        f.write("# expected_score,features... - erzeugt von tools/decision-policy/train_policy.py (onnxruntime)\n")
        for y, row in zip(ref_out, ref_in):
            f.write(repr(float(y)) + "," + ",".join(repr(float(v)) for v in row) + "\n")

    report = {
        "policy_model_version": MODEL_VERSION,
        "feature_schema_version": meta["feature_schema_version"],
        "features": len(names),
        "chosen_architecture": f"{len(names)}-{hidden[0]}-{hidden[1]}-1",
        "chosen_because": "kleineres Netz reicht" if klein_reicht else "kleineres Netz verliert zu viel",
        "parameters": chosen["parameters"],
        "onnx_bytes": len(data),
        "onnx_sha256": hashlib.sha256(data).hexdigest(),
        "onnxruntime_vs_numpy_max_abs_diff": parity,
        "dataset": {k: meta[k] for k in ("rows", "decisions", "days", "seeds")},
        "holdout_decisions": int(len(holdout)),
        "epochs": args.epochs,
        "seed": SEED,
        "candidates": {
            f"{h[0]}-{h[1]}": {"parameters": c["parameters"], "train": c["train"], "holdout": c["holdout"]}
            for h, c in kandidaten.items()
        },
    }
    with open(args.report, "w") as f:
        json.dump(report, f, indent=2)
        f.write("\n")
    print(json.dumps({k: report[k] for k in ("chosen_architecture", "parameters", "onnx_bytes", "onnx_sha256")}, indent=2))


if __name__ == "__main__":
    main()
