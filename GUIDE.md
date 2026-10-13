# Illyria Wiki Guide

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Writing Content](#writing-content)
- [Adding Images](#adding-images)
- [Building and Previewing](#building-and-previewing)
- [Validation](#validation)
- [Troubleshooting](#troubleshooting)
- [Reference: IllyriaPlus Plugin](#reference-illyriaplus-plugin)

---

## Prerequisites

Before editing the wiki, ensure you have:

- [mdBook](https://rust-lang.github.io/mdBook/) installed (`cargo install mdbook`)
- [Node.js](https://nodejs.org/) (for markdownlint-cli2)
- Git with SSH configured

## Quick Start

### Clone the Repository

```bash
# Clone the wiki
git clone git@github.com:XodiumSoftware/illyria-wiki.git IllyriaWiki
cd IllyriaWiki

# Clone IllyriaPlus for reference (optional, needed for wiki-sync skill)
git clone git@github.com:XodiumSoftware/IllyriaPlus.git ../IllyriaPlus
```

### Setup Dependencies

```bash
# Install all required tools
just setup
```

### Start Editing

```bash
# Serve with live-reload (opens browser automatically)
just serve
# Opens http://localhost:3000
```

## Project Structure

```
IllyriaWiki/
├── src/                    # Markdown source files
│   ├── SUMMARY.md          # Navigation structure
│   ├── introduction.md     # Homepage
│   ├── getting-started/    # New player guides
│   ├── gameplay/           # Core mechanics
│   ├── enchantments/       # Custom enchantments
│   ├── qol/                # Quality-of-life features
│   ├── recipes/            # Custom crafting recipes
│   ├── mods/               # Modded client integration
│   ├── datapacks/          # Vanilla datapacks
│   └── img/                # Images organized by type
│       ├── block/          # Block textures
│       ├── gui/            # GUI icons
│       ├── item/           # Item textures
│       ├── jade/           # Jade mod icons
│       └── method/         # Crafting station icons
├── theme/                  # Custom mdBook theme
│   └── css/                # Stylesheets
├── book/                   # Built output (generated)
└── book.toml               # mdBook configuration
```

## Writing Content

### Page Structure

All documentation pages follow this format:

```markdown
# Page Title

One-line summary of the feature or mechanic.

| Property   | Value                            |
| ---------- | -------------------------------- |
| Applies To | Pickaxes                         |
| Max Level  | 3                                |
| Source     | Enchanting table, anvil          |

## Effect

What the feature does in detail.

## Notes

Tips, warnings, or additional information.

## Related

- [Other Page](other-page.md)
```

### Adding a New Page

1. Create the file in the appropriate section:
   ```bash
   touch src/enchantments/my-enchantment.md
   ```

2. Register it in `src/SUMMARY.md`:
   ```markdown
   # Enchantments
   
   - [Overview](enchantments/index.md)
   - [My Enchantment](enchantments/my-enchantment.md)
   ```

3. Follow naming conventions:
   - Files: `kebab-case.md`
   - Images: `snake_case.png`

### Using Images

Reference images with the `mc-icon` class:

```html
<img class="mc-icon" alt="" src="../img/item/diamond.png"> Diamond
```

- Path is relative to the page location
- Always include `alt=""` for decorative icons
- The `mc-icon` class applies pixel-art styling

## Adding Images

Images come from the IllyriaResourcePack:

```bash
# Find a texture in the resource pack
find ../IllyriaPlus/IllyriaResourcePack/assets/minecraft/textures/ -name "*furnace*"

# Copy to the wiki (strip _front/_side suffixes)
cp ../IllyriaPlus/IllyriaResourcePack/assets/minecraft/textures/block/furnace_front.png \
   src/img/method/furnace.png
```

### Image Categories

| Category    | Usage                                      | Examples                          |
| ----------- | ------------------------------------------ | --------------------------------- |
| `item/`     | Item sprites                               | `diamond.png`, `enchanted_book.png` |
| `block/`    | Block textures                             | `amethyst_block.png`               |
| `gui/`      | GUI elements                               | `crafting_table_workstation.png`   |
| `method/`   | Crafting stations                          | `furnace.png`, `stonecutter.png`   |
| `jade/`     | Jade mod integration                       | `containers.png`, `growth.png`     |

## Building and Previewing

### Build the Wiki

```bash
# Build static site
just build

# Output goes to book/
```

### Serve with Live Reload

```bash
# Start development server
just serve

# Opens http://localhost:3000
# Auto-reloads when files change
```

### Build and Open

```bash
just open
```

## Validation

Always validate before committing:

### Lint Markdown

```bash
# Check for style issues
just lint

# Auto-fix issues
just lint-fix
```

### Check Links

```bash
# Verify all internal links work
just check-links
```

### Full Validation

```bash
# Lint + Build
just check
```

## Troubleshooting

### "mdbook not found"

Install mdBook:
```bash
cargo install mdbook
```

### "markdownlint-cli2 not found"

Install via npm:
```bash
npm install -g markdownlint-cli2
```

Or run setup:
```bash
just setup
```

### Build fails with "missing file"

Check that all files in `SUMMARY.md` exist:
```bash
# Find broken references
grep -r "](.*\.md)" src/SUMMARY.md | while read line; do
  file=$(echo "$line" | grep -oP '(?<=\()\S+(?=\))')
  [ ! -f "src/$file" ] && echo "Missing: $file"
done
```

### Images not showing

- Verify the path is relative to the page (`../img/...` from section pages)
- Check the file exists in `src/img/<category>/`
- Rebuild: `just clean && just build`

### Lychee link checker fails

Template variables like `{{github_repo}}` are excluded via `lychee.toml`. If you see other failures:
- Check for typos in URLs
- Verify external links are valid
- Internal links should use relative paths

## Reference: IllyriaPlus Plugin

This wiki documents the **Illyria Minecraft server** running the [IllyriaPlus](https://github.com/XodiumSoftware/IllyriaPlus) plugin.

### Mapping Plugin to Wiki

| Plugin Component                     | Wiki Section                    |
| ------------------------------------ | ------------------------------- |
| `enchantments/utility/*`             | `enchantments/`                 |
| `enchantments/vanilla/*`             | `enchantments/vanilla-tweaks.md` |
| `mechanics/player\|qol`               | `qol/`                          |
| `mechanics/world\|entity\|server`      | `gameplay/`                     |
| `recipes/vanilla/*`                  | `recipes/index.md`              |
| IllyriaBridge bridges                | `mods/`                         |

### Verifying Feature Documentation

When documenting features, always check the actual code:

```bash
# Check enchantment properties
cat ../IllyriaPlus/IllyriaCore/src/enchantments/utility/VinemineEnchantment.kt

# Check mechanic constants
grep -A 10 "private const val" ../IllyriaPlus/IllyriaCore/src/mechanics/player/MessagesMechanic.kt
```

The plugin uses compile-time constants (`private const val`), so the source code is the authoritative reference.

---

<p align="right"><a href="#top">▲</a></p>
