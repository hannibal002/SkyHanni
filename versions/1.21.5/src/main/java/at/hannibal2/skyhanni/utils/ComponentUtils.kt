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
        if (nbtInfo.extraAttributes != null) {
            val extraAttributes = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, nbtInfo.extraAttributes).asCompound().get()
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
            name = profile?.name?.getOrNull(),
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
        return "minecraft:" + convertMinecraftIdToModern2(id, damage)
    }

    private fun convertMinecraftIdToModern2(id: String, damage: Int): String {
        val strippedId = id.replace("minecraft:", "")
        return when {
            strippedId == "red_flower" -> getRedFlowerByDamage(damage) ?: strippedId
            strippedId == "dye" -> getDyeByDamage(damage) ?: strippedId
            strippedId == "spawn_egg" -> getSpawnEggByDamage(damage) ?: strippedId
            strippedId == "carpet" -> getColorByDamage(damage) + "_carpet"
            strippedId == "leaves" -> getWoodByDamage(damage) + "_leaves"
            strippedId == "leaves2" -> getWood2ByDamage(damage) + "_leaves"
            strippedId == "banner" -> getColorByDamage(damage) + "_banner"
            strippedId.contains("record_") -> strippedId.replace("record_", "music_disc_")
            strippedId == "cooked_fish" -> getCookedFishByDamage(damage) ?: strippedId
            strippedId == "wool" -> getColorByDamage(damage) + "_wool"
            strippedId == "fish" -> getFishByDamage(damage) ?: strippedId
            strippedId == "log" -> getWoodByDamage(damage) + "_log"
            strippedId == "log2" -> getWood2ByDamage(damage) + "_log"
            strippedId == "double_plant" -> getDoublePlantByDamage(damage) ?: strippedId
            strippedId == "stained_glass" -> getColorByDamage(damage) + "_stained_glass"
            strippedId == "stained_glass_pane" -> getColorByDamage(damage) + "_stained_glass_pane"
            strippedId == "stained_hardened_clay" -> getColorByDamage(damage) + "_terracotta"
            strippedId == "planks" -> getWoodByDamage(damage) + "_planks"
            strippedId == "sapling" -> getWoodByDamage(damage) + "_sapling"
            strippedId == "stonebrick" -> getStoneBrickByDamage(damage) ?: strippedId
            strippedId == "wooden_slab" -> getWoodByDamage(damage) + "_slab"
            strippedId == "tallgrass" -> getTallGrassByDamage(damage) ?: strippedId
            strippedId == "monster_egg" -> getMonsterEggByDamage(damage) ?: strippedId

            else -> getOtherItemById(strippedId) ?: strippedId
        }
    }

    fun convertModernToLegacyId(modernId: String): Pair<String, Int> {
        val (id, damage) = convertModernToLegacyId2(modernId.replace("minecraft:", ""))
        return Pair("minecraft:$id", damage)
    }

    private fun convertModernToLegacyId2(modernId: String): Pair<String, Int> {
        if (modernId == "player_head") return "skull" to 3
        getOtherItemByIdReversed(modernId)?.let { return it to 0 }
        getRedFlowerDamage(modernId)?.let { return "red_flower" to it }
        getDyeDamage(modernId)?.let { return "dye" to it }
        getSpawnEggDamage(modernId)?.let { return "spawn_egg" to it }
        getCookedFishDamage(modernId)?.let { return "cooked_fish" to it }
        getFishDamage(modernId)?.let { return "fish" to it }
        getDoublePlantDamage(modernId)?.let { return "double_plant" to it }
        getStoneBrickDamage(modernId)?.let { return "stonebrick" to it }
        getTallGrassDamage(modernId)?.let { return "tallgrass" to it }
        getMonsterEggDamage(modernId)?.let { return "monster_egg" to it }
        when {
            modernId.contains("music_disc_") -> {
                return modernId.replace("music_disc_", "record_") to 0
            }

            modernId.endsWith("_carpet") -> {
                val color = modernId.removeSuffix("_carpet")
                val damage = getColorDamage(color)
                if (damage != null) {
                    return "carpet" to damage
                }
            }

            modernId.endsWith("_leaves") -> {
                val wood = modernId.removeSuffix("_leaves")
                var damage = getWood2Damage(wood)
                if (damage != null) {
                    return "leaves" to damage
                }
                damage = getWoodDamage(wood)
                if (damage != null) {
                    return "leaves2" to damage
                }
            }

            modernId.endsWith("_banner") -> {
                val color = modernId.removeSuffix("_banner")
                val damage = getColorDamage(color)
                if (damage != null) {
                    return "banner" to damage
                }
            }

            modernId.endsWith("_wool") -> {
                val color = modernId.removeSuffix("_wool")
                val damage = getColorDamage(color)
                if (damage != null) {
                    return "wool" to damage
                }
            }

            modernId.endsWith("_log") -> {
                val wood = modernId.removeSuffix("_log")
                var damage = getWood2Damage(wood)
                if (damage != null) {
                    return "log" to damage
                }
                damage = getWoodDamage(wood)
                if (damage != null) {
                    return "log2" to damage
                }
            }

            modernId.endsWith("stained_glass") -> {
                val color = modernId.removeSuffix("_stained_glass")
                val damage = getColorDamage(color)
                if (damage != null) {
                    return "stained_glass" to damage
                }
            }

            modernId.endsWith("stained_glass_pane") -> {
                val color = modernId.removeSuffix("_stained_glass_pane")
                val damage = getColorDamage(color)
                if (damage != null) {
                    return "stained_glass_pane" to damage
                }
            }

            modernId.endsWith("_terracotta") -> {
                val color = modernId.removeSuffix("_terracotta")
                val damage = getColorDamage(color)
                if (damage != null) {
                    return "stained_hardened_clay" to damage
                }
            }

            modernId.endsWith("_planks") -> {
                val wood = modernId.removeSuffix("_planks")
                val damage = getWoodDamage(wood)
                if (damage != null) {
                    return "planks" to damage
                }
            }

            modernId.endsWith("_sapling") -> {
                val wood = modernId.removeSuffix("_sapling")
                val damage = getWoodDamage(wood)
                if (damage != null) {
                    return "sapling" to damage
                }
            }

            modernId.endsWith("_slab") -> {
                val wood = modernId.removeSuffix("_slab")
                val damage = getWoodDamage(wood)
                if (damage != null) {
                    return "wooden_slab" to damage
                }
            }
        }

        return modernId to 0
    }

    private val otherItemsMap = mapOf(
        "skull" to "player_head",
        "yellow_flower" to "dandelion",
        "fireworks" to "firework_rocket",
        "bed" to "red_bed",
        "trapdoor" to "oak_trapdoor",
        "speckled_melon" to "glistering_melon_slice",
        "melon_block" to "melon",
        "waterlily" to "lily_pad",
        "web" to "cobweb",
        "reeds" to "sugar_cane",
        "deadbush" to "dead_bush",
        "firework_charge" to "firework_star",
        "netherbrick" to "nether_brick",
        "wooden_button" to "oak_button",
        "slime" to "slime_block",
        "boat" to "oak_boat",
        "brick_block" to "bricks",
        "fence" to "oak_fence",
        "hardened_clay" to "terracotta",
        "fence_gate" to "oak_fence_gate",
        "grass" to "grass_block",
        "lit_pumpkin" to "jack_o_lantern",
        "mob_spawner" to "spawner",
        "noteblock" to "note_block",
        "golden_rail" to "powered_rail",
        "quartz_ore" to "nether_quartz_ore",
        "sign" to "oak_sign",
        "snow_layer" to "snow",
        "stone_slab2" to "red_sandstone_slab",
        "wooden_door" to "oak_door",
        "wooden_pressure_plate" to "oak_pressure_plate",
    )

    private fun getOtherItemById(id: String): String? {
        return otherItemsMap[id]
    }

    private fun getOtherItemByIdReversed(id: String): String? {
        return otherItemsMap.entries.find { it.value == id }?.key
    }

    private val redFlowerMap = mapOf(
        0 to "poppy",
        1 to "blue_orchid",
        2 to "allium",
        3 to "azure_bluet",
        4 to "red_tulip",
        5 to "orange_tulip",
        6 to "white_tulip",
        7 to "pink_tulip",
        8 to "oxeye_daisy",
    )

    private fun getRedFlowerByDamage(damage: Int): String? {
        return redFlowerMap[damage]
    }

    private fun getRedFlowerDamage(id: String): Int? {
        return redFlowerMap.entries.find { it.value == id }?.key
    }

    private val dyeMap = mapOf(
        0 to "ink_sac",
        1 to "red_dye",
        2 to "green_dye",
        3 to "cocoa_beans",
        4 to "lapis_lazuli",
        5 to "purple_dye",
        6 to "cyan_dye",
        7 to "light_gray_dye",
        8 to "gray_dye",
        9 to "pink_dye",
        10 to "lime_dye",
        11 to "yellow_dye",
        12 to "light_blue_dye",
        13 to "magenta_dye",
        14 to "orange_dye",
        15 to "bone_meal",
    )

    private fun getDyeByDamage(damage: Int): String? {
        return dyeMap[damage]
    }

    private fun getDyeDamage(id: String): Int? {
        return dyeMap.entries.find { it.value == id }?.key
    }

    private val spawnEggMap = mapOf(
        0 to "polar_bear_spawn_egg",
        4 to "elder_guardian_spawn_egg",
        52 to "spider_spawn_egg",
        54 to "zombie_spawn_egg",
        55 to "slime_spawn_egg",
        58 to "enderman_spawn_egg",
        61 to "blaze_spawn_egg",
        67 to "endermite_spawn_egg",
        94 to "squid_spawn_egg",
        96 to "mooshroom_spawn_egg",
        101 to "rabbit_spawn_egg",
        120 to "villager_spawn_egg",
    )

    private fun getSpawnEggByDamage(damage: Int): String? {
        return spawnEggMap[damage]
    }

    private fun getSpawnEggDamage(id: String): Int? {
        return spawnEggMap.entries.find { it.value == id }?.key
    }

    private val cookedFishMap = mapOf(
        0 to "cooked_cod",
        1 to "cooked_salmon",
    )

    private fun getCookedFishByDamage(damage: Int): String? {
        return cookedFishMap[damage]
    }

    private fun getCookedFishDamage(id: String): Int? {
        return cookedFishMap.entries.find { it.value == id }?.key
    }

    private val fishMap = mapOf(
        0 to "cod",
        1 to "salmon",
        2 to "tropical_fish",
        3 to "pufferfish",
    )

    private fun getFishByDamage(damage: Int): String? {
        return fishMap[damage]
    }

    private fun getFishDamage(id: String): Int? {
        return fishMap.entries.find { it.value == id }?.key
    }

    private val log2Map = mapOf(
        0 to "acacia",
        1 to "dark_oak",
    )

    private fun getWood2ByDamage(damage: Int): String {
        return log2Map[damage] ?: ""
    }

    private fun getWood2Damage(id: String): Int? {
        return log2Map.entries.find { it.value == id }?.key
    }

    private val doublePlantMap = mapOf(
        0 to "sunflower",
        1 to "lilac",
        2 to "tall_grass",
        3 to "large_fern",
        4 to "rose_bush",
        5 to "peony",
    )

    private fun getDoublePlantByDamage(damage: Int): String? {
        return doublePlantMap[damage]
    }

    private fun getDoublePlantDamage(id: String): Int? {
        return doublePlantMap.entries.find { it.value == id }?.key
    }

    private val stoneBrickMap = mapOf(
        0 to "stone_bricks",
        1 to "mossy_stone_bricks",
        2 to "cracked_stone_bricks",
        3 to "chiseled_stone_bricks",
    )

    private fun getStoneBrickByDamage(damage: Int): String? {
        return stoneBrickMap[damage]
    }

    private fun getStoneBrickDamage(id: String): Int? {
        return stoneBrickMap.entries.find { it.value == id }?.key
    }

    private val tallGrassMap = mapOf(
        0 to "dead_bush",
        1 to "short_grass",
        2 to "fern",
    )

    private fun getTallGrassByDamage(damage: Int): String? {
        return tallGrassMap[damage]
    }

    private fun getTallGrassDamage(id: String): Int? {
        return tallGrassMap.entries.find { it.value == id }?.key
    }

    private val colorMap = mapOf(
        0 to "white",
        1 to "orange",
        2 to "magenta",
        3 to "light_blue",
        4 to "yellow",
        5 to "lime",
        6 to "pink",
        7 to "gray",
        8 to "light_gray",
        9 to "cyan",
        10 to "purple",
        11 to "blue",
        12 to "brown",
        13 to "green",
        14 to "red",
        15 to "black",
    )

    private fun getColorByDamage(damage: Int): String {
        return colorMap[damage] ?: ""
    }

    private fun getColorDamage(id: String): Int? {
        return colorMap.entries.find { it.value == id }?.key
    }

    private val woodMap = mapOf(
        0 to "oak",
        1 to "spruce",
        2 to "birch",
        3 to "jungle",
        4 to "acacia",
        5 to "dark_oak",
    )

    private fun getWoodByDamage(damage: Int): String {
        return woodMap[damage] ?: ""
    }

    private fun getWoodDamage(id: String): Int? {
        return woodMap.entries.find { it.value == id }?.key
    }

    private val monsterEggMap = mapOf(
        0 to "infested_stone",
    )

    private fun getMonsterEggByDamage(damage: Int): String? {
        return monsterEggMap[damage]
    }

    private fun getMonsterEggDamage(id: String): Int? {
        return monsterEggMap.entries.find { it.value == id }?.key
    }
}
