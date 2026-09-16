---
name: wiki-add-page
description: Scaffolds a new page in IllyriaWiki following the wiki's structure, table formats, and SUMMARY.md registration.
---

# Add a Wiki Page

Use this skill when the user wants to document a new server feature, enchantment, weapon, recipe group, or mechanic in the wiki.

## Before Writing

1. Ask the user:
   - Page title and a one-line summary (`Break entire ore veins in one swing.`)
   - Which section it belongs to: `getting-started`, `datapacks`, `weapons`, `enchantments`, `qol`, `gameplay`, `recipes`, or `mods`
   - Any key properties (max level, applies-to, source, etc.) that fit a property table

2. Check whether a section index page (`<section>/index.md`) needs its list updated to mention the new page.

## File Conventions

- Path: `IllyriaWiki/src/<section>/<kebab-case-name>.md` (e.g. `enchantments/vinemine.md`)
- Filenames are lowercase kebab-case, matching existing siblings
- Start with `# Title`, then a one-sentence summary paragraph
- Use a two-column property table when the feature has structured metadata:

  ```markdown
  | Property   | Value                            |
  | ---------- | -------------------------------- |
  | Applies To | Pickaxes                         |
  | Max Level  | 3                                |
  | Source     | Enchanting table, anvil, trading |
  ```

- Follow with `## Effect` / `## Notes` sections describing behavior; add level/cost tables when relevant
- Keep tables plain text — method/recipe pages may use `<img class="mc-icon" ...>` icons (see wiki-add-icon skill)
- Link to related pages with relative paths (e.g. `[Tether](tether.md)`, `[Weapons](../weapons/index.md)`)

## Register in SUMMARY.md

1. Open `IllyriaWiki/src/SUMMARY.md`
2. Add `- [Title](<section>/<file>.md)` under the matching `# Section` heading
3. Keep entries within a section sorted: Overview first, then alphabetically by title

## Lint and Build

1. Run `npx markdownlint-cli2 --config .markdownlint.json "IllyriaWiki/src/**/*.md"` from the repo root
2. Run `mdbook build IllyriaWiki` from the repo root to verify it compiles

After finishing, summarize the files changed and ask the user if they want to commit.
