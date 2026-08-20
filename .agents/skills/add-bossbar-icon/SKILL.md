---
name: add-bossbar-icon
description: Adds a new custom bossbar icon to the IllyriaPlus resource pack using a bitmap font provider and updates the bossbar language overrides.
---

# Add Bossbar Icon

Use this skill when the user wants to add a new custom bossbar icon for a vanilla boss or event (e.g., Wither, Ender Dragon, Raid).

## What this skill covers

- Adding a bitmap font provider to `resourcepack/assets/minecraft/font/default.json`
- Placing the source PNG texture into `resourcepack/assets/illyriaplus/textures/font/`
- Updating `resourcepack/assets/minecraft/lang/en_us.json` with the appropriate translation override

## Steps

1. Ask the user for:
   - The boss/event name (e.g., `entity.minecraft.wither`, `event.minecraft.raid`)
   - The texture file path or source location
   - The Unicode private-use character to assign (e.g., `\uE901`)
   - The desired `ascent` and `height` values (optional; use reasonable defaults)

2. Place the texture at `resourcepack/assets/illyriaplus/textures/font/{name}.png`.

3. Open `resourcepack/assets/minecraft/font/default.json` and append a new bitmap provider:

   ```json
   {
       "type": "bitmap",
       "file": "illyriaplus:font/{name}.png",
       "ascent": 13,
       "height": 36,
       "chars": ["\uE9XX"]
   }
   ```

   Use a unique Unicode private-use code point for each icon.

4. Open `resourcepack/assets/minecraft/lang/en_us.json` and add the translation override:
   - For a boss entity: `"entity.minecraft.{name}": "\uE9XX"`
   - For a raid event: `"event.minecraft.raid": "\uE9XX"`
   - Also add empty strings for raid sub-keys if needed (`raid.raiders_remaining`, `raid.victory`, `raid.defeat`)

5. Run `./gradlew shadowJar` to verify the project still builds.

## Conventions

- Keep bossbar icons namespaced under `illyriaplus:font/`
- Use private-use Unicode code points starting at `\uE901`
- Do not add KDoc; resource pack files are not Kotlin code
- Keep entries sorted by code point in `default.json` and alphabetically by key in `en_us.json` when possible
