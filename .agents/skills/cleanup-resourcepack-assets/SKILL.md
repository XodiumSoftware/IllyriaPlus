---
name: cleanup-resourcepack-assets
description: Removes unused resource pack source folders and assets after they have been migrated into the active IllyriaResourcePack directory.
---

# Cleanup Resource Pack Assets

Use this skill when the user wants to remove leftover or duplicate resource pack source files/folders after migration work.

## What this skill covers

- Deleting source-only folders that are no longer part of the shipped resource pack
- Ensuring the active pack lives under `IllyriaResourcePack/`

## Steps

1. Confirm with the user which folders are safe to delete, or verify that the same files already exist under `IllyriaResourcePack/assets/`.

2. Delete the leftover source folder(s) using the `delete_path` tool. Common examples:
   - `minecraft/` at the project root
   - Duplicate `font/` or `textures/` directories outside `IllyriaResourcePack/`

3. Verify the active resource pack still contains the needed assets:
   - `IllyriaResourcePack/assets/minecraft/font/default.json`
   - `IllyriaResourcePack/assets/illyriaplus/textures/font/`
   - `IllyriaResourcePack/assets/minecraft/lang/en_us.json`

4. Run `./gradlew shadowJar` to make sure the build is not affected.

## Notes

- The plugin serves `irp.zip` built from `IllyriaResourcePack/`, so anything outside that folder is not shipped to players unless a build step copies it.
- Always double-check before deleting; do not remove files that are still referenced by Gradle tasks or other build scripts.
