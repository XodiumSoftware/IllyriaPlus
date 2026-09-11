package org.xodium.illyriaplus.data

import java.util.Base64

/**
 * Represents a custom flower pot variant obtainable via stonecutting.
 *
 * @property key The unique identifier for this variant.
 * @property lore The description displayed in the item's lore.
 * @property textureHash The Minecraft texture hash for the player head skin.
 */
internal data class PotData(
    val key: String,
    val lore: String,
    val textureHash: String,
) {
    /** The full Base64-encoded texture JSON string. */
    val texture: String
        get() =
            Base64.getEncoder().encodeToString(
                "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/$textureHash\"}}}"
                    .toByteArray(),
            )

    companion object {
        /** All custom pot variants available via stonecutting. */
        val VARIANTS =
            listOf(
// Glazed Terracotta Flower Pots
                PotData(
                    "1_blue",
                    "Blue",
                    "36352109c9b9317b9a5194b776ffdedec07fd06860fabdb410a2a131e2f72fe5",
                ),
                PotData(
                    "1_green",
                    "Green",
                    "6ac742902b93b7de898ddcb445e8199c906e2aa6a3c84326bb4707a0abcf48ae",
                ),
                PotData(
                    "1_purple",
                    "Purple",
                    "654f73ea0c448e8480673083f53949fd6d33efc2d22c21c9927904c4103ec05a",
                ),
                PotData(
                    "1_red",
                    "Red",
                    "f3ccb2c66f500abcef33a81498012826d5e366ef4fde0880260eff3c905b4e61",
                ),
                PotData(
                    "1_terracotta",
                    "Terracotta",
                    "43dac8bc0da4eecabace0ede5fa6ab7573f2b6209b5ac88ac50943e57d8b170a",
                ),
// Themed Flower Pots
                PotData(
                    "2_dark",
                    "Dark",
                    "68c600736ed9021a7fe722988d0a418177a2d230bef4380e3016e575c7f7dac6",
                ),
                PotData(
                    "2_light",
                    "Light",
                    "1b2f6faf1b99ac38d52b1c7bc49eb791c67054337d44d83d66adecb50f375",
                ),
                PotData(
                    "2_modern",
                    "Modern",
                    "91c9efabd99832e9837beebfb6cd5555805f02fe7e98b4d868096417778b8b04",
                ),
                PotData(
                    "2_n01_cup",
                    "Cup",
                    "ceddeccb73e3794b97011fc0f64311add0ff3f0014b2399b3a4608163b6053be",
                ),
                PotData(
                    "2_n02_vase",
                    "Pink Vase",
                    "4181f9f7c985476b997a1eb67d898f18eafb7c19505029ce1bd5272c1d31",
                ),
                PotData(
                    "2_n03_urn",
                    "Cursed",
                    "d4e8e8e821853efea18ce956804a6f8447273b755b7660493c30afbc3323c1",
                ),
                PotData(
                    "2_n04_luxury",
                    "Luxury",
                    "ee6e451d45dccd5aa5d068e80acf64567d5ad1e77ede912c0f4b05f91e1f675f",
                ),
                PotData(
                    "2_n05_aztec",
                    "Bright",
                    "8d639fd2bd65526335a309f2c268243aa5162d3aa7d1684b26fa312123f9796d",
                ),
                PotData(
                    "2_n06_waves",
                    "Bright Blue",
                    "bcdb2f398303025424bfb2db0e2cc053c127878e573fcc37c800e5839a343fee",
                ),
                PotData(
                    "2_n07_sage",
                    "Sage",
                    "67f4cfd173f987df5c7a6259ab7c3f4db96a42aab20460dc91d103c2616cd207",
                ),
                PotData(
                    "2_n08_olive",
                    "Olive",
                    "4bbbb4b40e93cbdc2c29622e3e6797f9ea05388db5f700e06589627d0282d850",
                ),
                PotData(
                    "2_n09_undecorated",
                    "Undecorated",
                    "b03b92e7e2c07c93288eea3531275dd3674a800b573e172de5e4e48de088be3a",
                ),
                PotData(
                    "2_n10_pottery",
                    "Trimmed",
                    "a6257e5a054262a8193c06a87f609f401c01e21bb80209698cbedef486821a9b",
                ),
                PotData(
                    "2_n11_heart",
                    "Heart",
                    "2c24ab92ba9d9235afec046e3b1ade19097b788f764e974386fbc32f94c6db4f",
                ),
                PotData(
                    "2_n12_prize",
                    "Prize",
                    "85e49a6f82016a17f771c43a7053b853e7a5f4e1db3179068e3745f11b8656d3",
                ),
                PotData(
                    "2_n13_basin",
                    "Basin",
                    "a21bfd055236d6d2c764c1f9ba8267e19ce453643e15f791dcd3272d34d66175",
                ),
                PotData(
                    "2_n14_boot",
                    "Boot",
                    "e5ff921d1c2ad5f8a8c2756e2dc14d4bd0868d50535202318e04bdf098dbbd6a",
                ),
                PotData(
                    "2_n15_classic",
                    "Fresh",
                    "e3a6d43c5e2ed86ea8e1b4e3dff5951abc64bcd34d785eb5862c504a2e6b8bcb",
                ),
                PotData(
                    "2_n16_totem",
                    "Totem",
                    "6a77aebe68ecc78cdfffaeffc04fa97e2339941e3aff41cdaa1ebc5ba44628a1",
                ),
// Pattern Flower Pots
                PotData(
                    "2a_cleanred",
                    "Clean Red",
                    "cac6a1415a9c73a4a74c17150b4227a898ac95e48a5e29f9240c12440463a0b7",
                ),
                PotData(
                    "2a_cleanwhite",
                    "Clean White",
                    "539ecec3112b4f542a2eb21f8917c235a76ae0b4b34b8eb7e8339298ed1b6291",
                ),
                PotData(
                    "2a_decorated",
                    "Decorated",
                    "7eba2978dc8187f5f6f5b35e317edb9d8d5e749c6394f39bb7e3b80f25e5f436",
                ),
                PotData(
                    "2a_petals",
                    "Petals",
                    "b83ccd1e501fa9bef499a43788dd7b40ece7472e318e20676bca3406a1a4c6e8",
                ),
// Nature Flower Pots
                PotData(
                    "3_azalea",
                    "Azalea",
                    "4ef99d9ec4db4f188093742fed90051bedc16f3acb4586e98d609aa239b705a2",
                ),
                PotData(
                    "3_bloom",
                    "Bloom",
                    "084a742d8884a216aeaba45a96fae541e329162bc40893a63bfbdccd775bf",
                ),
                PotData(
                    "3_cauldron",
                    "Cauldron",
                    "9fafb14949946de823849468bab7def22422b2e0813df45ff0e864271ab72c3b",
                ),
                PotData(
                    "3_cloud",
                    "Wave",
                    "c0b76987e4246f8c7a50e2c1ac2b7e64e6a483dea0ea6da9d4d23c2e61cea7e",
                ),
                PotData(
                    "3_composter",
                    "Composter",
                    "d56e791e2f97370b9f891bcc0f966e3eb29f38a37c702b8442aeb60e504ba397",
                ),
                PotData(
                    "3_glass",
                    "Glass",
                    "3a726a3f4056dc29bd4cd2904d6ad4cf7e251e86f852e3e20548d78ed8d7f",
                ),
                PotData(
                    "3_paper",
                    "Paper",
                    "4b129035004e74fea39d16077db910737dad4deb862d967dacb4682e3aeea822",
                ),
                PotData(
                    "3_porcelain",
                    "Porcelain",
                    "5f45055fa648cdbb2c620ba5650b29ec03c03026ba4fa88aff0552974737b2b7",
                ),
// Material Flower Pots
                PotData(
                    "3_pot",
                    "Pot",
                    "7a40b762dc84f89c3eefa7243745c2293970a61445c6476f75ff06e0dec502c8",
                ),
                PotData(
                    "3_pot0barrel",
                    "Barrel",
                    "99e923f12bda51052879fd4f53218faa5fc11cca6062d2ad34edbd2119ef8b5b",
                ),
                PotData(
                    "3_pot0basket",
                    "Basket",
                    "31385a7afb3552faf71c2c5a8e6a5b1d2e672c786e8074433eb5839ace83b4",
                ),
                PotData(
                    "3_pot0sapling",
                    "Sapling",
                    "b2118cd747f730db9fe988ba7bb3b5beda9ba5999f751a84c993cf720fd2b86b",
                ),
                PotData(
                    "3_pot1black",
                    "Black",
                    "19c40af7d8effa3b49d23abba2dda9bda345c9bd58b8f18c8b2e6c075f4a4425",
                ),
                PotData(
                    "3_pot1bucket",
                    "Bucket",
                    "3a28b8f951a407699faefa2e648ba00a930bf1e65b07ce3dd0c73340099c1339",
                ),
                PotData(
                    "3_potbronze",
                    "Bronze",
                    "e129ec25f266dc1ecb6e75f5992e636dc458afe7c3742ec139952daa9a9d41",
                ),
                PotData(
                    "3_potgold",
                    "Gold",
                    "7a7a11208eda74331e46c8443d70f9f350c34b8c2519521c978c5cc57cfa4f24",
                ),
// Terracotta Flower Pots
                PotData(
                    "black",
                    "Black Terracotta",
                    "18b26c9aff7d19944b04aa4dec95aef48ee3fa58f57157886133216e749e85",
                ),
                PotData(
                    "blue",
                    "Blue Terracotta",
                    "26fedb63782e6a797c88c49b4111986d28687fc85ae4a41d9530315fe2467d",
                ),
                PotData(
                    "brown",
                    "Brown Terracotta",
                    "89cd31b5ae8d807d158c35d6f21f4bc0f1b7e4cec28eeeb4c3b481825b347dc",
                ),
                PotData(
                    "cyan",
                    "Cyan Terracotta",
                    "c8b3e292f9980f8a5ddc4db30e785249cac51c4d41e55476a41de8424ee68",
                ),
                PotData(
                    "gray",
                    "Gray Terracotta",
                    "71ce7ee653e1472daa66f728ae22519b5d74b0e1509d5de8ff2eaca58bfb5",
                ),
                PotData(
                    "green",
                    "Green Terracotta",
                    "e993ab4452477d928af9df36f81ff15171fcc492ff2d82f4bb888fa9547e7e",
                ),
                PotData(
                    "lblue",
                    "Light Blue Terracotta",
                    "e68476c421f6c04960103dfc501e1d5ab5356cceb67abee9e7d2a1b4546cd0cc",
                ),
                PotData(
                    "lgray",
                    "Light Gray Terracotta",
                    "4e159107698dd972f73a0c3634b6c41d8bd2f66e1a15070824ce93beb6e",
                ),
                PotData(
                    "lime",
                    "Lime Terracotta",
                    "bf53387a4891e180c67df1b662cdaf89b658a888e1e722b339e159fc3a",
                ),
                PotData(
                    "magenta",
                    "Magenta Terracotta",
                    "2ece434c3de27663e7f84c596807595ad1614b022903f39c7e521bd894813a3",
                ),
                PotData(
                    "orange",
                    "Orange Terracotta",
                    "71c47ba77739a3d7265284938d6a33a42b1601bdb8b9c808813a7747848d333",
                ),
                PotData(
                    "pink",
                    "Pink Terracotta",
                    "625a8dd1b7a915a4b739e22e72938d94cddbeaf57e5c22d3b566de9511773",
                ),
                PotData(
                    "purple",
                    "Purple Terracotta",
                    "c8b3e292f9980f8a5ddc4db30e785249cac51c4d41e55476a41de8424ee68",
                ),
                PotData(
                    "red",
                    "Red Terracotta",
                    "d463476ab895df62a68d57c31cce2a6626aa5c34059c423672aede833821cd",
                ),
                PotData(
                    "white",
                    "White Terracotta",
                    "62c618db76cd067c0e5f75a3f742594b1b9a062f26dc7326ee547fa5cb331",
                ),
                PotData(
                    "yellow",
                    "Yellow Terracotta",
                    "63f89f36c83df4994ec173aa8974b12ac629c609b78f6da825a2109e7da739",
                ),
            )
    }
}
