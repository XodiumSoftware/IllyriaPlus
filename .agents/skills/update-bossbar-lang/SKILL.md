---
name: update-bossbar-lang
description: Updates or fixes Minecraft language overrides for custom bossbar icons in the IllyriaPlus resource pack.
---

# Update Bossbar Lang Overrides

Use this skill when the user needs to add, change, or remove the language overrides that make custom bossbar icons appear in-game.

## What this skill covers

- Editing `resourcepack/assets/minecraft/lang/en_us.json`
- Mapping vanilla boss/event translation keys to custom font Unicode characters

## Steps

1. Identify the vanilla translation key:
   - Wither: `entity.minecraft.wither`
   - Ender Dragon: `entity.minecraft.ender_dragon`
   - Raid: `event.minecraft.raid` (plus `event.minecraft.raid.raiders_remaining`, `event.minecraft.raid.victory`, `event.minecraft.raid.defeat`)

2. Find the corresponding Unicode code point from `resourcepack/assets/minecraft/font/default.json`.

3. Add or update the entry in `resourcepack/assets/minecraft/lang/en_us.json`:

   ```json
   "entity.minecraft.wither": "\uE901"
   ```

4. For raid events, also clear the sub-text by setting these to empty strings:

   ```json
   "event.minecraft.raid.raiders_remaining": "",
   "event.minecraft.raid.victory": "",
   "event.minecraft.raid.defeat": ""
   ```

5. Keep the language file alphabetically sorted by key where practical.

## Notes

- Language overrides must live in `assets/minecraft/lang/` because they override vanilla keys.
- The actual bitmap textures can (and should) live under `assets/illyriaplus/textures/font/`.
- Do not change `pack_format` unless the user explicitly asks for a format upgrade.
