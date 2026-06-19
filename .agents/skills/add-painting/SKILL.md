# Add a Painting

Use this skill when the user wants to add a new painting variant to IllyriaPlus.

## Before Writing Code

1. Ask the user:
   - What is the painting key (snake_case registry fragment)?
   - What is its size as `width x height` in blocks?
   - What namespace should the sprite asset live under? (default: `IllyriaPlus.ID` / `illyriaplus`)
   - What translation keys should be used for title and author? (default: `painting.<IllyriaPlus.ID>.<key>.title` and `painting.<IllyriaPlus.ID>.<key>.author`)
2. Confirm the painting should be placeable (almost always yes).

## Creating the Painting Data

1. Open `src/paintings/YapettoPaintings.kt`.
2. Add a new `PaintingData("key", Pair(width, height))` entry to the `DATA` list, keeping entries in alphabetical order.
   - If the painting needs non-default title/author keys, pass them explicitly:
     `PaintingData("key", Pair(width, height), title = "...", author = "...")`.
3. If the new painting needs its own file (e.g. it has custom behavior), create `src/paintings/<Name>Painting.kt` instead and make it implement `PaintingInterface`. Use `PaintingData` for metadata and reference the top-level shared vals in `YapettoPaintings` if appropriate.

## Wiring

1. Open `src/IllyriaPlusBootstrap.kt`.
2. If using `YapettoPaintings`, no wiring is required — it is already registered in the `RegistryEvents.PAINTING_VARIANT` handler and added to `PaintingVariantTagKeys.PLACEABLE`.
3. If creating a standalone painting object, register it in the `RegistryEvents.PAINTING_VARIANT` handler:
   ```kotlin
   register(<Name>Painting.key) { <Name>Painting.invoke(it) }
   ```
   and add its key to the `PLACEABLE` tag.

## Documentation

1. Update `ARCHITECTURE.md`:
   - Adjust the painting count if using `YapettoPaintings`.
   - Add the painting to the painting list/table if it is standalone.
2. Add a concise KDoc comment if creating a standalone painting object.

## Template

```kotlin
package org.xodium.illyriaplus.paintings

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.xodium.illyriaplus.data.PaintingData

/** Represents the <Name> painting variant. */
@Suppress("UnstableApiUsage")
internal object <Name>Painting : PaintingInterface {
    private val DATA = PaintingData("<key>", Pair(<width>, <height>))

    override fun invoke(
        builder: PaintingVariantRegistryEntry.Builder,
    ): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YapettoPaintings.ASSET_NAMESPACE, DATA.key))
            .width(DATA.size.first)
            .height(DATA.size.second)
            .title(Component.translatable(DATA.title).color(YapettoPaintings.TITLE_COLOR))
            .author(Component.translatable(DATA.author).color(YapettoPaintings.AUTHOR_COLOR))
}
```

After finishing, summarize the files changed and ask the user if they want to commit.
