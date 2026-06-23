---
name: add-painting
description: Scaffold a new IllyriaPlus painting variant in the Yapetto paintings list.
---

# Add a Painting

Use this skill when the user wants to add a new painting variant to IllyriaPlus.

## Before Writing Code

1. Ask the user:
   - What is the painting key (snake_case registry fragment)?
   - What is its size as `width x height` in blocks?
   - Confirm the painting should be placeable (almost always yes).
2. Determine the display title by converting the key with `snakeToProperCase` (e.g. `an_intruder` → `An Intruder`).

## Adding the Painting

1. Open `src/paintings/YapettoPaintings.kt`.
2. Add a new `PaintingData("<key>", Pair(<width>, <height>), YAPETTO)` entry to the `paintings` list.
3. Keep entries in alphabetical order by registry key.

## Wiring

No additional wiring is required:

- `IllyriaPlusBootstrap.kt` already iterates over `YapettoPaintings.paintings` to register every variant with `RegistryEvents.PAINTING_VARIANT`.
- Placeable tagging is handled automatically by `LifecycleEvents.TAGS.postFlatten(RegistryKey.PAINTING_VARIANT)` using the same list.

## Documentation

1. Update `ARCHITECTURE.md`:
   - Adjust the painting count if listed.
   - Ensure the painting section describes the current `src/paintings/YapettoPaintings.kt` list structure.
2. No per-painting KDoc is required; the list is self-documenting.

## Template

```kotlin
PaintingData("<key>", Pair(<width>, <height>), YAPETTO),
```

After finishing, summarize the files changed and ask the user if they want to commit.
