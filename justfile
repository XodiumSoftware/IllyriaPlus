# IllyriaPlus – justfile for common plugin tasks
# Run `just` to see all available recipes
#
# Cross-platform: `just` uses sh on Unix and PowerShell on Windows.

[windows]
set shell := ["powershell.exe", "-NoProfile", "-Command"]

# Default recipe (shows this list)
_default:
    @just --list

# Alias kept for muscle memory
alias list := _default

# Build all plugin JARs
[unix]
build:
    ./gradlew shadowJar

# Build all plugin JARs
[windows]
build:
    ./gradlew.bat shadowJar

# Run IllyriaCore test server
[unix]
run *args:
    ./gradlew :IllyriaCore:runServer {{ args }}

# Run IllyriaCore test server
[windows]
run *args:
    ./gradlew.bat :IllyriaCore:runServer {{ args }}

# Run IllyriaBridge test server
[unix]
run-bridge *args:
    ./gradlew :IllyriaBridge:runServer {{ args }}

# Run IllyriaBridge test server
[windows]
run-bridge *args:
    ./gradlew.bat :IllyriaBridge:runServer {{ args }}

# Run linting
[unix]
lint:
    ./gradlew ktlintCheck

# Run linting
[windows]
lint:
    ./gradlew.bat ktlintCheck

# Fix linting issues
[unix]
format:
    ./gradlew ktlintFormat

# Fix linting issues
[windows]
format:
    ./gradlew.bat ktlintFormat

# Build and run IllyriaCore test server
dev *args: build (run args)
