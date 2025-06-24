package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.DisplayInfo
import at.hannibal2.skyhanni.data.jsonobjects.other.NbtBoolean
import at.hannibal2.skyhanni.data.jsonobjects.other.NeuNbtInfoJson
import at.hannibal2.skyhanni.data.jsonobjects.other.PropertiesInfo
import at.hannibal2.skyhanni.data.jsonobjects.other.SkullOwnerInfo
import at.hannibal2.skyhanni.data.jsonobjects.other.TextureInfo
import at.hannibal2.skyhanni.data.jsonobjects.other.toGameProfile
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.setCustomItemName
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.DyedColorComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.component.type.ProfileComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtOps
import net.minecraft.util.Unit
import kotlin.jvm.optionals.getOrNull

object ComponentUtils {
    fun convertToComponents(stack: ItemStack, nbtInfo: NeuNbtInfoJson?) {
        nbtInfo ?: return
        nbtInfo.extraAttributes?.let { extraJson ->
            val extraAttributes = JsonOps.INSTANCE
                .convertTo(NbtOps.INSTANCE, extraJson)
                .asCompound()
                .get()
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(extraAttributes))
        }
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
            if (display.name != null) {
                stack.setCustomItemName(display.name)
            } else {
                ErrorManager.skyHanniError("stack display name is null", "extra attributes" to nbtInfo.extraAttributes)
            }
            if (display.lore != null) {
                stack.setLore(display.lore)
            }
        }
        if (nbtInfo.skullOwner != null) {
            val skullOwner = nbtInfo.skullOwner
            stack.set(DataComponentTypes.PROFILE, ProfileComponent(skullOwner.toGameProfile()))
        }

    }

    fun convertToNeuNbtInfoJson(stack: ItemStack): JsonObject {
        val isUnbreakable = NbtBoolean(stack.contains(DataComponentTypes.UNBREAKABLE))
        val profile = stack.get(DataComponentTypes.PROFILE)
        val profileProperties = profile?.properties?.get("textures")?.firstOrNull()
        val value = profileProperties?.value
        val signature = profileProperties?.signature
        val propertiesInfo = PropertiesInfo(listOf(TextureInfo(value = value, signature = signature)))
        val uuid = profile?.id?.getOrNull() ?: "53924f1a-87e6-4709-8e53-f1c7d13dc239"
        val skullOwner = SkullOwnerInfo(
            uuid = uuid.toString(),
            properties = propertiesInfo,
            hypixelPopulated = NbtBoolean(true),
            name = profile?.name?.getOrNull()
        )
        val lore = stack.getLore()
        val color = stack.get(DataComponentTypes.DYED_COLOR)?.rgb
        val displayInfo = DisplayInfo(name = stack.name.formattedTextCompat(), lore = lore, color = color)
        val customData = stack.get(DataComponentTypes.CUSTOM_DATA)
        val extraAttributes: JsonObject? = if (customData != null) {
            NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, customData.copyNbt()).asJsonObject
        } else {
            null
        }
        val enchants = if (stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) listOf(JsonObject()) else null

        val nbt = NeuNbtInfoJson(
            hideFlags = 254,
            unbreakable = isUnbreakable,
            skullOwner = skullOwner,
            display = displayInfo,
            extraAttributes = extraAttributes,
            explosion = null,
            customPotionEffects = null,
            enchantments = enchants,
            overrideMeta = NbtBoolean(true),
            generation = null,
            resolved = null,
        )
        return ConfigManager.gson.toJsonTree(nbt).asJsonObject
    }

    fun convertMinecraftIdToModern(id: String, damage: Int): String {
        val convertMinecraftIdToModern2 = convertMinecraftIdToModern2(id, damage)
        if (convertMinecraftIdToModern2 == id && damage > 0) {
            println("Unconverted minecraft id with damage above 0. id: $id damage: $damage")
        }
        return "minecraft:" + convertMinecraftIdToModern2
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

            strippedId == "snow" -> "snow_block"
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

            strippedId == "sand" && damage == 1 -> "red_sand"

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
