#!/usr/bin/env python3
"""Generate one Itoeva music track from music/manifest.json via Stability AI.

The API key is read only from STABILITY_API_KEY. It is never written to disk.
Use --dry-run to validate a track definition without making a paid API call.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

import requests


DEFAULT_MANIFEST = Path("music/manifest.json")


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
    }
    missing = sorted(required - track.keys())
    if missing:
        raise SystemExit(f"Track is missing fields: {', '.join(missing)}")

    duration = int(track["duration_seconds"])
    if not 1 <= duration <= 190:
        raise SystemExit("duration_seconds must be between 1 and 190 for this endpoint")
    if track["output_format"] not in {"mp3", "wav"}:
        raise SystemExit("output_format must be mp3 or wav")
    if track["model"] not in {"stable-audio-2", "stable-audio-2.5"}:
        raise SystemExit("model must be stable-audio-2 or stable-audio-2.5")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--track-id", required=True)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--output-dir", type=Path, default=Path("generated/music"))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    manifest, track = load_track(args.manifest, args.track_id)
    validate_track(track)

    prompt_path = Path(track["prompt_file"])
    prompt = prompt_path.read_text(encoding="utf-8").strip()
    if not prompt:
        raise SystemExit(f"Prompt is empty: {prompt_path}")

    endpoint = manifest.get(
        "api_endpoint",
        "https://api.stability.ai/v2beta/audio/stable-audio-2/text-to-audio",
    )
    extension = track["output_format"]
    output_name = f"{track['android_resource']}.{extension}"
    output_path = args.output_dir / output_name
    metadata_path = args.output_dir / f"{track['android_resource']}.json"

    resolved = {
        "track_id": track["id"],
        "title": track["title"],
        "model": track["model"],
        "duration_seconds": int(track["duration_seconds"]),
        "output_format": extension,
        "android_resource": track["android_resource"],
        "prompt_file": str(prompt_path),
        "prompt_sha256": hashlib.sha256(prompt.encode("utf-8")).hexdigest(),
        "endpoint": endpoint,
        "output_path": str(output_path),
    }

    if args.dry_run:
        print(json.dumps(resolved, indent=2, ensure_ascii=False))
        return 0

    api_key = os.environ.get("STABILITY_API_KEY", "").strip()
    if not api_key:
        raise SystemExit(
            "STABILITY_API_KEY is missing. Add it as a GitHub Actions secret; never commit the key."
        )

    args.output_dir.mkdir(parents=True, exist_ok=True)

    response = requests.post(
        endpoint,
        headers={
            "authorization": f"Bearer {api_key}",
            "accept": "audio/*",
            "stability-client-id": "Itoeva",
        },
        files={"none": (None, "")},
        data={
            "prompt": prompt,
            "output_format": extension,
            "duration": str(track["duration_seconds"]),
            "model": track["model"],
        },
        timeout=360,
    )

    if response.status_code != 200:
        content_type = response.headers.get("content-type", "")
        if "json" in content_type:
            try:
                detail = json.dumps(response.json(), ensure_ascii=False)
            except ValueError:
                detail = response.text[:1000]
        else:
            detail = response.text[:1000]
        raise SystemExit(f"Stability API failed ({response.status_code}): {detail}")

    output_path.write_bytes(response.content)
    resolved.update(
        {
            "generated_at_utc": datetime.now(timezone.utc).isoformat(),
            "provider": manifest.get("provider", "stability-ai"),
            "response_content_type": response.headers.get("content-type"),
            "bytes": len(response.content),
        }
    )
    metadata_path.write_text(
        json.dumps(resolved, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )

    print(f"Generated {output_path} ({len(response.content)} bytes)")
    print(f"Metadata: {metadata_path}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except requests.RequestException as exc:
        print(f"Network error: {exc}", file=sys.stderr)
        raise SystemExit(2)
