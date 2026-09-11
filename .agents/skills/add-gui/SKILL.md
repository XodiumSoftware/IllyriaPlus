---
name: add-gui
description: Adds a new custom GUI to the project using the InvUI framework with the project's Kotlin DSL patterns.
---

# Add a GUI

Use this skill when the user wants to add a new inventory GUI to the project.

## Before Writing Code

Ask the user:

- What is the GUI name?
- What is the title (MiniMessage-formatted)?
- What layout/design does it need (static items, paged items, freeform, etc.)?
- Which mechanic or feature will trigger/open this GUI?
- Does it need to accept item input (like a deposit/withdraw flow) or is it read-only display?
- Does it need any action handlers, callbacks, or live refresh behavior?

## Creating the GUI File

1. Create `IllyriaCore/src/gui/<Name>Gui.kt`.
2. Use `internal object <Name>Gui : ...` with `@OptIn(ExperimentalDslApi::class)`.
3. Hardcode UI strings as `private const val` properties at the top.
4. Define reusable border/decoration items as `private val`.
5. Use the `window(player)` DSL from `xyz.xenondevs.invui.dsl`.
6. Use `mutableProvider(emptyList<Item>())` (or equivalent) + `pagedItemsGui` / etc. for content areas.
7. Use `Item` for clickable elements, with `.onClick` handlers via Lambdas.
8. Use `MM.deserialize(...)` for all MiniMessage text.
9. Track open windows in a `mutableMapOf<Window, () -> Unit>()` for live refresh.
10. Provide `closeAll()` that closes all tracked windows safely.
11. Add an `open(...)` entry point function.

## Conventions

- **DSL style**: Use `window(player) { title by ...; upperGui by ... }`
- **Paged content**: Use `pagedItemsGui(structure)` with `#` border chars, `x` content slots, `<`/`>` navigation, and `Markers.CONTENT_LIST_SLOT_HORIZONTAL`
- **Item click handlers**: Use `BoundItem.pagedBuilder()` for prev/next buttons, and `item { itemProvider by ...; onClick { ... } }` for content items
- **Window lifecycle**: `addOpenHandler` and `addCloseHandler` to register/unregister for live refresh
- **VirtualInventory**: Use for deposit/withdraw flows (`VirtualInventory(size)` for `upperGui`)
- **Deferred reopen**: For "return to previous window" flows, use `player.scheduler.runDelayed(instance, { window.open() }, null, 1L)` because opening a window during the close handler is not allowed
- **Safe close**: Wrap `window.close()` in `runCatching` to isolate failures

## Template

The project has exactly one GUI example: `IllyriaCore/src/gui/WanderingTraderGui.kt`. Use it as the concrete implementation reference when building new GUIs.

- `window(player)` + `pagedItemsGui` with layout string DSL
- `BoundItem.pagedBuilder()` for previous/next page buttons
- `MutableListProvider<Item>` for dynamically refreshable content
- `VirtualInventory` for deposit/accept input flows
- Multi-window navigation with deferred reopen
- `closeAll()` cleanup

## Wiring

1. If used by a mechanic, call `<Name>Gui.open(...)` from the mechanic's event handler.
2. If the GUI should be cleaned up on plugin disable, call `<Name>Gui.closeAll()` in `onDisable` of the owning mechanic.
