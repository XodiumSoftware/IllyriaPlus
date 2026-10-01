# Portal Restrictions

Illyria restricts Overworld portal usage to the spawn protection area, while Nether portals can
be created anywhere but always lead back to the Overworld spawn. Additionally, dimension travel is
gated behind special key items.

## Dimension Keys

Traveling to certain dimensions requires holding a special key item in your inventory:

- **Nether** requires the **Incendium Key**
- **The End** requires the **Nullscape Key**

Stepping into a portal without its key instantly rejects you: an on-screen warning appears, you
are pushed back out of the portal, and a burst of portal particles plays. One key is **consumed**
per successful trip to the dimension — returning to the Overworld does not consume additional
keys. Players in creative or spectator mode are exempt from the key requirement and do not consume
keys.

Keys are found as rare loot in **Trial Chambers** vaults (both regular and ominous).

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

Your Nether-side portal is remembered: the next time you enter a Nether portal from the
Overworld, you return to that same portal instead of a freshly generated one. If the portal has
since been destroyed, a new one is generated normally.

The teleport destination Y coordinate is adjusted to the highest solid block at the spawn X/Z.
