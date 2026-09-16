# Jade

The server supports **Jade** (the modern WAILA fork), running it in server-connected mode so block and
entity tooltips show live server data.

## Features

| Block or entity | Live data shown                                |
| :-------------: | ---------------------------------------------- |
| Any block/entity | Name and mod origin (client-side overlay)     |
| <img class="mc-icon" alt="Furnace / Blast Furnace / Smoker" title="Furnace / Blast Furnace / Smoker" src="../img/jade/furnaces.webp"> | Smelt progress arrow with input/fuel/result    |
| <img class="mc-icon" alt="Brewing stand" title="Brewing stand" src="../img/jade/brewing_stand.png"> | Blaze-powder fuel left & brew-time countdown   |
| <img class="mc-icon" alt="Beehive" title="Beehive" src="../img/jade/beehive.png"> | Bee count (green when full) + honey level      |
| <img class="mc-icon" alt="Hopper" title="Hopper" src="../img/jade/hopper.png"> | “Locked” indicator when disabled by redstone   |
| <img class="mc-icon" alt="Redstone" title="Redstone" src="../img/jade/redstone.png"> | Output signal strength (power level)           |
| <img class="mc-icon" alt="Lectern" title="Lectern" src="../img/jade/lectern.webp"> | Name of the book placed on it                  |
| <img class="mc-icon" alt="Jukebox" title="Jukebox" src="../img/jade/jukebox.png"> | Currently playing record’s song                |
| <img class="mc-icon" alt="Command Block" title="Command Block" src="../img/jade/command_block.png"> | Stored command (operators only)                |
| <img class="mc-icon" alt="Chiseled Bookshelf" title="Chiseled Bookshelf" src="../img/jade/chiseled_bookshelf.png"> | Name of the book in the hovered slot           |
| <img class="mc-icon" alt="Campfire" title="Campfire" src="../img/jade/campfire.png"> | Cooking items with per-item countdown          |
| <img class="mc-icon" alt="Containers" title="Containers" src="../img/jade/containers.png"> | Contents of chests, barrels, shulkers & more   |
| <img class="mc-icon" alt="Trial Spawner" title="Trial Spawner" src="../img/jade/trial_spawner.png"> | Remaining cooldown time after a trial          |
| <img class="mc-icon" alt="Health" title="Health" src="../img/jade/heart.png"> | Hearts and armor when hovering a living entity |
| <img class="mc-icon" alt="Mob Growth" title="Mob Growth" src="../img/jade/growth.png"> | Time until a baby mob grows up                 |
| <img class="mc-icon" alt="Mob Breeding" title="Mob Breeding" src="../img/jade/breeding.png"> | Breeding cooldown, or "fed" when in love mode  |
| <img class="mc-icon" alt="Animal Owner" title="Animal Owner" src="../img/jade/animal_owner.png"> | Tamed animal's owner name                      |
| <img class="mc-icon" alt="Status Effects" title="Status Effects" src="../img/jade/status_effects.png"> | Active potion effects with durations           |
| <img class="mc-icon" alt="Copper Golem" title="Copper Golem" src="../img/jade/waxed.png"> | Waxed status (honeycomb icon on a waxed golem) |
| <img class="mc-icon" alt="Zombie Villager" title="Zombie Villager" src="../img/jade/zombie_villager.png"> | Cure conversion time while being healed        |
| <img class="mc-icon" alt="Chicken" title="Chicken" src="../img/jade/next_drop.png"> | Time until the next egg is laid                |

## Setup

1. Install [Jade](https://modrinth.com/mod/jade) on your client
2. Join the server — the mod is automatically detected
3. Hover a block or entity to see live server data in the tooltip

## Notes

- Vanilla clients are completely unaffected
- Server data synchronization happens automatically on join
- Only the features listed above are synced by the server; everything else renders client-side as usual
