# Portal Restrictions

IllyriaRPG restricts Overworld portal usage to the spawn protection area, while Nether portals can
be created anywhere but always lead back to the Overworld spawn. Additionally, dimension travel is
gated behind special key items.

## Dimension Keys

Traveling to certain dimensions requires holding a special key item in your inventory:

- **Nether** requires the **Incendium Key**
- **The End** requires the **Nullscape Key**

Attempting to enter a dimension without its key displays an error message and blocks the teleport.
One key is **consumed** per successful trip to the dimension — returning to the Overworld does not
consume additional keys. Players in creative or spectator mode are exempt from the key requirement
and do not consume keys.

Keys are craftable using Overworld materials (see [Recipes](../recipes/index.md)).

## Spawn Protection (Overworld)

Nether portals in the **Overworld** can only be created and entered within the server's
**spawn protection** radius (configured via `spawn-protection` in `server.properties`). Portals
outside this area are blocked.

Attempting to create or use a portal outside spawn protection shows:
**"Portals cannot be created, use the one at spawn instead!"**

The Overworld spawn portal teleports normally, using vanilla's portal generation and linking.

## Nether Portals

Portals in the **Nether** can be created and used **anywhere** — spawn protection does not apply.
However, instead of generating a linked portal at scaled coordinates, entering a Nether portal
teleports the player directly to the **Overworld world spawn** without creating or searching for
an Overworld portal.

The teleport destination Y coordinate is adjusted to the highest solid block at the spawn X/Z.
