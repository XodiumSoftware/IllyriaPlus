# Portal Restrictions

IllyriaRPG restricts portal usage to spawn and links the spawn portals between dimensions.

## Spawn Protection

Nether portals can only be created and entered within the server's **spawn protection** radius
(configured via `spawn-protection` in `server.properties`). Portals outside this area are blocked.

Attempting to create or use a portal outside spawn protection shows:
**"Portals cannot be created, use the one at spawn instead!"**

## Spawn Portal Linking

Portals at spawn always link the Overworld and Nether world spawns instead of using vanilla's
coordinate-scaled portal generation.

- **Overworld portal at spawn** → teleports to the **Nether world spawn**
- **Nether portal at Nether world spawn** → teleports to the **Overworld world spawn**

The teleport destination Y coordinate is adjusted to the highest solid block at the spawn X/Z.

## End Portal

Exiting the End via the exit portal always teleports to the **Overworld world spawn** regardless
of the player's bed or respawn anchor location.
