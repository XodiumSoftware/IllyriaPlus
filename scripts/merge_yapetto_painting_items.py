#!/usr/bin/env python3
"""Merge IllyriaPlus painting item cases into assets/minecraft/items/painting.json.

This script is idempotent: it removes any existing illyriaplus:* or yapetto:*
cases from the vanilla painting item model, then re-adds cases for every
IllyriaPlus painting model found under assets/illyriaplus/models/item/painting/.

Run it from the resource pack root (or repo root):
    python3 scripts/merge_yapetto_painting_items.py

To verify the merge without modifying files:
    python3 scripts/merge_yapetto_painting_items.py --check
"""

import json
import os
import sys

VANILLA_FILE = "assets/minecraft/items/painting.json"
ILLYRIAPLUS_MODELS_DIR = "assets/illyriaplus/models/item/painting"
ILLYRIAPLUS_NAMESPACE = "illyriaplus"


def find_pack_root() -> str | None:
    """Find the directory containing pack.mcmeta and the vanilla painting file.

    Searches the current working directory, then a child named 'irp', then
    walks up the tree looking for the resource pack root.
    """
    candidates = [os.getcwd(), os.path.join(os.getcwd(), "irp")]
    for candidate in candidates:
        if os.path.isfile(os.path.join(candidate, "pack.mcmeta")) and os.path.isfile(
            os.path.join(candidate, VANILLA_FILE)
        ):
            return candidate

    path = os.getcwd()
    for _ in range(5):
        if os.path.isfile(os.path.join(path, "pack.mcmeta")) and os.path.isfile(
            os.path.join(path, VANILLA_FILE)
        ):
            return path
        parent = os.path.dirname(path)
        if parent == path:
            break
        path = parent
    return None


def load_json(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_json(path: str, data: dict) -> None:
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def collect_illyriaplus_variants(models_dir: str) -> list[str]:
    """Return sorted IllyriaPlus painting variant names from model file names."""
    if not os.path.isdir(models_dir):
        return []
    variants = []
    for filename in os.listdir(models_dir):
        if filename.endswith(".json"):
            variants.append(os.path.splitext(filename)[0])
    return sorted(variants)


def make_case(variant: str) -> dict:
    """Build a single select case for an IllyriaPlus painting variant."""
    return {
        "when": f"{ILLYRIAPLUS_NAMESPACE}:{variant}",
        "model": {
            "type": "minecraft:model",
            "model": f"{ILLYRIAPLUS_NAMESPACE}:item/painting/{variant}",
        },
    }


def get_current_illyriaplus_cases(cases: list[dict]) -> set[str]:
    """Return the set of illyriaplus:* variant names currently in the cases array."""
    variants = set()
    for case in cases:
        when = str(case.get("when", ""))
        if when.startswith("illyriaplus:"):
            variants.add(when[len("illyriaplus:") :])
    return variants


def check_merge(
    data: dict, expected_variants: list[str]
) -> tuple[bool, set[str], set[str]]:
    """Return (ok, missing, extra) comparing current cases to expected variants."""
    try:
        select_model = data["model"]["models"][0]
        cases = select_model["cases"]
    except (KeyError, IndexError, TypeError):
        return (False, set(expected_variants), set())

    current = get_current_illyriaplus_cases(cases)
    expected = set(expected_variants)
    missing = expected - current
    extra = current - expected
    return (not missing and not extra, missing, extra)


def merge(data: dict, expected_variants: list[str]) -> tuple[dict, int, int, int]:
    """Return (updated_data, vanilla_count, illyriaplus_count, removed_count)."""
    select_model = data["model"]["models"][0]
    cases = select_model["cases"]

    def _is_modded_case(case: dict) -> bool:
        when = str(case.get("when", ""))
        return when.startswith(f"{ILLYRIAPLUS_NAMESPACE}:") or when.startswith(
            "yapetto:"
        )

    vanilla_cases = [c for c in cases if not _is_modded_case(c)]
    removed = len(cases) - len(vanilla_cases)

    illyriaplus_cases = [make_case(v) for v in expected_variants]
    select_model["cases"] = vanilla_cases + illyriaplus_cases

    return data, len(vanilla_cases), len(illyriaplus_cases), removed


def main() -> int:
    check_mode = "--check" in sys.argv

    root = find_pack_root()
    if root is None:
        print(
            f"Error: could not find resource pack root containing pack.mcmeta and {VANILLA_FILE}.",
            file=sys.stderr,
        )
        return 1

    os.chdir(root)
    if not check_mode:
        print(f"Working in resource pack root: {root}")

    data = load_json(VANILLA_FILE)
    illyriaplus_variants = collect_illyriaplus_variants(ILLYRIAPLUS_MODELS_DIR)

    if check_mode:
        ok, missing, extra = check_merge(data, illyriaplus_variants)
        if not ok:
            if missing:
                print(
                    f"Error: {VANILLA_FILE} is missing {len(missing)} IllyriaPlus case(s):",
                    file=sys.stderr,
                )
                for v in sorted(missing):
                    print(f"  - illyriaplus:{v}", file=sys.stderr)
            if extra:
                print(
                    f"Error: {VANILLA_FILE} has {len(extra)} unexpected IllyriaPlus case(s):",
                    file=sys.stderr,
                )
                for v in sorted(extra):
                    print(f"  - illyriaplus:{v}", file=sys.stderr)
            print(
                "Run 'python3 scripts/merge_yapetto_painting_items.py' to fix.",
                file=sys.stderr,
            )
            return 1
        print(
            f"Check passed: {len(illyriaplus_variants)} IllyriaPlus cases present in {VANILLA_FILE}"
        )
        return 0

    data, vanilla_count, illyriaplus_count, removed = merge(data, illyriaplus_variants)
    save_json(VANILLA_FILE, data)

    print(f"Updated {VANILLA_FILE}")
    print(f"  Vanilla cases: {vanilla_count}")
    print(f"  Removed old illyriaplus cases: {removed}")
    print(f"  Added illyriaplus cases: {illyriaplus_count}")
    print(f"  Total cases: {vanilla_count + illyriaplus_count}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
