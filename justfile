# IllyriaWiki – justfile for common documentation tasks
# Run `just` to see all available recipes

# Default recipe (shows this list)
list:
    @just --list

# Build the wiki
build:
    mdbook build

# Build and open in browser
open: build
    xdg-open book/index.html 2>/dev/null || open book/index.html 2>/dev/null || start book/index.html

# Serve with live-reload on port 3000
serve:
    mdbook serve --open --port 3000

# Lint all Markdown files
lint:
    npx markdownlint-cli2 --config .markdownlint.json "src/**/*.md"

# Fix linting issues automatically
lint-fix:
    npx markdownlint-cli2 --config .markdownlint.json --fix "src/**/*.md"

# Run link checker (requires lychee: cargo install lychee)
check-links:
    lychee --config lychee.toml "src/**/*.md"

# Full validation: lint + build
check: lint build

# Clean build artifacts
clean:
    mdbook clean

# Setup dependencies (requires cargo and npm)
setup:
    @echo "Installing mdBook..."
    cargo install mdbook
    @echo "Installing markdownlint-cli2..."
    npm install -g markdownlint-cli2
    @echo "✅ Setup complete!"

# Clone IllyriaPlus as sibling (for wiki-sync reference)
setup-reference:
    #!/usr/bin/env sh
    if [ ! -d "../IllyriaPlus" ]; then
        git clone git@github.com:XodiumSoftware/IllyriaPlus.git ../IllyriaPlus
        echo "✅ IllyriaPlus cloned as sibling directory"
    else
        echo "ℹ️  IllyriaPlus already exists"
    fi
