---
name: bootstrap-audit
description: Audits IllyriaPlus registration wiring to ensure all utility enchantments, mechanics, recipes, and enchantment listeners are correctly registered.
---

# Bootstrap Audit

Use this skill when the user wants to verify that the plugin's registration wiring is consistent and complete.

## When to Use

- Before committing large refactors
- When enchantments are not showing up in-game
- When a mechanic/recipe is not taking effect
- When the user asks "is everything registered correctly?"

## Steps

1. Read `src/IllyriaPlus.kt` and extract:
   - The `recipes` list
   - The `mechanics` list
   - The `enchantments` (event listeners) list

2. Read `src/IllyriaPlusBootstrap.kt` and extract:
   - The item tags created in `LifecycleEvents.TAGS.preFlatten`
   - The enchantments registered in `RegistryEvents.ENCHANTMENT`
   - The enchantments added to `TRADEABLE`, `NON_TREASURE`, and `IN_ENCHANTING_TABLE` tags in `LifecycleEvents.TAGS.postFlatten`

3. Compare the source directories against the registration lists:
   - Every file in `src/enchantments/utility/` should be registered in the bootstrap registry AND tagged.
   - Every file in `src/enchantments/vanilla/` should be in the `enchantments` listener list in `IllyriaPlus.kt` (unless intentionally unused).
   - Every file in `src/mechanics/` (including subdirectories) should be in the `mechanics` list.
   - Every file in `src/recipes/vanilla/` should be in the `recipes` list.

4. Report findings:
   - Missing registrations (file exists but not listed)
   - Registrations without matching files (stale references)
   - Enchantments in bootstrap registry that are not tagged as tradeable/non-treasure/in-table
   - Item tags referenced but not defined

5. Do not auto-fix unless the user asks. Present a clear list of inconsistencies with file paths and line references.

After finishing, ask the user if they want you to apply any fixes and whether they want to commit the audit results.
