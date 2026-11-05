# Mannequin TODO

## Combat

- **Focus on hostile players.** Threat scoring and focus switching are effectively `Monster`-only:
  `nearestMonster` drives ambient threat, and `isAlly` only excludes the owner. Extending this to
  hostile players needs a decision on PvP consent and on what counts as a threat, since another
  player is indistinguishable from an ally by type alone.
- **Distribute monster aggro.** `MannequinHostility` currently assigns every monster within range
  to the nearest mannequin, so all monsters pile onto the same one. Round-robin the monsters across
  the group's members so a horde is spread out.
- **Combat groups ignore each other.** Two different players' mannequin groups fighting the same
  horde have no awareness of each other; they can stack on the same slot ring. Consider merging
  slot rings when multiple groups share a focus target.
- **Ranged shield recovery.** Only `MannequinSwordCombat` re-raises the shield after a swing.
  Bow/crossbow and trident mannequins never raise their shield again after firing once.
- **Group member clustering.** Groups are built from every mannequin owned by a player that is
  within `GROUP_RADIUS` of the first one found. A player with two squads far apart gets one group
  containing only the seed's cluster; the rest form no group at all. Proper connected clustering
  would handle multiple squads per owner.
- **Cross-world groups.** Groups are per-world by construction, but the group map is keyed only by
  owner UUID, so a player with mannequins in the Nether and the Overworld only gets the last world
  processed. Key groups by `(world, owner)` instead.

## Interaction / UI

- **Show group state in the UI.** `MannequinListUI` shows combat mode but not whether the mannequin
  is engaged, defensive, and who the group's focus target is.
- **Group-wide commands.** Editing combat/movement mode only affects the clicked mannequin; squad
  management (set all to aggressive, recall all) would need a dedicated group UI.
