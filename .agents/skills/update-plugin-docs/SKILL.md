---
name: update-plugin-docs
description: Updates IllyriaPlus documentation files (ARCHITECTURE.md, GUIDE.md) and regenerates Dokka docs to match current code changes.
---

# Update Plugin Docs

Use this skill after making code changes that affect mechanics, enchantments, recipes, PDCs, data classes, utilities, or build instructions.

## When to Use

Load this skill when the user says something like:
- "Update the docs"
- "Regenerate documentation"
- "Keep the README/ARCHITECTURE/GUIDE in sync"
- After adding/removing mechanics, enchantments, recipes, or PDCs

## Steps

1. Inspect the current codebase to determine what has changed:
   - Read `src/IllyriaPlus.kt` for the lists of `recipes`, `mechanics`, and `enchantments`.
   - Read `src/IllyriaPlusBootstrap.kt` for registered enchantments and tags.
   - List `src/mechanics/` subdirectories, `src/enchantments/utility/`, `src/enchantments/vanilla/`, `src/recipes/vanilla/`, `src/data/`, and `src/pdcs/`.

2. Update `ARCHITECTURE.md`:
   - Ensure the project structure diagram matches the actual directories.
   - Update mechanic category lists and the total count.
   - Update the bootstrap-registered enchantment table.
   - Update the enchantment event-handler list.
   - Update the recipe list.
   - Update data classes and PDC fields as needed.
   - Ensure conventions and commands still match `AGENTS.md`.

3. Update `GUIDE.md`:
   - Update feature lists (enchantments, recipes).
   - Update version numbers to match `build.gradle.kts`.
   - Ensure the Configuration section reflects compile-time constants, not config files.

4. Regenerate Dokka HTML documentation:
   - Run `./gradlew dokkaGenerateHtml`.
   - Report any failures.

5. If public interfaces or KDoc comments were changed significantly, mention that the user should review the generated `docs/` output.

After finishing, summarize what documentation was updated and ask the user if they want to commit.
