# Mannequins

Spawn companion mannequins that track nearby entities with their heads, follow you like a pet, fight your battles, and block with a shield.

## Spawning

Use the `/mannequin` command (operator-only) to spawn a mannequin at your location. You become its owner.

## Editing

**Right-click** a mannequin you own to open the edit dialog, where you can change:

| Setting     | Description                                                                 |
| ----------- | --------------------------------------------------------------------------- |
| Skin        | Player name whose skin the mannequin wears (leave blank for default)        |
| Name        | Custom display name shown above its head                                    |
| Description | Text shown below the name                                                   |
| Follow      | Whether the mannequin follows you (see below)                               |
| Combat Mode | How it reacts to nearby monsters: Aggressive, Defensive (default), Fleeing  |
| Main Hand   | Which hand is dominant (left or right)                                      |

## Equipment

**Shift + right-click** a mannequin to equip or swap items. The slot is determined by where you click:

| Click Location              | Slot  | Accepts              |
| --------------------------- | ----- | -------------------- |
| Head region                 | Head  | Helmets, any block   |
| Chest region                | Chest | Chestplates          |
| Legs region                 | Legs  | Leggings             |
| Feet region                 | Feet  | Boots                |
| Left arm (offhand side)     | Hand  | Any item             |
| Right arm (main hand side)  | Hand  | Any item             |

- Shift-right-click with an **item** equips it, or swaps with what's already in that slot.
- Shift-right-click with an **empty hand** takes the item from the slot.

## Behaviors

### Head Tracking

Mannequins turn their heads to look at the nearest living entity — players, animals, monsters — within 8 blocks.

### Following

When **Follow** is enabled, the mannequin walks behind you at walking speed:

- Stops when within 2 blocks of you.
- Teleports to you when you stray more than 12 blocks away (or switch dimensions), landing on the nearest ground — never mid-air.
- Pauses while fighting or defending (Combat Mode) and resumes afterwards.

### Combat

Mannequins fight back when provoked and defend their owner. Their **Combat Mode** decides how they react to nearby monsters:

- **Aggressive** — the mannequin attacks monsters that come within 12 blocks, without waiting to be hit.
- **Defensive** *(default)* — the mannequin stops moving and raises its shield when a monster is within 12 blocks, then fights back only once the monster strikes it. Blocked hits wear the shield down until it breaks with the usual break sound.
- **Fleeing** — the mannequin ignores monsters completely and keeps following you instead of engaging.

In combat a mannequin chases its target at sprinting speed and swings its held weapon (damage matches the weapon, e.g. a netherite sword hits for 8).

Regardless of combat mode (except Fleeing), mannequins also:

- **Retaliate** — anything that attacks the mannequin becomes its target.
- **Assist** — when you attack something, your mannequins join the fight.
- **Defend** — when something attacks you, your mannequins attack the aggressor.

The fight ends when the target dies, escapes more than 30 blocks away, or changes dimensions. Following pauses while fighting and resumes afterwards.

## Notes

- Monsters treat mannequins as valid targets — zombies, skeletons, and creepers will detect and attack them, respecting vanilla daylight rules (e.g. spiders stay passive in daylight).
- Only the owner can edit a mannequin or swap its equipment.
