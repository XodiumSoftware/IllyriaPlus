# Recipe Images

Recipe grid images shown on the [Recipes](../recipes/index.md) page.

## Workflow

1. Open the [Crafting Recipe Generator](https://crafting.thedestruc7i0n.ca/).
2. Drag & drop the ingredients onto the grid exactly as defined in
   `IllyriaCore/src/recipes/vanilla/<Name>Recipe.kt`.
3. Set the output item and count on the right.
4. Click **Download** to export a PNG.
5. Save the PNG here using the naming convention below.
6. Reference it in the recipes page.

## Naming Convention

<table-name>_<method>.png

- `<table-name>` — the page/table grouping (e.g., `chainmail_helmet`, `wool_to_string`).
- `<method>` — one of: `crafting`, `furnace`, `blast_furnace`, `smoker`, `campfire`, `stonecutter`, `smithing`.

### Examples

- `chainmail_helmet_crafting.png`
- `rotten_flesh_smelting.png`
- `diamond_recycling_blast_furnace.png`
- `custom_paintings_stonecutter.png`

## Style Guidelines

- Leave the generator's background transparent (default export).
- Do **not** add arrows or labels — the generator's output already includes them.
- Keep filenames lowercase `snake_case`.
- One image per recipe; do not stack multiple recipes into a single image.
