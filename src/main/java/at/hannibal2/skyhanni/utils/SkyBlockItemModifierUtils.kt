package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import net.minecraft.item.ItemStack

object SkyBlockItemModifierUtils {
    private val drillPartTypes = listOf("drill_part_upgrade_module", "drill_part_engine", "drill_part_fuel_tank")

    fun ItemStack.getHotPotatoCount(): Int {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            for (attributes in extraAttributes.keySet) {
                if (attributes != "hot_potato_count") continue
                return extraAttributes.getInteger(attributes)
            }
        }
        return 0
    }

    fun ItemStack.getFarmingForDummiesCount(): Int {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            for (attributes in extraAttributes.keySet) {
                if (attributes != "farming_for_dummies_count") continue
                return extraAttributes.getInteger(attributes)
            }
        }
        return 0
    }

    fun ItemStack.getSilexCount(): Int {
        var silexTier = 0
        for ((name, amount) in getEnchantments()) {
            if (name == "efficiency") {
                if (amount > 5) {
                    silexTier = amount - 5
                }
            }
        }

        if (getInternalName() == "STONK_PICKAXE") {
            silexTier--
        }

        return silexTier
    }

    fun ItemStack.getTransmissionTunerCount(): Int {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            for (attributes in extraAttributes.keySet) {
                if (attributes != "tuned_transmission") continue
                return extraAttributes.getInteger(attributes)
            }
        }
        return 0
    }

    fun ItemStack.getManaDisintegrators(): Int {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            for (attributes in extraAttributes.keySet) {
                if (attributes != "mana_disintegrator_count") continue
                return extraAttributes.getInteger(attributes)
            }
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

    fun ItemStack.getDrillUpgrades(): List<String> {
        val list = mutableListOf<String>()
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            for (attributes in extraAttributes.keySet) {
                if (attributes in drillPartTypes) {
                    val upgradeItem = extraAttributes.getString(attributes)
                    list.add(upgradeItem.uppercase())
                }
            }
        }

        return list
    }

    fun ItemStack.getPowerScroll(): String? {
        return tagCompound?.getCompoundTag("ExtraAttributes")?.getString("power_ability_scroll")
            ?.takeUnless { it.isBlank() }
    }

    fun ItemStack.getHelmetSkin(): String? {
        return tagCompound?.getCompoundTag("ExtraAttributes")?.getString("skin")?.takeUnless { it.isBlank() }
    }

    fun ItemStack.getArmorDye(): String? {
        return tagCompound?.getCompoundTag("ExtraAttributes")?.getString("dye_item")?.takeUnless { it.isBlank() }
    }

    fun ItemStack.getAbilityScrolls(): List<String> {
        val list = mutableListOf<String>()
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            for (attributes in extraAttributes.keySet) {
                if (attributes == "ability_scroll") {

                    val tagList = extraAttributes.getTagList(attributes, 8)
                    for (i in 0..3) {
                        val text = tagList.get(i).toString()
                        if (text == "END") break
                        var internalName = text.replace("\"", "")
                        list.add(internalName)
                    }
                }
            }
        }

        return list
    }

    fun ItemStack.getReforgeName(): String? {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            for (attributes in extraAttributes.keySet) {
                if (attributes != "modifier") continue
                return extraAttributes.getString(attributes)
            }
        }
        return null
    }

    fun ItemStack.isRecombobulated(): Boolean {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            return extraAttributes.hasKey("rarity_upgrades")
        }

        return false
    }

    fun ItemStack.hasJalapenoBook(): Boolean {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            return extraAttributes.hasKey("jalapeno_count")
        }

        return false
    }

    fun ItemStack.hasEtherwarp(): Boolean {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            return extraAttributes.hasKey("ethermerge")
        }

        return false
    }

    fun ItemStack.hasWoodSingularity(): Boolean {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            return extraAttributes.hasKey("wood_singularity_count")
        }

        return false
    }

    fun ItemStack.hasArtOfWar(): Boolean {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            return extraAttributes.hasKey("art_of_war_count")
        }

        return false
    }

    // TODO untested
    fun ItemStack.hasBookOfStats(): Boolean {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            return extraAttributes.hasKey("stats_book")
        }

        return false
    }

    fun ItemStack.hasArtOfPiece(): Boolean {
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            return extraAttributes.hasKey("artOfPeaceApplied")
        }

        return false
    }

    fun ItemStack.getEnchantments(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            for (attributes in extraAttributes.keySet) {
                if (attributes != "enchantments") continue
                val enchantments = extraAttributes.getCompoundTag(attributes)
                for (key in enchantments.keySet) {
                    map[key] = enchantments.getInteger(key)
                }
            }
        }
        return map
    }

    fun ItemStack.getGemstones(): List<GemstoneSlot> {
        val list = mutableListOf<GemstoneSlot>()
        for (tags in tagCompound.keySet) {
            if (tags != "ExtraAttributes") continue
            val extraAttributes = tagCompound.getCompoundTag(tags)
            for (attributes in extraAttributes.keySet) {
                if (attributes != "gems") continue
                val gemstones = extraAttributes.getCompoundTag(attributes)
                for (key in gemstones.keySet) {
                    if (key.endsWith("_gem")) continue
                    if (key == "unlocked_slots") continue
                    val value = gemstones.getString(key)
                    if (value == "") continue

                    val rawType = key.split("_")[0]
                    val type = GemstoneType.getByName(rawType)

                    val tier = GemstoneTier.getByName(value)
                    if (tier == null) {
                        LorenzUtils.debug("Gemstone tier is null for item $name: ('$key' = '$value')")
                        continue
                    }
                    if (type != null) {
                        list.add(GemstoneSlot(type, tier))
                    } else {
                        val newKey = gemstones.getString(key + "_gem")
                        val newType = GemstoneType.getByName(newKey)
                        if (newType == null) {
                            LorenzUtils.debug("Gemstone type is null for item $name: ('$newKey' with '$key' = '$value')")
                            continue
                        }
                        list.add(GemstoneSlot(newType, tier))
                    }
                }
            }
        }
        return list
    }

    class GemstoneSlot(val type: GemstoneType, val tier: GemstoneTier) {
        fun getInternalName() = "${tier}_${type}_GEM"
    }

    enum class GemstoneTier(val displayName: String) {
        ROUGH("Rough"),
        FLAWED("Flawed"),
        FINE("Fine"),
        FLAWLESS("Flawless"),
        PERFECT("Perfect"),
        ;

        companion object {
            fun getByName(name: String) = GemstoneTier.values().firstOrNull { it.name == name }
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