# Anvil Improvements

Significant quality-of-life changes to anvils, including disenchanting and cost improvements.

## Key Features

| Feature             | Description                                             |
| ------------------- | ------------------------------------------------------- |
| No "Too Expensive!" | Cost cap removed — repair/enchant anything               |
| Level Cap Bypass    | Exceed normal enchantment level limits                   |
| Disenchant Item     | Extract enchantments back onto books                     |
| MiniMessage Names   | Use formatting codes when renaming items                 |

---

## Removed Cost Limit

The vanilla 40-level anvil cap ("Too Expensive!") is removed. You can always combine and repair items regardless of
cost — as long as you have the levels.

Operations that would normally cost 40+ levels are allowed and will deduct the full level cost.

---

## Enchantment Level Cap Bypass

The vanilla limit on enchantment levels is bypassed on anvils, allowing you to combine enchantments beyond their
normal maximum levels where the game otherwise permits it.

---

## Disenchanting

Extract enchantments from an item back onto an enchanted book.

### How to Disenchant

1. Place the **enchanted item** (or **Enchanted Book**) in the first anvil slot
2. Place a **regular Book** in the second slot
3. The result slot will show an **Enchanted Book** with the extracted enchantments
4. Take the result to complete the disenchantment

### Rules

- The item must have **2 or more** non-curse enchantments (single-enchantment items cannot be disenchanted)
- **Curses** (Binding, Vanishing) are never extracted
- When disenchanting an **Enchanted Book** with multiple enchantments, only **one** enchantment is split off per book
- Extracted enchantments are removed from the source item
- The book is consumed

### XP Cost

The level cost scales with the number and level of enchantments extracted. Higher levels cost more.

---

## MiniMessage Renaming

When renaming items in an anvil, you can use **MiniMessage** formatting syntax for styled item names.

### Examples

| Input                              | Result                     |
| ---------------------------------- | -------------------------- |
| `<red>Flaming Sword</red>`           | Red, italic-free name      |
| `<gradient:#FF0000:#0000FF>Blade</gradient>` | Gradient-colored name  |
| `<bold><gold>Excalibur</gold></bold>` | Bold, gold name         |

> See the [MiniMessage docs](https://docs.papermc.io/adventure/minimessage/format/) for the full formatting syntax.
