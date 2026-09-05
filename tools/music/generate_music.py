#!/usr/bin/env python3
"""Generate one Itoeva music track with Stable Audio 3 open weights.

The paid Stability API is intentionally not used. Model weights are downloaded from
Hugging Face by the official ``stable-audio-3`` inference library. The model is gated,
so the user running generation must have accepted its terms and authenticated with a
Hugging Face account (``HF_TOKEN`` works in CI).

``--dry-run`` validates the versioned track definition without importing PyTorch,
downloading weights, or generating audio.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from datetime import datetime, timezone
from pathlib import Path


DEFAULT_MANIFEST = Path("music/manifest.json")
SUPPORTED_MODELS = {"small-music": 120, "medium": 380}

# WAV stays allowed for a lossless local experiment; `ogg` is what the app ships.
SUPPORTED_OUTPUT_FORMATS = {"wav", "ogg"}

# libsndfile crashes on one large Vorbis write - see write_vorbis().
VORBIS_BLOCK_FRAMES = 8192


def load_track(manifest_path: Path, track_id: str) -> tuple[dict, dict]:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    matches = [track for track in manifest.get("tracks", []) if track.get("id") == track_id]
    if len(matches) != 1:
        known = ", ".join(track.get("id", "?") for track in manifest.get("tracks", []))
        raise SystemExit(f"Unknown or duplicate track id '{track_id}'. Known: {known}")
    return manifest, matches[0]


def validate_track(track: dict) -> None:
    required = {
        "id",
        "title",
        "model",
        "duration_seconds",
        "output_format",
        "android_resource",
        "prompt_file",
        "steps",
        "cfg_scale",
        "seed",
    }
    missing = sorted(required - track.keys())
    if missing:
        raise SystemExit(f"Track is missing fields: {', '.join(missing)}")

    model = track["model"]
    if model not in SUPPORTED_MODELS:
        raise SystemExit(f"model must be one of: {', '.join(sorted(SUPPORTED_MODELS))}")

    duration = int(track["duration_seconds"])
    if not 1 <= duration <= SUPPORTED_MODELS[model]:
        raise SystemExit(
            f"duration_seconds must be between 1 and {SUPPORTED_MODELS[model]} for {model}"
        )

    if track["output_format"] not in SUPPORTED_OUTPUT_FORMATS:
        raise SystemExit(
            "output_format must be one of: "
            + ", ".join(sorted(SUPPORTED_OUTPUT_FORMATS))
        )
    if int(track["steps"]) < 1:
        raise SystemExit("steps must be >= 1")
    if float(track["cfg_scale"]) <= 0:
        raise SystemExit("cfg_scale must be > 0")


def write_vorbis(output_path: Path, frames, sample_rate: int) -> None:
    """Write Ogg/Vorbis via libsndfile.

    Ships compressed rather than as WAV: the same 90 seconds are ~1 MB instead of ~16 MB,
    and the app has to carry one file per mood, time of day and place. Vorbis rather than
    Opus because ``:app-sim`` targets minSdk 26 and Opus in ``.ogg`` only decodes from
    Android 10 (API 29). See EVOLUTION.md, entry for 2026-09-05.

    Written in blocks on purpose. A single large write crashes libsndfile 1.2.2 with a
    segmentation fault for Vorbis at 44.1 kHz; the same data written in blocks is fine.
    """
    import soundfile as sf

    with sf.SoundFile(
        str(output_path),
        mode="w",
        samplerate=sample_rate,
        channels=frames.shape[1],
        format="OGG",
        subtype="VORBIS",
    ) as handle:
        for start in range(0, frames.shape[0], VORBIS_BLOCK_FRAMES):
            handle.write(frames[start : start + VORBIS_BLOCK_FRAMES])


def resolve_device(requested: str) -> str | None:
    if requested == "auto":
        return None
    if requested not in {"cpu", "cuda", "mps"}:
        raise SystemExit("--device must be auto, cpu, cuda or mps")
    return requested


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--track-id", required=True)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--output-dir", type=Path, default=Path("generated/music"))
    parser.add_argument("--device", default="auto", choices=["auto", "cpu", "cuda", "mps"])
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    manifest, track = load_track(args.manifest, args.track_id)
    validate_track(track)

    prompt_path = Path(track["prompt_file"])
    prompt = prompt_path.read_text(encoding="utf-8").strip()
    if not prompt:
        raise SystemExit(f"Prompt is empty: {prompt_path}")

    extension = track["output_format"]
    output_name = f"{track['android_resource']}.{extension}"
    output_path = args.output_dir / output_name
    metadata_path = args.output_dir / f"{track['android_resource']}.json"

    resolved = {
        "track_id": track["id"],
        "title": track["title"],
        "backend": manifest.get("backend", "stable-audio-3-open-weights"),
        "model": track["model"],
        "model_repository": manifest.get("model_repository"),
        "inference_library_repository": manifest.get("inference_library_repository"),
        "inference_library_commit": manifest.get("inference_library_commit"),
        "duration_seconds": int(track["duration_seconds"]),
        "output_format": extension,
        "android_resource": track["android_resource"],
        "prompt_file": str(prompt_path),
        "prompt_sha256": hashlib.sha256(prompt.encode("utf-8")).hexdigest(),
        "steps": int(track["steps"]),
        "cfg_scale": float(track["cfg_scale"]),
        "seed": int(track["seed"]),
        "requested_device": args.device,
        "output_path": str(output_path),
    }

    if args.dry_run:
        print(json.dumps(resolved, indent=2, ensure_ascii=False))
        return 0

    # Heavy dependencies are imported only for a real generation. This keeps dry-run
    # useful in ordinary repo CI without downloading PyTorch or model weights.
    try:
        import torch
        import torchaudio
        from stable_audio_3 import StableAudioModel
    except ImportError as exc:
        raise SystemExit(
            "Stable Audio 3 runtime is not installed. Follow music/README.md or use the "
            "Generate Itoeva Music GitHub workflow."
        ) from exc

    requested_device = resolve_device(args.device)
    if requested_device == "cuda" and not torch.cuda.is_available():
        raise SystemExit("CUDA was requested but torch.cuda.is_available() is false")
    if requested_device == "mps" and not torch.backends.mps.is_available():
        raise SystemExit("MPS was requested but torch.backends.mps.is_available() is false")

    # The model repository is gated. huggingface_hub automatically reads HF_TOKEN;
    # locally, an existing `hf auth login` session also works.
    if os.environ.get("CI") and not os.environ.get("HF_TOKEN"):
        raise SystemExit(
            "HF_TOKEN is missing in CI. Accept the Stable Audio 3 model terms on "
            "Hugging Face and add a read token as the GitHub Actions secret HF_TOKEN."
        )

    args.output_dir.mkdir(parents=True, exist_ok=True)

    print(f"Loading Stable Audio 3 model: {track['model']}")
    model = StableAudioModel.from_pretrained(track["model"], device=requested_device)
    actual_device = str(model.device)

    print(
        f"Generating {track['duration_seconds']}s on {actual_device} "
        f"(steps={track['steps']}, cfg={track['cfg_scale']}, seed={track['seed']})"
    )
    audio = model.generate(
        prompt=prompt,
        duration=float(track["duration_seconds"]),
        steps=int(track["steps"]),
        cfg_scale=float(track["cfg_scale"]),
        seed=int(track["seed"]),
        batch_size=1,
    )

    if audio.ndim != 3 or audio.shape[0] != 1:
        raise SystemExit(f"Unexpected Stable Audio output shape: {tuple(audio.shape)}")

    sample_rate = int(model.model.sample_rate)
    waveform = audio[0].detach().to(torch.float32).cpu().clamp(-1, 1)
    if extension == "ogg":
        write_vorbis(output_path, waveform.numpy().T, sample_rate)
    else:
        torchaudio.save(
            str(output_path),
            waveform,
            sample_rate,
            encoding="PCM_S",
            bits_per_sample=16,
        )

    resolved.update(
        {
            "generated_at_utc": datetime.now(timezone.utc).isoformat(),
            "actual_device": actual_device,
            "sample_rate": sample_rate,
            "channels": int(waveform.shape[0]),
            "samples": int(waveform.shape[-1]),
            "bytes": output_path.stat().st_size,
            "hf_token_present": bool(os.environ.get("HF_TOKEN")),
        }
    )
    metadata_path.write_text(
        json.dumps(resolved, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )

    print(f"Generated {output_path} ({output_path.stat().st_size} bytes)")
    print(f"Metadata: {metadata_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
