---
name: wiki-sync
description: Synchronizes IllyriaWiki pages with the current plugin codebase after enchantments, mechanics, recipes, or weapons are added/changed/removed.
---

# Sync Wiki with Code

Use this skill after code changes to keep the wiki truthful: new enchantment/mechanic/recipe/weapon objects in the plugin must have matching wiki pages, and removed features must be removed from the wiki.

## Trigger

Load this skill when:
- The user says "sync the wiki", "update the wiki to match", or similar
- You just added/removed/changed an entry in `IllyriaCore.kt`'s `mechanics`, `enchantments`, or `recipes` lists
- You just registered a new utility enchantment in `IllyriaCoreBootstrap.kt`

Do **not** use it for `GUIDE.md` or `README.md` — that's the `update-plugin-docs` skill.

## Audit Steps

1. Read the registration lists in:
   - `IllyriaCore/src/IllyriaCore.kt` — `mechanics`, `enchantments`, `recipes`
   - `IllyriaCore/src/IllyriaCoreBootstrap.kt` — custom enchantments registered into the Paper registry

2. Read `IllyriaWiki/src/SUMMARY.md` for the documented pages.

3. Diff code vs. wiki:
   - **New object in code, no wiki page** → create one (use the `wiki-add-page` skill's conventions)
   - **Wiki page exists, object removed from code** → remove the page and its `SUMMARY.md` entry; check for inbound links from other pages with `grep` (e.g. `grep -r "name.md" IllyriaWiki/src/`) and fix them
   - **Behavior changed in code** → update the page's Effect/Notes/property tables to match

4. Verify the introduction page (`src/introduction.md`) still describes the feature set accurately; update its card grid or About bullets if a whole category was added/removed.

## Section Mapping

| Code concept                         | Wiki section           |
| ------------------------------------ | ---------------------- |
| `enchantments/utility/*` (registry)  | `enchantments/`        |
| `enchantments/vanilla/*` (overrides) | `enchantments/vanilla-tweaks.md` |
| `mechanics/player|qol`               | `qol/`                 |
| `mechanics/world|entity|server`      | `gameplay/`            |
| `recipes/vanilla/*`                  | `recipes/index.md`     |
| Custom weapons (smithing)            | `weapons/`             |
| IllyriaBridge bridges                | `mods/`                |

Use judgment for edge cases — ask the user if a feature's placement is ambiguous.

## Verify

1. `npx markdownlint-cli2 --config .markdownlint.json "IllyriaWiki/src/**/*.md"` — 0 issues
2. `mdbook build IllyriaWiki` — builds without warnings about missing files referenced in `SUMMARY.md`

After finishing, summarize what wiki pages were added/updated/removed and ask the user if they want to commit.
