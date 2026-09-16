# Condensing

Quickly convert raw materials into their block form — and back.

## Usage

| Command       | Description                                          |
| ------------- | ---------------------------------------------------- |
| `/condense`   | Condenses all applicable items into blocks           |
| `/uncondense` | Splits blocks back into their base items             |

Both commands apply to your **entire inventory**.

## Supported Conversions

<div class="icon-table">

| Base Item                                                                                              | Block                                                                                              |
| :----------------------------------------------------------------------------------------------------: | :------------------------------------------------------------------------------------------------: |
| <img class="mc-icon" alt="Amethyst Shard" title="Amethyst Shard" src="../img/item/amethyst_shard.png">                | <img class="mc-icon" alt="Amethyst Block" title="Amethyst Block" src="../img/block/amethyst_block.png">                |
| <img class="mc-icon" alt="Bone Meal" title="Bone Meal" src="../img/item/bone_meal.png">                                  | <img class="mc-icon" alt="Bone Block" title="Bone Block" src="../img/block/bone_block.png">                                  |
| <img class="mc-icon" alt="Coal" title="Coal" src="../img/item/coal.png">                                              | <img class="mc-icon" alt="Coal Block" title="Coal Block" src="../img/block/coal_block.png">                                  |
| <img class="mc-icon" alt="Copper Ingot" title="Copper Ingot" src="../img/item/copper_ingot.png">                        | <img class="mc-icon" alt="Copper Block" title="Copper Block" src="../img/block/copper_block.png">                              |
| <img class="mc-icon" alt="Diamond" title="Diamond" src="../img/item/diamond.png">                                      | <img class="mc-icon" alt="Diamond Block" title="Diamond Block" src="../img/block/diamond_block.png">                            |
| <img class="mc-icon" alt="Dried Kelp" title="Dried Kelp" src="../img/item/dried_kelp.png">                              | <img class="mc-icon" alt="Dried Kelp Block" title="Dried Kelp Block" src="../img/block/dried_kelp_block.png">                    |
| <img class="mc-icon" alt="Emerald" title="Emerald" src="../img/item/emerald.png">                                      | <img class="mc-icon" alt="Emerald Block" title="Emerald Block" src="../img/block/emerald_block.png">                            |
| <img class="mc-icon" alt="Gold Ingot" title="Gold Ingot" src="../img/item/gold_ingot.png">                              | <img class="mc-icon" alt="Gold Block" title="Gold Block" src="../img/block/gold_block.png">                                  |
| <img class="mc-icon" alt="Gold Nugget" title="Gold Nugget" src="../img/item/gold_nugget.png">                            | <img class="mc-icon" alt="Gold Ingot" title="Gold Ingot" src="../img/item/gold_ingot.png">                                  |
| <img class="mc-icon" alt="Iron Ingot" title="Iron Ingot" src="../img/item/iron_ingot.png">                              | <img class="mc-icon" alt="Iron Block" title="Iron Block" src="../img/block/iron_block.png">                                  |
| <img class="mc-icon" alt="Iron Nugget" title="Iron Nugget" src="../img/item/iron_nugget.png">                            | <img class="mc-icon" alt="Iron Ingot" title="Iron Ingot" src="../img/item/iron_ingot.png">                                  |
| <img class="mc-icon" alt="Lapis Lazuli" title="Lapis Lazuli" src="../img/item/lapis_lazuli.png">                        | <img class="mc-icon" alt="Lapis Block" title="Lapis Block" src="../img/block/lapis_block.png">                                |
| <img class="mc-icon" alt="Melon Slice" title="Melon Slice" src="../img/item/melon_slice.png">                            | <img class="mc-icon" alt="Melon" title="Melon" src="../img/block/melon.png">                                          |
| <img class="mc-icon" alt="Nether Wart" title="Nether Wart" src="../img/item/nether_wart.png">                            | <img class="mc-icon" alt="Nether Wart Block" title="Nether Wart Block" src="../img/block/nether_wart_block.png">                  |
| <img class="mc-icon" alt="Netherite Ingot" title="Netherite Ingot" src="../img/item/netherite_ingot.png">                | <img class="mc-icon" alt="Netherite Block" title="Netherite Block" src="../img/block/netherite_block.png">                        |
| <img class="mc-icon" alt="Quartz" title="Quartz" src="../img/item/quartz.png">                                         | <img class="mc-icon" alt="Quartz Block" title="Quartz Block" src="../img/block/quartz_block.png">                              |
| <img class="mc-icon" alt="Redstone" title="Redstone" src="../img/item/redstone.png">                                     | <img class="mc-icon" alt="Redstone Block" title="Redstone Block" src="../img/block/redstone_block.png">                        |
| <img class="mc-icon" alt="Slime Ball" title="Slime Ball" src="../img/item/slime_ball.png">                              | <img class="mc-icon" alt="Slime Block" title="Slime Block" src="../img/block/slime_block.png">                                |
| <img class="mc-icon" alt="Wheat" title="Wheat" src="../img/item/wheat.png">                                            | <img class="mc-icon" alt="Hay Block" title="Hay Block" src="../img/block/hay_block.png">                                      |

</div>

## Notes

- Conversion ratio is always **9 base items → 1 block**
- Overflows (when your inventory can't fit the output blocks) are dropped at your feet
- Nether wart, quartz, and melon have no vanilla reverse-recipe — `/uncondense` is the only way to split them
