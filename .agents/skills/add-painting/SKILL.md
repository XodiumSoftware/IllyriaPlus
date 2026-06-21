# Add a Painting

Use this skill when the user wants to add a new painting variant to IllyriaPlus.

## Before Writing Code

1. Ask the user:
   - What is the painting key (snake_case registry fragment)?
   - What is its size as `width x height` in blocks?
   - Confirm the painting should be placeable (almost always yes).
2. Determine the class name from the key using `PascalCase` + `Painting` suffix, e.g. `alpha` → `AlphaPainting`.
   - The registry key, asset id, and title are all derived automatically from this class name.

## Creating the Painting File

1. Create `src/paintings/yapetto/<Name>Painting.kt`.
2. Make it implement `PaintingInterface` as an `internal object`.
3. Only specify `width` and `height` in `invoke()`.
   - `assetId` is set via `PaintingInterface.assetKey`.
   - `title` is set via `PaintingInterface.title`.
   - `author` is always `MM.deserialize(YAPETTO)`.
4. Ensure the class name maps to the intended registry key via `toRegistryKeyFragment("Painting")`.

## Wiring

1. Open `src/IllyriaPlusBootstrap.kt`.
2. Add the new painting object to the `paintings` list, keeping entries in alphabetical order by class name.
3. The `RegistryEvents.PAINTING_VARIANT` handler and `PaintingVariantTagKeys.PLACEABLE` registration already iterate over the list, so no extra wiring is required.

## Documentation

1. Update `ARCHITECTURE.md`:
   - Adjust the painting count if listed.
   - Ensure the painting section describes the current per-file `src/paintings/yapetto/` structure and the derived `assetKey`/`title` properties.
2. Add a concise KDoc comment to the new painting object.

## Template

```kotlin
package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: <key>. */
@Suppress("UnstableApiUsage")
internal object <Name>Painting : PaintingInterface {
    override fun invoke(
        builder: PaintingVariantRegistryEntry.Builder,
    ): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, assetKey))
            .width(<width>)
            .height(<height>)
            .title(MM.deserialize(title))
            .author(MM.deserialize(YAPETTO))
}
```

After finishing, summarize the files changed and ask the user if they want to commit.
