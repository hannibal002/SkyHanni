package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.mixins.hooks.ItemStackCachedData
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.JsonObject
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import java.util.Locale

object SkyBlockItemModifierUtils {
    private val drillPartTypes = listOf("drill_part_upgrade_module", "drill_part_engine", "drill_part_fuel_tank")
    private val petLevelPattern = "§7\\[Lvl (?<level>.*)\\] .*".toPattern()

    fun ItemStack.getHotPotatoCount() = getAttributeInt("hot_potato_count")

    fun ItemStack.getFarmingForDummiesCount() = getAttributeInt("farming_for_dummies_count")

    fun ItemStack.getPolarvoidBookCount() = getAttributeInt("polarvoid")

    fun ItemStack.getCultivatingCounter() = getAttributeLong("farmed_cultivating")

    fun ItemStack.getHoeCounter() = getAttributeLong("mined_crops")

    fun ItemStack.getSilexCount() = getEnchantments()?.get("efficiency")?.let {
        it - 5 - getBaseSilexCount()
    }?.takeIf { it > 0 }

    private fun ItemStack.getBaseSilexCount() = when (getInternalName_old()) {
        "STONK_PICKAXE" -> 1
        "PROMISING_SPADE" -> 5

        else -> 0
    }

    fun ItemStack.getTransmissionTunerCount() = getAttributeInt("tuned_transmission")

    fun ItemStack.getManaDisintegrators() = getAttributeInt("mana_disintegrator_count")

    fun ItemStack.getDungeonStarCount() = if (isDungeonItem()) {
        getAttributeInt("upgrade_level") ?: getAttributeInt("dungeon_item_level")
    } else null

    private fun ItemStack.isDungeonItem() = getLore().any { it.contains("DUNGEON ") }

    fun ItemStack.getPetExp() = getPetInfo()?.get("exp")?.asDouble

    fun ItemStack.getPetCandyUsed(): Int? {
        val data = cachedData
        if (data.petCandies == -1) {
            data.petCandies = getPetInfo()?.get("candyUsed")?.asInt
        }
        return data.petCandies
    }

    fun ItemStack.getPetItem(): String? {
        val data = cachedData
        if (data.heldItem == "") {
            data.heldItem = getPetInfo()?.get("heldItem")?.asString
        }
        return data.heldItem
    }

    fun ItemStack.isRiftTransferable(): Boolean? {
        val data = cachedData
        if (data.riftTransferable == null) {
            data.riftTransferable = getLore().any { it == "§5§kX§5 Rift-Transferable §kX" }
        }
        return data.riftTransferable
    }

    fun ItemStack.isRiftExportable(): Boolean? {
        val data = cachedData
        if (data.riftExportable == null) {
            data.riftExportable = getLore().any { it == "§5§kX§5 Rift-Exportable §kX" }
        }
        return data.riftExportable
    }

    private fun ItemStack.getPetInfo() =
        ConfigManager.gson.fromJson(getExtraAttributes()?.getString("petInfo"), JsonObject::class.java)

    @Suppress("CAST_NEVER_SUCCEEDS")
    inline val ItemStack.cachedData get() = (this as ItemStackCachedData).skyhanni_cachedData

    fun ItemStack.getPetLevel(): Int {
        petLevelPattern.matchMatcher(this.displayName) {
            return group("level").toInt()
        }
        return 0
    }

    fun ItemStack.getDrillUpgrades() = getExtraAttributes()?.let {
        val list = mutableListOf<NEUInternalName>()
        for (attributes in it.keySet) {
            if (attributes in drillPartTypes) {
                val upgradeItem = it.getString(attributes)
                list.add(upgradeItem.uppercase().asInternalName())
            }
        }
        list
    }

    fun ItemStack.getPowerScroll() = getAttributeString("power_ability_scroll")?.asInternalName()

    fun ItemStack.getEnrichment() = getAttributeString("talisman_enrichment")

    fun ItemStack.getHelmetSkin() = getAttributeString("skin")?.asInternalName()

    fun ItemStack.getArmorDye() = getAttributeString("dye_item")?.asInternalName()

    fun ItemStack.getRune(): NEUInternalName? {
        val runesMap = getExtraAttributes()?.getCompoundTag("runes") ?: return null
        val runesList = runesMap.keySet.associateWith { runesMap.getInteger(it) }.toList()
        if (runesList.isEmpty()) return null
        val (name, tier) = runesList.first()
        return "${name.uppercase()}_RUNE;$tier".asInternalName()
    }

