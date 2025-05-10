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
        if (strippedId == "skull") {
            return "player_head"
        }
        if (strippedId == "red_flower") {
            if (damage == 0) {
                return "poppy"
            }
            if (damage == 1) {
                return "blue_orchid"
            }
            if (damage == 2) {
                return "allium"
            }
            if (damage == 3) {
                return "azure_bluet"
            }
            if (damage == 4) {
                return "red_tulip"
            }
            if (damage == 5) {
                return "orange_tulip"
            }
            if (damage == 6) {
                return "white_tulip"
            }
            if (damage == 7) {
                return "pink_tulip"
            }
            if (damage == 8) {
                return "oxeye_daisy"
            }
        }
        if (strippedId == "yellow_flower") {
            return "dandelion"
        }
        if (strippedId == "fireworks") {
            return "firework_rocket"
        }
        if (strippedId == "dye") {
            if (damage == 0) {
                return "ink_sac"
            }
            if (damage == 1) {
                return "red_dye"
            }
            if (damage == 2) {
                return "green_dye"
            }
            if (damage == 3) {
                return "cocoa_beans"
            }
            if (damage == 4) {
                return "lapis_lazuli"
            }
            if (damage == 5) {
                return "purple_dye"
            }
            if (damage == 6) {
                return "cyan_dye"
            }
            if (damage == 7) {
                return "light_gray_dye"
            }
            if (damage == 8) {
                return "gray_dye"
            }
            if (damage == 9) {
                return "pink_dye"
            }
            if (damage == 10) {
                return "lime_dye"
            }
            if (damage == 11) {
                return "yellow_dye"
            }
            if (damage == 12) {
                return "light_blue_dye"
            }
            if (damage == 13) {
                return "magenta_dye"
            }
            if (damage == 14) {
                return "orange_dye"
            }
            if (damage == 15) {
                return "bone_meal"
            }
        }
        if (strippedId == "spawn_egg") {
            if (damage == 0) {
                return "polar_bear_spawn_egg"
            }
            if (damage == 4) {
                return "elder_guardian_spawn_egg"
            }
            if (damage == 52) {
                return "spider_spawn_egg"
            }
            if (damage == 54) {
                return "zombie_spawn_egg"
            }
            if (damage == 55) {
                return "slime_spawn_egg"
            }
            if (damage == 58) {
                return "enderman_spawn_egg"
            }
            if (damage == 61) {
                return "blaze_spawn_egg"
            }
            if (damage == 67) {
                return "endermite_spawn_egg"
            }
            if (damage == 94) {
                return "squid_spawn_egg"
            }
            if (damage == 96) {
                return "mooshroom_spawn_egg"
            }
            if (damage == 101) {
                return "rabbit_spawn_egg"
            }
            if (damage == 120) {
                return "villager_spawn_egg"
            }
        }
        if (strippedId == "carpet") {
            return getColor(damage) + "_carpet"
        }
        if (strippedId == "leaves") {
            return getWood(damage) + "_leaves"
        }
        if (strippedId == "leaves2") {
            if (damage == 0) {
                return "acacia_leaves"
            }
            if (damage == 1) {
                return "dark_oak_leaves"
            }
        }
        if (strippedId == "banner") {
            return getColor(damage) + "_banner"
        }
        if (strippedId.contains("record_")) {
            return strippedId.replace("record_", "music_disc_")
        }
        if (strippedId == "cooked_fish") {
            if (damage == 0) {
                return "cooked_cod"
            }
            if (damage == 1) {
                return "cooked_salmon"
            }
        }
        if (strippedId == "bed") {
            return "red_bed"
        }
        if (strippedId == "wool") {
            return getColor(damage) + "_wool"
        }
        if (strippedId == "trapdoor") {
            return "oak_trapdoor"
        }
        if (strippedId == "speckled_melon") {
            return "glistering_melon_slice"
        }
        if (strippedId == "melon_block") {
            return "melon"
        }
        if (strippedId == "fish") {
            if (damage == 0) {
                return "cod"
            }
            if (damage == 1) {
                return "salmon"
            }
            if (damage == 2) {
                return "tropical_fish"
            }
            if (damage == 3) {
                return "pufferfish"
            }
        }
        if (strippedId == "log") {
            return getWood(damage) + "_log"
        }
        if (strippedId == "log2") {
            if (damage == 0) {
                return "acacia_log"
            }
            if (damage == 1) {
                return "dark_oak_log"
            }
        }
        if (strippedId == "waterlily") {
            return "lily_pad"
        }
        if (strippedId == "web") {
            return "cobweb"
        }
        if (strippedId == "reeds") {
            return "sugar_cane"
        }
        if (strippedId == "double_plant") {
            if (damage == 0) {
                return "sunflower"
            }
            if (damage == 1) {
                return "lilac"
            }
            if (damage == 2) {
                return "tall_grass"
            }
            if (damage == 3) {
                return "large_fern"
            }
            if (damage == 4) {
                return "rose_bush"
            }
            if (damage == 5) {
                return "peony"
            }
        }
        if (strippedId == "deadbush") {
            return "dead_bush"
        }
        if (strippedId == "firework_charge") {
            return "firework_star"
        }
        if (strippedId == "netherbrick") {
            return "nether_brick"
        }
        if (strippedId == "wooden_button") {
            return "oak_button"
        }
        if (strippedId == "slime") {
            return "slime_block"
        }
        if (strippedId == "boat") {
            return "oak_boat"
        }
        if (strippedId == "brick_block") {
            return "bricks"
        }
        if (strippedId == "stained_glass") {
            return getColor(damage) + "_stained_glass"
        }
        if (strippedId == "stained_glass_pane") {
            return getColor(damage) + "_stained_glass_pane"
        }
        if (strippedId == "hardened_clay") {
            return "terracotta"
        }
        if (strippedId == "stained_hardened_clay") {
            return getColor(damage) + "_terracotta"
        }
        if (strippedId == "fence") {
            return "oak_fence"
        }
        if (strippedId == "fence_gate") {
            return "oak_fence_gate"
        }
        if (strippedId == "grass") {
            return "grass_block"
        }
        if (strippedId == "lit_pumpkin") {
            return "jack_o_lantern"
        }
        if (strippedId == "planks") {
            return getWood(damage) + "_planks"
        }
        if (strippedId == "mob_spawner") {
            return "spawner"
        }
        if (strippedId == "noteblock") {
            return "note_block"
        }
        if (strippedId == "golden_rail") {
            return "powered_rail"
        }
        if (strippedId == "quartz_ore") {
            return "nether_quartz_ore"
        }
        if (strippedId == "sapling") {
            return getWood(damage) + "_sapling"
        }
        if (strippedId == "sign") {
            return "oak_sign"
        }
        if (strippedId == "stonebrick") {
            if (damage == 0) {
                return "stone_bricks"
            }
            if (damage == 1) {
                return "mossy_stone_bricks"
            }
            if (damage == 2) {
                return "cracked_stone_bricks"
            }
            if (damage == 3) {
                return "chiseled_stone_bricks"
            }
        }
        if (strippedId == "snow_layer") {
            return "snow"
        }
        if (strippedId == "wooden_slab") {
            return getWood(damage) + "_slab"
        }
        if (strippedId == "stone_slab2") {
            return "red_sandstone_slab"
        }
        if (strippedId == "wooden_door") {
            return "oak_door"
        }
        if (strippedId == "wooden_pressure_plate") {
            return "oak_pressure_plate"
        }
        if (strippedId == "tallgrass") {
            if (damage == 0) {
                return "dead_bush"
            }
            if (damage == 1) {
                return "short_grass"
            }
            if (damage == 2) {
                return "fern"
            }
        }
        if (strippedId == "monster_egg") {
            if (damage == 0) {
                return "infested_stone"
            }
        }
        return strippedId
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
