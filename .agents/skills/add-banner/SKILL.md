---
name: add-banner
description: Adds a custom banner pattern or banner-related item/mechanic to the IllyriaPlus project.
---

# Add Banner

Use this skill when the user wants to add a custom banner pattern, banner item, or banner-related mechanic to the project.

## What this skill covers

- Resource-pack banner patterns (textures and pattern JSON)
- Custom banner items via the item builder system (`src/items/`)
- Banner-related gameplay mechanics (`src/mechanics/`)

## Before writing anything

Ask the user to clarify:

1. Is this a **resource-pack pattern** (new banner design), a **custom banner item** (e.g., a prefab banner), or a **gameplay mechanic** that reacts to banners?
2. What is the internal name/key (snake_case or lowerCamelCase)?
3. For patterns: is there a source PNG, and what dye color / default palette should it use?
4. For items/mechanics: which Bukkit events, materials, or data components are involved?

## Resource-pack banner pattern

1. Place the pattern texture in the appropriate resource pack path:
   - `resourcepack/assets/minecraft/textures/entity/banner/{pattern}.png`
   - If namespaced: `resourcepack/assets/illyriaplus/textures/entity/banner/{pattern}.png`
2. Add the pattern definition to `resourcepack/assets/minecraft/banner_pattern/{pattern}.json` if the project uses custom pattern registry entries.
3. Update language overrides in `resourcepack/assets/minecraft/lang/en_us.json`:
   - `"block.minecraft.banner.{pattern}.{color}": "<Display Name>"` if applicable

## Custom banner item

1. Create `src/items/banners/<Name>BannerItem.kt` (or use an existing group folder).
2. Implement `internal object <Name>BannerItem : ItemInterface`.
3. Build an `ItemStack` with `Material.WHITE_BANNER` (or another base color) and use `BannerMeta` to set patterns.
4. Add concise KDoc.
5. Reference the item from any recipe or mechanic that needs it.

## Banner mechanic

1. Create `src/mechanics/{category}/<Name>BannerMechanic.kt`.
2. Implement `internal object <Name>BannerMechanic : MechanicInterface`.
3. Add `@EventHandler fun on(event: <EventType>)` methods as needed.
4. Register it in `src/IllyriaPlus.kt` in the `mechanics` list.
5. Add concise KDoc.

## Conventions

- Keep banner assets namespaced under `illyriaplus:` when they are custom (not vanilla overrides).
- Use `BannerMeta` and `PatternType` for code-based banner customization.
- Do not create nested `Config` objects; hardcode constants directly in the mechanic/item object.
- Keep the `mechanics` list in `IllyriaPlus.kt` alphabetically sorted.

## Validation

After making changes, run:

```bash
./gradlew shadowJar
./gradlew ktlintCheck
```

If the change includes KDoc or public API changes, also run `./gradlew dokkaGenerateHtml`.
