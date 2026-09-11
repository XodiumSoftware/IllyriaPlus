---
name: add-mechanic
description: Scaffolds a new gameplay mechanic object in the correct mechanics/{category} package and registers it in the main plugin class.
---

# Add a Mechanic

Use this skill when the user wants to add a new gameplay mechanic to the project.

## Before Writing Code

1. Ask the user:
   - What is the mechanic name?
   - Which category fits best: `entity`, `entity/monster`, `player`, `server`, or `world`?
   - Which Bukkit event(s) should it listen to?
   - Does it need commands or permissions?
   - What settings/constants does it need?
2. If unsure about category, suggest one based on the event type (e.g. player events → `player/`, world/block events → `world/`).

## Creating the Mechanic File

1. Create `IllyriaCore/src/mechanics/{category}/<Name>Mechanic.kt` using the template below.
2. The object must be `internal object <Name>Mechanic : MechanicInterface` (or `MonsterInterface` for monster-specific mechanics).
3. Hardcode all settings as `private const val` / `private val` properties directly in the object. Do **not** create a nested `Config` object.
4. Add `@EventHandler fun on(event: <EventType>)` methods named `on(event: ...)` per project convention.
5. **Extract event bodies into private handler functions** — the `on(event)` function should be a thin delegate: `fun on(event: X) = handler(event)`. The private handler carries the full implementation and a KDoc explaining the behavior.
6. If commands are needed, override `val cmds: Collection<CommandData>` and `val perms: List<Permission>`.
7. Keep `internal` visibility for all helper functions.

## Wiring

1. Open the main plugin class (e.g., `IllyriaCore/src/Plugin.kt`).
2. Import the new mechanic.
3. Add it alphabetically to the `mechanics` list in `onEnable()`.

## Documentation

Add a concise KDoc comment to the object explaining its purpose.

## Template

```kotlin
package <project.package>.mechanics.{category}

import org.bukkit.event.EventHandler
import org.bukkit.event.<EventType>
import <project.package>.mechanics.MechanicInterface

/** Represents a mechanic handling <description> within the system. */
internal object <Name>Mechanic : MechanicInterface {
    // private const val / private val settings here

    @EventHandler(ignoreCancelled = true)
    fun on(event: <EventType>) = handleThing(event)

    /**
     * Handles <description of what the handler does>.
     *
     * @param event The [<EventType>] triggered when <when>.
     */
    private fun handleThing(event: <EventType>) {
        // implementation
    }
}
```

Replace `<project.package>` with the actual project package (e.g., `org.xodium.illyriaplus`).

After finishing, summarize the files changed and ask the user if they want to commit.
