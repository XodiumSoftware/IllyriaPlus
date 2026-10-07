---
name: add-font-glyph
description: Adds a new custom font glyph to the IllyriaPlus resource pack using a bitmap font provider, optionally wired into language overrides or a Kotlin glyph enum.
---

# Add Font Glyph

Use this skill when the user wants to add a new custom glyph (icon, symbol, or sprite) renderable anywhere text is used — chat, dialogs, GUI titles, bossbars, actionbars — via Minecraft's font system.

## What this skill covers

- Adding a bitmap font provider to `IllyriaResourcePack/assets/minecraft/font/default.json`
- Placing or referencing the source PNG texture
- Optionally updating `IllyriaResourcePack/assets/minecraft/lang/en_us.json` with a translation override (e.g., bossbar icons)
- Optionally adding a Kotlin constant/enum entry so the glyph is easy to use from MiniMessage strings

## Steps

1. Ask the user for:
    - The glyph name (e.g., `raid`, `crown`)
    - The texture file path or source location
    - The Unicode private-use character to assign (e.g., `\uE900`)
    - The desired `ascent` and `height` values (optional; use reasonable defaults — `ascent: 8`, `height: 8` for inline text glyphs; `ascent: 13`, `height: 36` for big bossbar icons)

2. Make the texture available under the resource pack. Either:
    - Place a copy at `IllyriaResourcePack/assets/illyriacore/textures/font/{name}.png`, or
    - Reference an existing texture path directly (e.g., `illyriacore:item/icons/raid.png`) — no copy needed

3. Open `IllyriaResourcePack/assets/minecraft/font/default.json` (create it if missing with a top-level `"providers": []` array) and append a new bitmap provider:

    ```json
    {
        "type": "bitmap",
        "file": "illyriacore:font/{name}.png",
        "ascent": 8,
        "height": 8,
        "chars": ["\uE9XX"]
    }
    ```

    Use a unique Unicode private-use code point for each glyph. Verify uniqueness by scanning the existing providers first.

4. (Optional) For glyphs replacing vanilla translatable text (bossbar names, raid events), open `IllyriaResourcePack/assets/minecraft/lang/en_us.json` and add the translation override:
    - For a boss entity: `"entity.minecraft.{name}": "\uE9XX"`
    - For a raid event: `"event.minecraft.raid": "\uE9XX"`
    - Also add empty strings for raid sub-keys if needed (`raid.raiders_remaining`, `raid.victory`, `raid.defeat`)

5. (Optional) For glyphs used from plugin code, add an entry to a Kotlin glyph enum (e.g., `IllyriaCore/src/enums/GlyphEnum.kt`) so it can be interpolated into MiniMessage strings:

    ```kotlin
    RAID("\uE9XX"),
    ```

6. Run `./gradlew shadowJar` to verify the project still builds (resource pack files are not compiled, but this catches accidental Kotlin edits).

## Conventions

- Keep glyph textures namespaced under `illyriacore:` (either in `textures/font/` or referenced in place from another folder)
- Use private-use Unicode code points starting at `\uE901`
- Do not add KDoc; resource pack files are not Kotlin code
- Keep entries sorted by code point in `default.json` and alphabetically by key in `en_us.json` when possible
