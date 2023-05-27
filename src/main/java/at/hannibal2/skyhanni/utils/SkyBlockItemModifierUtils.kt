package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack

object SkyBlockItemModifierUtils {
    private val drillPartTypes = listOf("drill_part_upgrade_module", "drill_part_engine", "drill_part_fuel_tank")
    private val petLevelPattern = "§7\\[Lvl (?<level>.*)\\] .*".toPattern()

    fun ItemStack.getHotPotatoCount() = getAttributeInt("hot_potato_count")

    fun ItemStack.getFarmingForDummiesCount() = getAttributeInt("farming_for_dummies_count")

    fun ItemStack.getCultivatingCounter() = getAttributeLong("farmed_cultivating")

    fun ItemStack.getHoeCounter() = getAttributeLong("mined_crops")

    fun ItemStack.getSilexCount() = getEnchantments()?.get("efficiency")?.let {
        it - 5 - getBaseSilexCount()
    }?.takeIf { it > 0 }

    private fun ItemStack.getBaseSilexCount() = when (getInternalName()) {
        "STONK_PICKAXE" -> 1
        "PROMISING_SPADE" -> 5

        else -> 0
    }

    fun ItemStack.getTransmissionTunerCount() = getAttributeInt("tuned_transmission")

    fun ItemStack.getManaDisintegrators() = getAttributeInt("mana_disintegrator_count")

    fun ItemStack.getPetCandyUsed() = ConfigManager.gson.fromJson(getExtraAttributes()?.getString("petInfo"), JsonObject::class.java)?.get("candyUsed")?.asInt

    fun ItemStack.getPetItem() = ConfigManager.gson.fromJson(getExtraAttributes()?.getString("petInfo"), JsonObject::class.java)?.get("heldItem")?.asString

    fun ItemStack.getPetLevel(): Int {
        petLevelPattern.matchMatcher(this.displayName) {
            return group("level").toInt()
        }
        return 0
    }

    fun ItemStack.getMasterStars(): Int {
        val stars = mapOf(
            "➊" to 1,
            "➋" to 2,
            "➌" to 3,
            "➍" to 4,
            "➎" to 5,
        )
        val itemName = name!!
        for ((icon, number) in stars) {
            if (itemName.endsWith(icon)) {
                return number
            }
        }

        return 0
    }

    fun ItemStack.getDrillUpgrades() = getExtraAttributes()?.let {
        val list = mutableListOf<String>()
        for (attributes in it.keySet) {
            if (attributes in drillPartTypes) {
                val upgradeItem = it.getString(attributes)
                list.add(upgradeItem.uppercase())
            }
        }
        list
    }

    fun ItemStack.getPowerScroll() = getAttributeString("power_ability_scroll")

    fun ItemStack.getHelmetSkin() = getAttributeString("skin")

    fun ItemStack.getArmorDye() = getAttributeString("dye_item")

    fun ItemStack.getRune(): String? {
        val runesMap = getExtraAttributes()?.getCompoundTag("runes") ?: return null
        val runesList = runesMap.keySet.associateWith { runesMap.getInteger(it) }.toList()
        if (runesList.isEmpty()) return null
        val (name, tier) = runesList.first()
        return "${name.uppercase()}_RUNE;$tier"
    }

    fun ItemStack.getAbilityScrolls() = getExtraAttributes()?.let {
        val list = mutableListOf<String>()
        for (attributes in it.keySet) {
            if (attributes == "ability_scroll") {
                val tagList = it.getTagList(attributes, 8)
                for (i in 0..3) {
                    val text = tagList.get(i).toString()
                    if (text == "END") break
                    val internalName = text.replace("\"", "")
                    list.add(internalName)
                }
            }
        }
        list.toList()
    }

    fun ItemStack.getReforgeName() = getAttributeString("modifier")

    fun ItemStack.isRecombobulated() = getAttributeBoolean("rarity_upgrades")

    fun ItemStack.hasJalapenoBook() = getAttributeBoolean("jalapeno_count")

    fun ItemStack.hasEtherwarp() = getAttributeBoolean("ethermerge")

    fun ItemStack.hasWoodSingularity() = getAttributeBoolean("wood_singularity_count")

    fun ItemStack.hasArtOfWar() = getAttributeBoolean("art_of_war_count")

    // TODO untested
    fun ItemStack.hasBookOfStats() = getAttributeBoolean("stats_book")

    fun ItemStack.hasArtOfPiece() = getAttributeBoolean("artOfPeaceApplied")

    fun ItemStack.getEnchantments() = getExtraAttributes()?.takeIf { it.hasKey("enchantments") }?.run {
        val enchantments = this.getCompoundTag("enchantments")
        enchantments.keySet.associateWith { enchantments.getInteger(it) }
    }

    fun ItemStack.getGemstones() = getExtraAttributes()?.let {
        val list = mutableListOf<GemstoneSlot>()
        for (attributes in it.keySet) {
            if (attributes != "gems") continue
            val gemstones = it.getCompoundTag(attributes)
            for (key in gemstones.keySet) {
                if (key.endsWith("_gem")) continue
                if (key == "unlocked_slots") continue
                var value = gemstones.getString(key)
                if (value == "") {
                    val tag = gemstones.getCompoundTag(key)
                    value = tag.getString("quality")
                    if (value == "") continue
                }

                val rawType = key.split("_")[0]
                val type = GemstoneType.getByName(rawType)

                val quality = GemstoneQuality.getByName(value)
                if (quality == null) {
                    LorenzUtils.debug("Gemstone quality is null for item $name: ('$key' = '$value')")
                    continue
                }
                if (type != null) {
                    list.add(GemstoneSlot(type, quality))
                } else {
                    val newKey = gemstones.getString(key + "_gem")
                    val newType = GemstoneType.getByName(newKey)
                    if (newType == null) {
                        LorenzUtils.debug("Gemstone type is null for item $name: ('$newKey' with '$key' = '$value')")
                        continue
                    }
                    list.add(GemstoneSlot(newType, quality))
                }
            }
        }
        list
    }

    private fun ItemStack.getAttributeString(label: String) =
        getExtraAttributes()?.getString(label)?.takeUnless { it.isBlank() }

    private fun ItemStack.getAttributeInt(label: String) =
        getExtraAttributes()?.getInteger(label)?.takeUnless { it == 0 }

    private fun ItemStack.getAttributeLong(label: String) =
        getExtraAttributes()?.getLong(label)?.takeUnless { it == 0L }

    private fun ItemStack.getAttributeBoolean(label: String): Boolean {
        return getExtraAttributes()?.hasKey(label) ?: false
    }

    private fun ItemStack.getExtraAttributes() = tagCompound?.getCompoundTag("ExtraAttributes")

    class GemstoneSlot(val type: GemstoneType, val quality: GemstoneQuality) {
        fun getInternalName() = "${quality}_${type}_GEM"
    }

    enum class GemstoneQuality(val displayName: String) {
        ROUGH("Rough"),
        FLAWED("Flawed"),
        FINE("Fine"),
        FLAWLESS("Flawless"),
        PERFECT("Perfect"),
        ;

        companion object {
            fun getByName(name: String) = GemstoneQuality.values().firstOrNull { it.name == name }
        }
    }

    enum class GemstoneType(val displayName: String) {
        JADE("Jade"),
        AMBER("Amber"),
        TOPAZ("Topaz"),
        SAPPHIRE("Sapphire"),
        AMETHYST("Amethyst"),
        JASPER("Jasper"),
        RUBY("Ruby"),
        OPAL("Opal"),
        ;

        companion object {
            fun getByName(name: String) = values().firstOrNull { it.name == name }
        }
    }
}