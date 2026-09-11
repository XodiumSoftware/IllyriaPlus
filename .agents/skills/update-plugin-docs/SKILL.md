---
name: update-plugin-docs
description: Updates the project GUIDE.md to match current code changes.
---

# Update Plugin Docs

Use this skill after making code changes that affect mechanics, enchantments, recipes, PDCs, data classes, utilities, tables, or build instructions.

## When to Use

Load this skill when the user says something like:
- "Update the docs"
- "Keep the README/GUIDE in sync"
- After adding/removing mechanics, enchantments, recipes, or PDCs

## Steps

1. Inspect the current codebase to determine what has changed:
   - Read the main plugin class (e.g., `IllyriaCore/src/Plugin.kt`) for the lists of `recipes`, `mechanics`, and `enchantments`.
   - Read the plugin bootstrap class (e.g., `IllyriaCore/src/PluginBootstrap.kt`) for registered enchantments and tags.
   - List relevant source directories (`IllyriaCore/src/mechanics/`, `IllyriaCore/src/enchantments/`, `IllyriaCore/src/recipes/`, `IllyriaCore/src/data/`, `IllyriaCore/src/pdcs/`, `IllyriaCore/src/tables/`).

2. Update `GUIDE.md` or the project's main documentation file.
   - Update feature lists (enchantments, recipes).
   - Update version numbers to match `build.gradle.kts`.
   - Ensure the Configuration section reflects compile-time constants, not config files.

After finishing, summarize what documentation was updated and ask the user if they want to commit.
