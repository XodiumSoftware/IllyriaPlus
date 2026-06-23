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

```bash
./gradlew mergeYapettoPaintings
```

To verify the merge without modifying files:

```bash
./gradlew checkYapettoPaintings
```

## When to use

- After adding, removing, or renaming painting models in `resourcepack/assets/illyriaplus/models/item/painting/`.
- Before committing resourcepack changes.
- CI will fail the resourcepack workflow if `checkYapettoPaintings` reports missing or extra cases.

## Conventions

- Do not hand-edit `resourcepack/assets/minecraft/items/painting.json` for IllyriaPlus cases; always use the Gradle task.
- The task preserves vanilla cases and only touches `illyriaplus:*` / `yapetto:*` entries.
