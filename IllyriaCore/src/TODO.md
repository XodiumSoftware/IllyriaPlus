# IllyriaPlus — Soulslike Roadmap

This file tracks the phased plan to make IllyriaPlus more Dark Souls-like.
All changes follow the existing architecture: Kotlin `object`s implementing
`MechanicInterface`, `EnchantmentInterface`, `RecipeInterface`, or
`ItemsInterface`; compile-time constants; and Paper 26.3 APIs.

---

## Phase 1 — Death, Souls, and Stain System

- [x] Add `SoulFragmentItem` (`items/soul/SoulFragmentItem.kt`)
  - Custom item used as the Souls currency.
- [x] Add `DeathStainMechanic` (`mechanics/player/DeathStainMechanic.kt`)
  - Store XP and last death location in PDC on death.
  - Spawn a retrievable stain marker at the death location.
  - Recover lost XP/souls by interacting with the stain.
  - Overwrite old stain and lose a portion of souls on subsequent death.
- [x] Add Hollowed/Frail state on respawn
  - Apply weakness and reduced max health until stain is recovered or timer ends.
- [x] Disable natural regeneration in the wild
  - Enforce via `NaturalRegenMechanic` so healing must come from flasks/bonfires.

---

## Phase 2 — Estus-like Flask System

- [x] Add `FlaskItem` (`items/flask/FlaskItem.kt`)
  - Charges-based healing potion with custom model.
- [x] Add `BonfireMechanic` (`mechanics/world/BonfireMechanic.kt`)
  - Right-clicking a lit campfire sets respawn, heals, and refills flask charges.
  - Respawns hostile mobs in a radius when resting.
- [x] Add flask upgrade recipes
  - Increase max charges and healing potency via crafting recipes.

---

## Phase 3 — Stamina / Commitment Combat

- [ ] Add `StaminaMechanic` (`mechanics/player/StaminaMechanic.kt`)
  - Per-player stamina tracked in PDC and shown on action bar.
  - Attacking, sprinting, blocking, and jumping drain stamina.
  - Stamina regenerates quickly while idle.
- [ ] Assign stamina costs to existing weapon tiers
  - Heavy weapons (Adamantite/Draconic) cost more stamina.
  - Light weapons (Mithril/Orichalcum) cost less.
- [ ] Add dodge/roll
  - Sneak + movement input while stamina is available grants speed + i-frames.

---

## Phase 4 — Enemy and Boss Design

- [ ] Add `MobScalingMechanic` (`mechanics/entity/MobScalingMechanic.kt`)
  - Random affixes for mobs spawned in dangerous biomes/light levels.
  - Affixes: extra health, damage, speed, fire aura, etc.
  - Affixed mobs drop `SoulFragmentItem` and rare materials.
- [ ] Add `BossEmberItem` and boss summoning recipes
  - Place specific block pattern and use ember to spawn a boss.
- [ ] Add `BossMechanic` (`mechanics/entity/BossMechanic.kt`)
  - Spawn buffed boss entity with custom loot table.
  - Drop unique materials used for top-tier gear/flask upgrades.

---

## Phase 5 — Gear Rebalance (Trade-offs, Not Linear Upgrades)

- [ ] Add set bonuses when wearing 2–4 matching armor pieces.
- [ ] Add weapon skills / charged attacks on right-click for custom weapons.

---

## Phase 6 — Multiplayer Tension (Optional, Later)

- [ ] Summon signs for co-op.
- [ ] Opt-in invasions/dueling.
- [ ] Lightweight covenants with passives and rivalries.

---

## Phase 7 — Atmosphere and UX

- [ ] Override death screen with “YOU DIED” style text and sound.
- [ ] Bonfire rest visuals and sound.
- [ ] Fog walls / boss arena entry effects.
- [ ] Resource pack assets for new items, GUI, and sounds.
- [ ] Update `GUIDE.md` and `illyria-wiki` pages.

---

## In Progress

_None._

## Done

- Removed old linear-tier custom gear (Adamantite/Draconic/Mithril/Orichalcum).
- Cleaned up resource pack `items/` and `models/item/` definitions for the removed tiers.
- Phase 1 — Death Stain / Souls system implemented.
- Phase 2 — Estus Flask / Bonfire system implemented.
- `GUIDE.md` and `illyria-wiki` pages **not yet updated**.
