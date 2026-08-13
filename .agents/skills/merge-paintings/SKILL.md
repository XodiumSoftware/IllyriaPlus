---
name: merge-paintings
description: Synchronize custom painting item cases into the vanilla painting.json after resourcepack updates.
---

# merge-paintings

Run this skill after making any manual changes under the project's custom painting models directory (e.g., `resourcepack/assets/<namespace>/models/item/painting/`).

## What it does

Synchronizes custom painting item cases into the vanilla `painting.json` item model.
It is idempotent: it removes any existing `<namespace>:*` cases from the vanilla painting item model, then re-adds cases for every custom painting model found under the project's models directory.

## How to run

Create a temporary Python file from the script below, run it from the project root, then delete it.

To merge:

```bash
python /tmp/merge_custom_paintings.py
```

To verify the merge without modifying files:

```bash
python /tmp/merge_custom_paintings.py --check
```

## Script

```python
#!/usr/bin/env python3
"""Synchronize custom painting cases into the vanilla painting.json.

Run from the project root. The script is idempotent: it removes any existing
<NAMESPACE>:* cases from the vanilla painting item model, then re-adds a case
for every painting model found under the project's custom models directory.
"""

import json
import sys
from pathlib import Path

# CONFIGURE THESE FOR THE PROJECT
NAMESPACE = "example"
VANILLA_FILE = Path("resourcepack/assets/minecraft/items/painting.json")
MODELS_DIR = Path(f"resourcepack/assets/{NAMESPACE}/models/item/painting")


def collect_variants(models_dir: Path) -> list[str]:
    if not models_dir.is_dir():
        return []
    return sorted(
        p.stem for p in models_dir.iterdir() if p.is_file() and p.suffix == ".json"
    )


def make_case(variant: str, namespace: str) -> dict:
    return {
        "when": f"{namespace}:{variant}",
        "model": {
            "type": "minecraft:model",
            "model": f"{namespace}:item/painting/{variant}",
        },
    }


def merge(namespace: str) -> None:
    if not VANILLA_FILE.is_file():
        print(f"Error: vanilla file not found: {VANILLA_FILE}", file=sys.stderr)
        sys.exit(1)

    data = json.loads(VANILLA_FILE.read_text(encoding="utf-8"))
    cases = data["model"]["models"][0]["cases"]

    vanilla_cases = [
        case
        for case in cases
        if not str(case.get("when", "")).startswith(f"{namespace}:")
    ]

    variants = collect_variants(MODELS_DIR)
    custom_cases = [make_case(v, namespace) for v in variants]

    data["model"]["models"][0]["cases"] = vanilla_cases + custom_cases

    VANILLA_FILE.write_text(
        json.dumps(data, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"Updated {VANILLA_FILE}")
    print(f"  Vanilla cases: {len(vanilla_cases)}")
    print(f"  Removed custom cases: {len(cases) - len(vanilla_cases)}")
    print(f"  Added {namespace} cases: {len(custom_cases)}")
    print(f"  Total cases: {len(vanilla_cases) + len(custom_cases)}")


def check(namespace: str) -> int:
    if not VANILLA_FILE.is_file():
        print(f"Error: vanilla file not found: {VANILLA_FILE}", file=sys.stderr)
        return 1
    if not MODELS_DIR.is_dir():
        print(f"Error: models directory not found: {MODELS_DIR}", file=sys.stderr)
        return 1

    data = json.loads(VANILLA_FILE.read_text(encoding="utf-8"))
    cases = data["model"]["models"][0]["cases"]

    current = {
        str(case["when"]).removeprefix(f"{namespace}:")
        for case in cases
        if str(case.get("when", "")).startswith(f"{namespace}:")
    }
    expected = set(collect_variants(MODELS_DIR))

    missing = sorted(expected - current)
    extra = sorted(current - expected)

    if missing or extra:
        if missing:
            print(
                f"Error: painting.json is missing {len(missing)} {namespace} case(s):",
                file=sys.stderr,
            )
            for variant in missing:
                print(f"  - {namespace}:{variant}", file=sys.stderr)
        if extra:
            print(
                f"Error: painting.json has {len(extra)} unexpected {namespace} case(s):",
                file=sys.stderr,
            )
            for variant in extra:
                print(f"  - {namespace}:{variant}", file=sys.stderr)
        print(
            "Re-run the merge script to fix.",
            file=sys.stderr,
        )
        return 1

    print(f"Check passed: {len(expected)} {namespace} cases present in painting.json")
    return 0


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] in ("--check", "-c"):
        sys.exit(check(NAMESPACE))
    merge(NAMESPACE)
```

## When to use

- After adding, removing, or renaming painting models in the project's custom models directory.
- Before committing resourcepack changes.

## Conventions

- Do not hand-edit the vanilla `painting.json` for custom cases; always use the merge script.
- The script preserves vanilla cases and only touches `<namespace>:*` entries.
- Replace `NAMESPACE` in the script with the project's actual namespace.
