# JEI, REI & EMI

Custom recipes are visible in all three major recipe viewers — **JEI** (Just Enough Items), **REI**
(Roughly Enough Items), and **EMI**.

## Features

- See custom weapon recipes (Greatsword, Halberd, Longsword) directly in your recipe viewer
- Browse all custom vanilla-style recipes (chainmail, wool-to-string, etc.)
- Look up recipes and item usages as usual

## Setup

1. Install a mod loader (**Fabric** or **NeoForge**)
2. Install one of the recipe viewers:
   - [JEI](https://modrinth.com/mod/jei)
   - [REI](https://modrinth.com/mod/rei)
   - [EMI](https://modrinth.com/mod/emi)
3. Join the server — supported clients are detected automatically

## How it works

- **REI and EMI** read custom recipes from the standard Minecraft recipe sync that the server already
  sends on join, so they work out of the box.
- **JEI** bypasses the vanilla recipe system on Fabric/NeoForge, so the server additionally pushes the
  recipes to JEI clients through a dedicated channel.

In both cases, custom recipes appear automatically — no extra steps needed.

## Notes

- Vanilla clients are completely unaffected
- Recipe sync happens automatically on every join
