---
name: add-pdc
description: Adds a new player Persistent Data Container field to PlayerPDC.kt with the correct NamespacedKey and PersistentDataType.
---

# Add a Player PDC Field

Use this skill when the user wants to store new per-player persistent data in a Paper-based project.

## Before Writing Code

1. Ask the user:
   - What is the property name? (e.g. `lastLogin`)
   - What type should it store? (String, Boolean, Int, Double, Long, or a custom serializable type)
   - What should the default value be?
   - Should it be nullable or have a default fallback?

## Adding the Field

1. Open `src/pdcs/PlayerPDC.kt`.
2. Add a new private `NamespacedKey` using `NamespacedKey(instance, "snake_case_key_name")`.
3. Add a `var Player.<propertyName>` extension property using `persistentDataContainer` and the appropriate `PersistentDataType`.
4. Keep the visibility `internal` and match the style of existing properties (`nickname`, `scoreboardVisibility`).
5. If the value is complex, add a custom `PersistentDataType` or use JSON serialization; otherwise prefer primitive types.

## Documentation

1. Add KDoc to the new property explaining its purpose and default behavior.
2. If the field is used by multiple mechanics or commands, document it in `ARCHITECTURE.md` under the PDC section.

## Example

```kotlin
/** The [NamespacedKey] used for storing <description>. */
private val <PROPERTY>_KEY = NamespacedKey(instance, "<key_name>")

/**
 * Gets or sets a [Player]'s <property> in their persistent data container.
 *
 * @return <default/fallback description>.
 */
var Player.<propertyName>: <Type>
    get() = persistentDataContainer.getOrDefault(<PROPERTY>_KEY, PersistentDataType.<TYPE>, <default>)
    set(value) = persistentDataContainer.set(<PROPERTY>_KEY, PersistentDataType.<TYPE>, value)
```

After finishing, summarize the files changed and ask the user if they want to commit.
