package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.jsonobjects.other.NeuNbtInfoJson
import at.hannibal2.skyhanni.data.jsonobjects.other.toGameProfile
import com.mojang.serialization.JsonOps
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.component.type.ProfileComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtOps
import net.minecraft.util.Unit

object ComponentUtils {
    fun convertToComponents(stack: ItemStack, nbtInfo: NeuNbtInfoJson?) {
        nbtInfo ?: return
        val extraAttributes = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, nbtInfo.extraAttributes).asCompound().get()
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(extraAttributes))
        if (nbtInfo.enchantments?.isNotEmpty() == true) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        }
        if (nbtInfo.unbreakable?.boolean == true) {
            stack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE)
        }
        if (nbtInfo.display != null) {
            val display = nbtInfo.display
            if (display.color != null) {
                stack.set(DataComponentTypes.DYED_COLOR, DyedColorComponent(display.color))
            }
        }
        if (nbtInfo.skullOwner != null) {
            val skullOwner = nbtInfo.skullOwner
            stack.set(DataComponentTypes.PROFILE, ProfileComponent(skullOwner.toGameProfile()))
        }

    }

    fun convertMinecraftIdToModern(id: String, damage: Int): String {
        return "minecraft:" + convertMinecraftIdToModern2(id, damage)
    }

    private fun convertMinecraftIdToModern2(id: String, damage: Int): String {
        val strippedId = id.replace("minecraft:", "")
        return when {
            strippedId == "skull" -> "player_head"
            strippedId == "red_flower" -> when (damage) {
                0 -> "poppy"
                1 -> "blue_orchid"
                2 -> "allium"
                3 -> "azure_bluet"
                4 -> "red_tulip"
                5 -> "orange_tulip"
                6 -> "white_tulip"
                7 -> "pink_tulip"
                8 -> "oxeye_daisy"
                else -> strippedId
            }

            strippedId == "yellow_flower" -> "dandelion"
            strippedId == "fireworks" -> "firework_rocket"
            strippedId == "dye" -> when (damage) {
                0 -> "ink_sac"
                1 -> "red_dye"
                2 -> "green_dye"
                3 -> "cocoa_beans"
                4 -> "lapis_lazuli"
                5 -> "purple_dye"
                6 -> "cyan_dye"
                7 -> "light_gray_dye"
                8 -> "gray_dye"
                9 -> "pink_dye"
                10 -> "lime_dye"
                11 -> "yellow_dye"
                12 -> "light_blue_dye"
                13 -> "magenta_dye"
                14 -> "orange_dye"
                15 -> "bone_meal"
                else -> strippedId
            }

            strippedId == "spawn_egg" -> when (damage) {
                0 -> "polar_bear_spawn_egg"
                4 -> "elder_guardian_spawn_egg"
                52 -> "spider_spawn_egg"
                54 -> "zombie_spawn_egg"
                55 -> "slime_spawn_egg"
                58 -> "enderman_spawn_egg"
                61 -> "blaze_spawn_egg"
                67 -> "endermite_spawn_egg"
                94 -> "squid_spawn_egg"
                96 -> "mooshroom_spawn_egg"
                101 -> "rabbit_spawn_egg"
                120 -> "villager_spawn_egg"
                else -> strippedId
            }

            strippedId == "carpet" -> getColor(damage) + "_carpet"
            strippedId == "leaves" -> getWood(damage) + "_leaves"
            strippedId == "leaves2" -> when (damage) {
                0 -> "acacia_leaves"
                1 -> "dark_oak_leaves"
                else -> strippedId
            }

            strippedId == "banner" -> getColor(damage) + "_banner"
            strippedId.contains("record_") -> strippedId.replace("record_", "music_disc_")
            strippedId == "cooked_fish" -> when (damage) {
                0 -> "cooked_cod"
                1 -> "cooked_salmon"
                else -> strippedId
            }

            strippedId == "bed" -> "red_bed"
            strippedId == "wool" -> getColor(damage) + "_wool"
            strippedId == "trapdoor" -> "oak_trapdoor"
            strippedId == "speckled_melon" -> "glistering_melon_slice"
            strippedId == "melon_block" -> "melon"
            strippedId == "fish" -> when (damage) {
                0 -> "cod"
                1 -> "salmon"
                2 -> "tropical_fish"
                3 -> "pufferfish"
                else -> strippedId
            }

            strippedId == "log" -> getWood(damage) + "_log"
            strippedId == "log2" -> when (damage) {
                0 -> "acacia_log"
                1 -> "dark_oak_log"
                else -> strippedId
            }

            strippedId == "waterlily" -> "lily_pad"
            strippedId == "web" -> "cobweb"
            strippedId == "reeds" -> "sugar_cane"
            strippedId == "double_plant" -> when (damage) {
                0 -> "sunflower"
                1 -> "lilac"
                2 -> "tall_grass"
                3 -> "large_fern"
                4 -> "rose_bush"
                5 -> "peony"
                else -> strippedId
            }

            strippedId == "deadbush" -> "dead_bush"
            strippedId == "firework_charge" -> "firework_star"
            strippedId == "netherbrick" -> "nether_brick"
            strippedId == "wooden_button" -> "oak_button"
            strippedId == "slime" -> "slime_block"
            strippedId == "boat" -> "oak_boat"
            strippedId == "brick_block" -> "bricks"
            strippedId == "stained_glass" -> getColor(damage) + "_stained_glass"
            strippedId == "stained_glass_pane" -> getColor(damage) + "_stained_glass_pane"
            strippedId == "hardened_clay" -> "terracotta"
            strippedId == "stained_hardened_clay" -> getColor(damage) + "_terracotta"
            strippedId == "fence" -> "oak_fence"
            strippedId == "fence_gate" -> "oak_fence_gate"
            strippedId == "grass" -> "grass_block"
            strippedId == "lit_pumpkin" -> "jack_o_lantern"
            strippedId == "planks" -> getWood(damage) + "_planks"
            strippedId == "mob_spawner" -> "spawner"
            strippedId == "noteblock" -> "note_block"
            strippedId == "golden_rail" -> "powered_rail"
            strippedId == "quartz_ore" -> "nether_quartz_ore"
            strippedId == "sapling" -> getWood(damage) + "_sapling"
            strippedId == "sign" -> "oak_sign"
            strippedId == "stonebrick" -> when (damage) {
                0 -> "stone_bricks"
                1 -> "mossy_stone_bricks"
                2 -> "cracked_stone_bricks"
                3 -> "chiseled_stone_bricks"
                else -> strippedId
            }

            strippedId == "snow_layer" -> "snow"
            strippedId == "wooden_slab" -> getWood(damage) + "_slab"
            strippedId == "stone_slab2" -> "red_sandstone_slab"
            strippedId == "wooden_door" -> "oak_door"
            strippedId == "wooden_pressure_plate" -> "oak_pressure_plate"
            strippedId == "tallgrass" -> when (damage) {
                0 -> "dead_bush"
                1 -> "short_grass"
                2 -> "fern"
                else -> strippedId
            }

            strippedId == "monster_egg" -> when (damage) {
                0 -> "infested_stone"
                else -> strippedId
            }

            else -> strippedId
        }
    }

    private fun getColor(damage: Int): String {
        return when (damage) {
            0 -> "white"
            1 -> "orange"
            2 -> "magenta"
            3 -> "light_blue"
            4 -> "yellow"
            5 -> "lime"
            6 -> "pink"
            7 -> "gray"
            8 -> "light_gray"
            9 -> "cyan"
            10 -> "purple"
            11 -> "blue"
            12 -> "brown"
            13 -> "green"
            14 -> "red"
            15 -> "black"
            else -> ""
        }
    }

    private fun getWood(damage: Int): String {
        return when (damage) {
            0 -> "oak"
            1 -> "spruce"
            2 -> "birch"
            3 -> "jungle"
            4 -> "acacia"
            5 -> "dark_oak"
            else -> ""
        }
    }
}
