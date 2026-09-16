---
name: wiki-add-icon
description: Adds a Minecraft texture icon to an IllyriaWiki page by copying it from IllyriaResourcePack into the wiki's image folder.
---

# Add a Minecraft Icon to a Wiki Page

Use this skill when the user wants to show an in-game item/block/method icon next to text in a wiki page (e.g. the crafting method icons in `src/recipes/index.md`).

## Icon Source

Icons come from the project's resource pack: `IllyriaResourcePack/assets/minecraft/textures/`.

Common locations:
- Block faces (machines, stations): `block/<name>_front.png` (e.g. `furnace_front.png`, `crafting_table_front.png`, `blast_furnace_front.png`, `stonecutter_side.png`)
- Items: `item/<name>.png`

Use `find_path` with a glob like `IllyriaPlus/IllyriaResourcePack/assets/minecraft/textures/block/<thing>*.png` to locate the right texture. Prefer `*_front.png` for stations; use `_side` or `_top` only when no front face exists.

## Steps

1. Locate the texture in the resource pack.
2. Copy it into the wiki under `IllyriaWiki/src/img/<category>/<name>.png`, where `<category>` groups by use (e.g. `method` for crafting stations). Create the folder if needed. Strip suffixes like `_front`/`_side` from the destination filename (e.g. `furnace_front.png` → `furnace.png`).
3. In the Markdown page, reference it as:

   ```html
   <img class="mc-icon" alt="" src="../img/<category>/<name>.png"> Label
   ```

   - Path is relative to the page's folder (`../img/...` from a section page; adjust depth for nested pages)
   - `class="mc-icon"` is required — it applies the pixel-art sizing (`~1.4em`, `image-rendering: pixelated`, inline alignment) defined in `theme/cards.css`
   - Always include `alt=""` when the icon is decorative and followed by a text label (MD045); only use a non-empty alt if the icon conveys information not present in text

## Verify

1. `mdbook build IllyriaWiki` — confirm the image lands in `book/img/<category>/`
2. `npx markdownlint-cli2 --config .markdownlint.json "IllyriaWiki/src/**/*.md"` from the repo root — must pass with 0 issues

After finishing, summarize the files changed and ask the user if they want to commit.
