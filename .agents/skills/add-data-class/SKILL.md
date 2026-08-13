---
name: add-data-class
description: Scaffolds a new data class in src/data/ for structured immutable data used across the project.
---

# Add a Data Class

Use this skill when the user needs a new structured data type shared across the project (mechanics, enchantments, recipes, utilities, database tables, etc.).
Use this skill when the user needs a new structured data type shared across the project (mechanics, enchantments, recipes, utilities, database tables, etc.).

## Before Writing Code

1. Ask the user:
   - What is the data class name?
   - What properties should it have, and what are their types?
   - Is there a sensible default value for any property?
   - Should it include a companion object factory function or helper method?
   - Which mechanic(s) will use it?

## Creating the Data Class

1. Create `src/data/<Name>.kt`.
2. Define `internal data class <Name>(...)` with immutable `val` properties.
3. Keep the class focused on one responsibility.
4. Add a concise KDoc comment explaining the purpose of the data class.
5. If the data class needs construction helpers, add a `companion object` with factory functions or extension functions in the file.

## Conventions

- Use `val` only — no mutable `var` properties in data classes unless there is a strong reason.
- Prefer primitive or existing platform/API types.
- If the data class is used for lookups, consider adding a helper that converts a collection into a map (see `BuildSetupData.toMaterialMap()`).

## Documentation

1. Add the new data class to the Data Classes section of `ARCHITECTURE.md`.
2. If it is used across multiple mechanics, mention which ones.
3. If public API shape changes, run `./gradlew dokkaGenerateHtml`.

## Template

```kotlin
package <project.package>.data

/**
 * Represents <description>.
 *
 * @property <name> <description>
 */
internal data class <Name>(
    val <property1>: <Type1>,
    val <property2>: <Type2>,
)
```

Replace `<project.package>` with the actual project package (e.g., `org.xodium.illyriaplus`).

Replace `<project.package>` with the actual project package (e.g., `org.xodium.illyriaplus` or `org.xodium.illyriakingdoms`).

After finishing, summarize the files changed and ask the user if they want to commit.
