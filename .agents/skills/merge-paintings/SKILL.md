---
name: merge-paintings
description: Synchronize IllyriaPlus painting item cases into the vanilla painting.json after manual resourcepack updates.
---

# merge-paintings

Run this skill after making any manual changes under `resourcepack/assets/illyriaplus/models/item/painting/`.

## What it does

Synchronizes IllyriaPlus painting item cases into `resourcepack/assets/minecraft/items/painting.json`.
It is idempotent: it removes any existing `illyriaplus:*` or `yapetto:*` cases from the vanilla painting item model, then re-adds cases for every IllyriaPlus painting model found under `resourcepack/assets/illyriaplus/models/item/painting/`.

## How to run

Create a temporary Python file from the script below, run it from the project root (`IllyriaPlus/`), then delete it.

To merge:

```bash
python /tmp/merge_yapetto_paintings.py
```

To verify the merge without modifying files:

```bash
python /tmp/merge_yapetto_paintings.py --check
```

## Script

```python
#!/usr/bin/env python3
"""Synchronize IllyriaPlus painting cases into the vanilla painting.json.

Run from the project root (IllyriaPlus/). The script is idempotent: it removes
any existing illyriaplus:* or yapetto:* cases from the vanilla painting item
model, then re-adds a case for every painting model found under
resourcepack/assets/illyriaplus/models/item/painting/.
"""

import json
import sys
from pathlib import Path

VANILLA_FILE = Path("resourcepack/assets/minecraft/items/painting.json")
MODELS_DIR = Path("resourcepack/assets/illyriaplus/models/item/painting")


def collect_variants(models_dir: Path) -> list[str]:
    if not models_dir.is_dir():
        return []
    return sorted(
        p.stem for p in models_dir.iterdir() if p.is_file() and p.suffix == ".json"
    )


def make_case(variant: str) -> dict:
    return {
        "when": f"illyriaplus:{variant}",
        "model": {
            "type": "minecraft:model",
            "model": f"illyriaplus:item/painting/{variant}",
        },
    }


def merge() -> None:
    if not VANILLA_FILE.is_file():
        print(f"Error: vanilla file not found: {VANILLA_FILE}", file=sys.stderr)
        sys.exit(1)

    data = json.loads(VANILLA_FILE.read_text(encoding="utf-8"))
    cases = data["model"]["models"][0]["cases"]

    vanilla_cases = [
        case
        for case in cases
        if not str(case.get("when", "")).startswith(("illyriaplus:", "yapetto:"))
    ]

    variants = collect_variants(MODELS_DIR)
    illyriaplus_cases = [make_case(v) for v in variants]

    data["model"]["models"][0]["cases"] = vanilla_cases + illyriaplus_cases

    VANILLA_FILE.write_text(
        json.dumps(data, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"Updated {VANILLA_FILE}")
    print(f"  Vanilla cases: {len(vanilla_cases)}")
    print(f"  Removed modded cases: {len(cases) - len(vanilla_cases)}")
    print(f"  Added illyriaplus cases: {len(illyriaplus_cases)}")
    print(f"  Total cases: {len(vanilla_cases) + len(illyriaplus_cases)}")


def check() -> int:
    if not VANILLA_FILE.is_file():
        print(f"Error: vanilla file not found: {VANILLA_FILE}", file=sys.stderr)
        return 1
    if not MODELS_DIR.is_dir():
        print(f"Error: models directory not found: {MODELS_DIR}", file=sys.stderr)
        return 1

    data = json.loads(VANILLA_FILE.read_text(encoding="utf-8"))
    cases = data["model"]["models"][0]["cases"]

    current = {
        str(case["when"]).removeprefix("illyriaplus:")
        for case in cases
        if str(case.get("when", "")).startswith("illyriaplus:")
    }
    expected = set(collect_variants(MODELS_DIR))

    missing = sorted(expected - current)
    extra = sorted(current - expected)

    if missing or extra:
        if missing:
            print(
                f"Error: painting.json is missing {len(missing)} IllyriaPlus case(s):",
                file=sys.stderr,
            )
            for variant in missing:
                print(f"  - illyriaplus:{variant}", file=sys.stderr)
        if extra:
            print(
                f"Error: painting.json has {len(extra)} unexpected IllyriaPlus case(s):",
                file=sys.stderr,
            )
            for variant in extra:
                print(f"  - illyriaplus:{variant}", file=sys.stderr)
        print(
            "Re-run the merge script to fix.",
            file=sys.stderr,
        )
        return 1

    print(f"Check passed: {len(expected)} IllyriaPlus cases present in painting.json")
    return 0


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] in ("--check", "-c"):
        sys.exit(check())
    merge()
```

## When to use

- After adding, removing, or renaming painting models in `resourcepack/assets/illyriaplus/models/item/painting/`.
- Before committing resourcepack changes.

## Conventions

- Do not hand-edit `resourcepack/assets/minecraft/items/painting.json` for IllyriaPlus cases; always use the merge script.
- The script preserves vanilla cases and only touches `illyriaplus:*` / `yapetto:*` entries.