    fun ItemStack.getAbilityScrolls() = getExtraAttributes()?.let {
        val list = mutableListOf<NEUInternalName>()
        for (attributes in it.keySet) {
            if (attributes == "ability_scroll") {
                val tagList = it.getTagList(attributes, 8)
                for (i in 0..3) {
                    val text = tagList.get(i).toString()
                    if (text == "END") break
                    list.add(text.replace("\"", "").asInternalName())
                }
            }
        }
        list.toList()
    }

    fun ItemStack.getAttributes() = getExtraAttributes()
        ?.takeIf { it.hasKey("attributes", 10) }
        ?.getCompoundTag("attributes")
        ?.let { attr ->
            attr.keySet.map {
                it.uppercase() to attr.getInteger(it)
            }.sortedBy { it.first }
        }

    fun ItemStack.getReforgeName() = getAttributeString("modifier")?.let {
        when {
            it == "pitchin" -> "pitchin_koi"
            it == "warped" && name!!.removeColor().startsWith("Hyper ") -> "endstone_geode"

            else -> it
        }
    }

    fun ItemStack.isRecombobulated() = getAttributeBoolean("rarity_upgrades")

    fun ItemStack.hasJalapenoBook() = getAttributeBoolean("jalapeno_count")

    fun ItemStack.hasEtherwarp() = getAttributeBoolean("ethermerge")

    fun ItemStack.hasWoodSingularity() = getAttributeBoolean("wood_singularity_count")

    fun ItemStack.hasArtOfWar() = getAttributeBoolean("art_of_war_count")

    // TODO untested
    fun ItemStack.hasBookOfStats() = getAttributeBoolean("stats_book")

    fun ItemStack.hasArtOfPeace() = getAttributeBoolean("artOfPeaceApplied")

    fun ItemStack.getLivingMetalProgress() = getAttributeInt("lm_evo")

    fun ItemStack.getPrehistoricEggBlocksWalked() = getAttributeInt("blocks_walked")

    fun ItemStack.getNecronHandlesFound() = getAttributeInt("handles_found")

    fun ItemStack.getBloodGodKills() = getAttributeInt("blood_god_kills")
    
    fun ItemStack.getYetiRodFishesCaught() = getAttributeInt("fishes_caught")

    fun ItemStack.getEdition() = getAttributeInt("edition")

    fun ItemStack.getAuctionNumber() = getAttributeInt("auction")

    fun ItemStack.getEnchantments() = getExtraAttributes()?.takeIf { it.hasKey("enchantments") }?.run {
        val enchantments = this.getCompoundTag("enchantments")
        enchantments.keySet.associateWith { enchantments.getInteger(it) }
    }

    fun ItemStack.getAppliedPocketSackInASack(): Int? {
        val data = cachedData
        if (data.sackInASack == -1) {
            data.sackInASack = getAttributeInt("sack_pss")
        }
        return data.sackInASack
    }

    fun ItemStack.getRecipientName() = getAttributeString("recipient_name")

    fun ItemStack.getItemUuid() = getAttributeString("uuid")

    fun ItemStack.getItemId() = getAttributeString("id")

    fun ItemStack.getMinecraftId() = Item.itemRegistry.getNameForObject(item) as ResourceLocation

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

    fun ItemStack.getExtraAttributes() = tagCompound?.getCompoundTag("ExtraAttributes")

    class GemstoneSlot(val type: GemstoneType, val quality: GemstoneQuality) {
        fun getInternalName() = "${quality}_${type}_GEM".asInternalName()
    }

    enum class GemstoneQuality(val displayName: String) {
        ROUGH("Rough"),
        FLAWED("Flawed"),
        FINE("Fine"),
        FLAWLESS("Flawless"),
        PERFECT("Perfect"),
        ;

        companion object {
            fun getByName(name: String) = entries.firstOrNull { it.name == name }
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
            fun getByName(name: String) = entries.firstOrNull { it.name == name }
        }
    }

    enum class GemstoneSlotType(val colorCode: Char) {
        JADE('a'),
        AMBER('6'),
        TOPAZ('e'),
        SAPPHIRE('b'),
        AMETHYST('5'),
        JASPER('d'),
        RUBY('c'),
        OPAL('f'),
        COMBAT('4'),
        OFFENSIVE('9'),
        DEFENSIVE('a'),
        MINING('5'),
        UNIVERSAL('f')
        ;

        companion object {
            fun getColorCode(name: String) = entries.stream().filter {
                name.uppercase(Locale.ENGLISH).contains(it.name)
            }.findFirst().get().colorCode
        }
    }
}