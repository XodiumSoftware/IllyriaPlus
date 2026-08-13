---
name: add-utils
description: Adds a new utility function or nested utility object inside src/Utils.kt following existing conventions.
---

# Add Utilities

Use this skill when the user needs a new helper function or group of helpers for the project.

## Before Writing Code

1. Ask the user:
   - What should the utility do?
   - Is it a single function or a group of related functions?
   - Which existing type does it extend, if any?
   - Which mechanics or enchantments will use it?

## Adding the Utility

1. Open `src/Utils.kt`.
2. Decide where the utility belongs:
   - If it extends an existing type (e.g. `Player`, `Block`, `String`), prefer a top-level extension function.
   - If it is a group of related helpers, add a new nested `object` inside `Utils`.
3. Keep visibility `internal`.
4. Use the project's text formatting helper (e.g., `Utils.MM`) for any MiniMessage formatting.
5. Follow the project's member ordering: `const val`, `val`, `var`, `fun`.
6. Add `@Suppress("UnstableApiUsage")` if using experimental Paper APIs.

## Conventions

- Prefer extension functions over standalone helpers.
- Avoid creating a new utility object for a single function unless it clearly belongs to a broader domain.
- Use descriptive names; for extension functions, the receiver type is implicit context.

## Documentation

1. Add KDoc for complex utilities explaining parameters, return values, and any side effects.
2. Update the Utilities section of `ARCHITECTURE.md` if adding a new nested `object` or if the utility is used across multiple modules.
3. If the utility changes public API shape, run `./gradlew dokkaGenerateHtml`.

## Example

```kotlin
/** <Description>. */
internal fun <ReceiverType>.<functionName>(): <ReturnType> {
    // implementation
}
```

After finishing, summarize the files changed and ask the user if they want to commit.
