set shell := ["sh", "-c"]

# Build all plugin JARs
build:
    ./gradlew shadowJar

# Run IllyriaCore test server
run *args:
    ./gradlew :IllyriaCore:runServer {{ args }}

# Run IllyriaBridge test server
run-bridge *args:
    ./gradlew :IllyriaBridge:runServer {{ args }}

# Run linting
lint:
    ./gradlew ktlintCheck

# Fix linting issues
format:
    ./gradlew ktlintFormat

# Build and run IllyriaCore test server
dev *args: build (run args)
