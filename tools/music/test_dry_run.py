"""The dry run must stay free of audio dependencies.

This exists because of a real failure. PR #101 added the release gate and imported
``audio_polish`` - and with it numpy - at the top of ``generate_music.py``. The generation
workflow runs ``--dry-run`` as its FIRST step, before installing anything, so that a typo in
the manifest fails in seconds instead of after a model download. The module-level import
turned that step into an unconditional ``ModuleNotFoundError``, and the very next generation
run died on it (run 34031057429, 2026-09-06).

Ordinary CI could not see it: ``verify-music-tooling.yml`` installs numpy and soundfile before
it calls the dry run, so there the import always succeeded. The check therefore has to be an
explicit one - run the dry run in a subprocess with the audio modules made unimportable.
"""

from __future__ import annotations

import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parent
REPO = TOOLS.parent.parent
GENERATOR = TOOLS / "generate_music.py"

# Everything the dry run must not need. soundfile is listed as well: it is installed by the
# same workflow step as numpy, so a top-level import of it would fail in exactly the same way.
BLOCKED = ("numpy", "soundfile", "torch", "torchaudio", "stable_audio_3")


def shadowed_env(shadow: str) -> dict:
    """The real environment, with `shadow` put ahead of site-packages.

    Inherited rather than replaced on purpose: a hosted runner's Python needs its own
    LD_LIBRARY_PATH to start at all, and a subprocess that cannot start would fail this test
    for the wrong reason. PYTHONPATH is searched before site-packages, so the stubs still win.
    """
    env = dict(os.environ)
    env["PYTHONPATH"] = shadow
    return env


class DryRunDependencyTest(unittest.TestCase):
    def run_dry(self, blocked: tuple[str, ...]) -> subprocess.CompletedProcess:
        """Run --dry-run with `blocked` modules made unimportable.

        A stub package that raises on import beats deleting the real one: it reproduces the
        bare runner (module simply absent) without touching the environment of other tests.
        """
        with tempfile.TemporaryDirectory() as shadow:
            for name in blocked:
                (Path(shadow) / f"{name}.py").write_text(
                    f'raise ImportError("{name} is not installed on the bare runner")\n',
                    encoding="utf-8",
                )
            return subprocess.run(
                [sys.executable, str(GENERATOR), "--track-id", "main-day-01", "--dry-run"],
                cwd=REPO,
                env=shadowed_env(shadow),
                capture_output=True,
                text=True,
            )

    def test_dry_run_needs_no_audio_deps(self):
        result = self.run_dry(BLOCKED)
        self.assertEqual(
            result.returncode,
            0,
            f"Dry run must work without {', '.join(BLOCKED)}.\n"
            f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}",
        )
        self.assertIn("main-day-01", result.stdout)

    def test_guard_would_notice_a_top_level_import(self):
        """The stubs really do break an import - otherwise the test above proves nothing.

        Without this, a typo in the stub names would make the guard silently vacuous.
        """
        with tempfile.TemporaryDirectory() as shadow:
            (Path(shadow) / "numpy.py").write_text('raise ImportError("blocked")\n', encoding="utf-8")
            result = subprocess.run(
                [sys.executable, "-c", "import numpy"],
                env=shadowed_env(shadow),
                capture_output=True,
                text=True,
            )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("blocked", result.stderr)

    def test_every_manifest_track_passes_the_bare_dry_run(self):
        """Not just one id: the workflow is dispatched with whichever track the user picks."""
        import json

        manifest = json.loads((REPO / "music" / "manifest.json").read_text(encoding="utf-8"))
        with tempfile.TemporaryDirectory() as shadow:
            for name in BLOCKED:
                (Path(shadow) / f"{name}.py").write_text('raise ImportError("x")\n', encoding="utf-8")
            for track in manifest["tracks"]:
                with self.subTest(track=track["id"]):
                    result = subprocess.run(
                        [sys.executable, str(GENERATOR), "--track-id", track["id"], "--dry-run"],
                        cwd=REPO,
                        env=shadowed_env(shadow),
                        capture_output=True,
                        text=True,
                    )
                    self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == "__main__":
    unittest.main()
