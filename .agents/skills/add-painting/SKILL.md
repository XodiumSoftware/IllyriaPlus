---
name: add-painting
description: Scaffold a new custom painting variant in the project's paintings list.
---

# Add a Painting

Use this skill when the user wants to add a new custom painting variant to a Paper-based project.

## Before Writing Code

1. Ask the user:
   - What is the painting key (snake_case registry fragment)?
   - What is its size as `width x height` in blocks?
   - Confirm the painting should be placeable (almost always yes).
2. Determine the display title by converting the key with a helper like `snakeToProperCase` (e.g. `an_intruder` → `An Intruder`).

## Adding the Painting

1. Open the project's paintings registry file (e.g., `src/paintings/ProjectPaintings.kt`).
2. Add a new `PaintingData("<key>", Pair(<width>, <height>), <NAMESPACE>)` entry to the `paintings` list.
3. Keep entries in alphabetical order by registry key.

## Wiring

No additional wiring is required when the project already iterates over the paintings list to register variants with `RegistryEvents.PAINTING_VARIANT` and tag them as placeable via `LifecycleEvents.TAGS.postFlatten(RegistryKey.PAINTING_VARIANT)`.

If the project does not have that plumbing yet, add it:
1. In the plugin bootstrap, iterate over `paintings` and call `register(
   TypedKey.create(RegistryKey.PAINTING_VARIANT, NamespacedKey(namespace, key))
) { ... }` for each entry.
2. Tag each variant as placeable in the post-flatten tag event.

## Documentation

1. Update `ARCHITECTURE.md`:
   - Adjust the painting count if listed.
   - Ensure the painting section describes the current paintings list structure.
2. No per-painting KDoc is required; the list is self-documenting.

## Template

```kotlin
PaintingData("<key>", Pair(<width>, <height>), <NAMESPACE_TAG>),
```

After finishing, summarize the files changed and ask the user if they want to commit.
