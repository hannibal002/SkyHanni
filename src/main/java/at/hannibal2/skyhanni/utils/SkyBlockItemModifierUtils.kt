package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.fishing.FishingApi.getFishingRodPart
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CachedItemData.Companion.cachedData
import at.hannibal2.skyhanni.utils.ItemUtils.containsCompound
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getStringList
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.isPositive
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.getBooleanOrDefault
import at.hannibal2.skyhanni.utils.compat.getByteOrDefault
import at.hannibal2.skyhanni.utils.compat.getCompoundOrDefault
import at.hannibal2.skyhanni.utils.compat.getDoubleOrDefault
import at.hannibal2.skyhanni.utils.compat.getIntOrDefault
import at.hannibal2.skyhanni.utils.compat.getLongOrDefault
import at.hannibal2.skyhanni.utils.compat.getStringOrDefault
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
object SkyBlockItemModifierUtils {

    fun ItemStack.getCoinsOfAvarice() = getAttributeLong("collected_coins")

    private val drillPartTypes = listOf("drill_part_upgrade_module", "drill_part_engine", "drill_part_fuel_tank")

    fun ItemStack.getHotPotatoCount() = getAttributeInt("hot_potato_count")

    fun ItemStack.getWetBookCount() = getAttributeInt("wet_book_count")

    fun ItemStack.getFarmingForDummiesCount() = getAttributeInt("farming_for_dummies_count")

    fun ItemStack.getOverclockerCount() = getAttributeInt("levelable_overclocks")

    fun ItemStack.getPolarvoidBookCount() = getAttributeInt("polarvoid")

    fun ItemStack.getBookwormBookCount() = getAttributeInt("bookworm_books")

    fun ItemStack.getCultivatingCounter() = getAttributeLong("farmed_cultivating")

    fun ItemStack.getOldHoeCounter() = getAttributeLong("mined_crops")
    fun ItemStack.getHoeExp() = getAttributeDouble("levelable_exp")?.toLong()
    fun ItemStack.getHoeLevel() = getAttributeInt("levelable_lvl")

    fun ItemStack.getSilexCount() = getHypixelEnchantments()?.get("efficiency")?.let {
        it - 5 - getBaseSilexCount()
    }?.takeIf { it > 0 }

    fun ItemStack.getMithrilInfusion(): Boolean = getAttributeByte("mithril_infusion") == 1.toByte()
    fun ItemStack.getFreeWill(): Boolean = getAttributeByte("free_will") == 1.toByte()

    private fun ItemStack.getBaseSilexCount() = when (getInternalName().asString()) {
        "STONK_PICKAXE" -> 1
        "PROMISING_SPADE" -> 5

        else -> 0
    }

    fun ItemStack.getTransmissionTunerCount() = getAttributeInt("tuned_transmission")

    fun ItemStack.getManaDisintegrators() = getAttributeInt("mana_disintegrator_count")

    fun ItemStack.getDungeonStarCount() = if (isDungeonItem()) {
        getStarCount() ?: getAttributeInt("dungeon_item_level")
    } else null

    fun ItemStack.getStarCount() = getAttributeInt("upgrade_level")

    private fun ItemStack.isDungeonItem() = getLore().any { it.contains("DUNGEON ") }

    @KSerializable
    data class PetInfo(
        @Expose val type: String,
        @Expose val active: Boolean = false,
        @Expose val exp: Double = 0.0,
        @Expose val tier: LorenzRarity,
        @Expose val hideInfo: Boolean = false,
        @Expose val heldItem: NeuInternalName? = null,
        @Expose val candyUsed: Int = 0,
        @Expose val skin: String? = null,
        @Deprecated("Some pets do not have uuids, use uniqueId instead", replaceWith = ReplaceWith("uniqueId"))
        @Expose val uuid: UUID? = null,
        @Expose val uniqueId: UUID? = null, // Only null when pet is read from a shop, or another non-"owned" source
        @Expose val hideRightClick: Boolean? = null,
        @Expose val noMove: Boolean? = null,
        @Expose val extraData: JsonObject? = null,
    ) {
        @Suppress("PropertyName")
        @Deprecated("Do not use, does not reflect Tier Boost, use PetData(petInfo).fauxInternalName instead")
        val _internalName = "$type;${tier.id}".toInternalName()
        val properSkinItem get() = skin?.let { "PET_SKIN_$skin".toInternalName() }
        fun getSkinVariantIndex() = properSkinItem?.let { PetUtils.getVariantIndexOrNull(it) }
    }

    fun ItemStack.getPetCandyUsed(): Int? {
        val data = cachedData
        if (data.petCandies == -1) {
            data.petCandies = getPetInfo()?.candyUsed
        }
        return data.petCandies
    }

    fun ItemStack.getHeldPetItem(): NeuInternalName? {
        val data = cachedData
        if (data.heldItem == NeuInternalName.NONE) {
            data.heldItem = getPetInfo()?.heldItem
        }
        return data.heldItem
    }

    fun ItemStack.isRiftTransferable(): Boolean {
        val data = cachedData
        return data.riftTransferable
            ?: UtilsPatterns.riftTransferablePattern.anyMatches(getLore())
                .also { data.riftTransferable = it }
    }

    fun ItemStack.isRiftExportable(): Boolean {
        val data = cachedData
        return data.riftExportable
            ?: UtilsPatterns.riftExportablePattern.anyMatches(getLore())
                .also { data.riftExportable = it }
    }

    fun ItemStack.wasRiftTransferred(): Boolean = getAttributeBoolean("rift_transferred")

    val warnedAboutPetParseFailure: MutableSet<String> = mutableSetOf()
    var lastWarnedParseFailure: SimpleTimeMark = SimpleTimeMark.farPast()

    fun ItemStack.getPetInfo(): PetInfo? {
        val colorlessName = hoverName.string.removeColor()
        // Repo pets will always return null for PetInfo, don't even attempt to parse it
        if (colorlessName.contains("→") || colorlessName.contains("{LVL}")) return null
        val petInfoJson = getExtraAttributes()?.takeIf {
            it.contains("petInfo")
        }?.getStringOrDefault("petInfo")?.takeIf {
            it.isNotEmpty()
        } ?: return null

        return try {
            ConfigManager.gson.fromJson(petInfoJson, PetInfo::class.java)
        } catch (e: Exception) {
            val added = warnedAboutPetParseFailure.add(colorlessName)
            if (!added || lastWarnedParseFailure.passedSince() <= 1.minutes) return null
            lastWarnedParseFailure = SimpleTimeMark.now()
            ErrorManager.skyHanniError(
                "Failed to parse pet info for item: $colorlessName",
                "exception" to e.message,
                "extraAttributes" to extraAttributes.toString(),
                "petInfoJson" to petInfoJson,
            )
        }
    }

    fun ItemStack.getPetLevel(): Int = getPetInfo()?.let(PetUtils::xpToLevel) ?: 1

    fun ItemStack.getMaxPetLevel(): Int = PetUtils.getMaxLevel(getInternalName())

    fun ItemStack.getDrillUpgrades() = getExtraAttributes()?.let {
        val list = mutableListOf<NeuInternalName>()
        for (attributes in it.keySet()) {
            if (attributes in drillPartTypes) {
                val upgradeItem = it.getStringOrDefault(attributes)
                list.add(upgradeItem.uppercase().toInternalName())
            }
        }
        list
    }

    fun ItemStack.getRodParts(): List<NeuInternalName> {
        return FishingApi.RodPart.entries.mapNotNull {
            this.getFishingRodPart(it)
        }
    }

    fun ItemStack.getPowerScroll() = getAttributeString("power_ability_scroll")?.toInternalName()

    fun ItemStack.getEnrichment() = getAttributeString("talisman_enrichment")

    fun ItemStack.getHelmetSkin() = getAttributeString("skin")?.toInternalName()

    fun ItemStack.getArmorDye() = getAttributeString("dye_item")?.toInternalName()

    fun ItemStack.getFungiCutterMode() = getAttributeString("fungi_cutter_mode")

    fun ItemStack.getRanchersSpeed() = getAttributeInt("ranchers_speed")

    fun ItemStack.getRune(): NeuInternalName? {
        val runesMap = getExtraAttributes()?.getCompoundOrDefault("runes") ?: return null
        val runesList = runesMap.keySet().associateWith { runesMap.getIntOrDefault(it) }.toList()
        if (runesList.isEmpty()) return null
        val (name, tier) = runesList.first()
        return "${name.uppercase()}_RUNE;$tier".toInternalName()
    }

    fun ItemStack.getAbilityScrolls() = getExtraAttributes()?.let { compound ->
        val ultimateWitherScroll = "ULTIMATE_WITHER_SCROLL".toInternalName()
        val implosion = "IMPLOSION_SCROLL".toInternalName()
        val witherShield = "WITHER_SHIELD_SCROLL".toInternalName()
        val shadowWarp = "SHADOW_WARP_SCROLL".toInternalName()

        val scrolls = mutableSetOf<NeuInternalName>()

        for (scroll in compound.getStringList("ability_scroll").map { it.toInternalName() }) {
            if (scroll == ultimateWitherScroll) {
                scrolls.add(implosion)
                scrolls.add(witherShield)
                scrolls.add(shadowWarp)
                continue
            }
            scrolls.add(scroll)
        }

        scrolls.toList()
    }

    fun ItemStack.getAttributes() = getExtraAttributes()
        ?.takeIf { it.containsCompound("attributes") }
        ?.getCompoundOrDefault("attributes")
        ?.let { attr ->
            attr.keySet().map {
                it.uppercase() to attr.getIntOrDefault(it)
            }.sortedBy { it.first }
        }

    fun ItemStack.hasAttributes() = getAttributes() != null

    fun ItemStack.getReforgeModifier() = getAttributeString("modifier")

    fun ItemStack.isRecombobulated() = getAttributeInt("rarity_upgrades").isPositive()

    fun ItemStack.hasJalapenoBook() = getAttributeInt("jalapeno_count").isPositive()

    fun ItemStack.hasEtherwarp() = getAttributeBoolean("ethermerge")

    fun ItemStack.hasWoodSingularity() = getAttributeInt("wood_singularity_count").isPositive()

    fun ItemStack.hasDivanPowderCoating() = getAttributeBoolean("divan_powder_coating")

    fun ItemStack.hasArtOfWar() = getAttributeInt("art_of_war_count").isPositive()

    fun ItemStack.hasBookOfStats() = getAttributeInt("stats_book") != null

    fun ItemStack.hasArtOfPeace() = getAttributeBoolean("artOfPeaceApplied")

    fun ItemStack.isMuseumDonated() = getAttributeBoolean("donated_museum")

    fun ItemStack.getLivingMetalProgress() = getAttributeInt("lm_evo")

    fun ItemStack.getSecondsHeld() = when (getItemId()) { // TODO move item IDs and attribute tags to repo
        "NEW_BOTTLE_OF_JYRRE" -> getAttributeInt("bottle_of_jyrre_seconds")
        "DISCRITE" -> getAttributeInt("rift_discrite_seconds")
        else -> getAttributeInt("seconds_held")
    }

    fun ItemStack.getEdition() = getAttributeInt("edition")

    fun ItemStack.getNewYearCake() = getAttributeInt("new_years_cake")

    fun ItemStack.getPersonalCompactorActive() = getAttributeByte("PERSONAL_DELETOR_ACTIVE") == 1.toByte()

    fun ItemStack.getHypixelEnchantments(): Map<String, Int>? = getExtraAttributes()
        ?.takeIf { it.contains("enchantments") }
        ?.run {
            val enchantments = this.getCompoundOrDefault("enchantments")
            enchantments.keySet().associateWith { enchantments.getIntOrDefault(it) }
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

    fun ItemStack.getMinecraftId() = BuiltInRegistries.ITEM.getKey(item)

    private val identifierPattern = "[a-z0-9_\\-.:]+".toRegex()

    fun isVanillaItem(itemId: String): Boolean {
        if (!identifierPattern.matches(itemId)) return false
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId)) != Items.AIR
    }

    fun ItemStack.getGemstones() = getExtraAttributes()?.let {
        val list = mutableListOf<GemstoneSlot>()
        for (attributes in it.keySet()) {
            if (attributes != "gems") continue
            val gemstones = it.getCompoundOrDefault(attributes)
            for (key in gemstones.keySet()) {
                if (key.endsWith("_gem")) continue
                if (key == "unlocked_slots") continue
                var value = gemstones.getStringOrDefault(key)
                if (value == "") {
                    val tag = gemstones.getCompoundOrDefault(key)
                    value = tag.getStringOrDefault("quality")
                    if (value == "") continue
                }

                val rawType = key.split("_")[0]
                val type = GemstoneType.getByNameOrNull(rawType)

                val quality = GemstoneQuality.getByNameOrNull(value)
                if (quality == null) {
                    ChatUtils.debug("Gemstone quality is null for item name().formattedTextCompatLeadingWhiteLessResets()§7: ('$key' = '$value')")
                    continue
                }
                if (type != null) {
                    list.add(GemstoneSlot(type, quality))
                } else {
                    val newKey = gemstones.getStringOrDefault(key + "_gem")
                    val newType = GemstoneType.getByNameOrNull(newKey)
                    if (newType == null) {
                        ChatUtils.debug("Gemstone type is null for item name().formattedTextCompatLeadingWhiteLessResets()§7: ('$newKey' with '$key' = '$value')")
                        continue
                    }
                    list.add(GemstoneSlot(newType, quality))
                }
            }
        }
        list
    }

    fun ItemStack.getAttributeString(label: String) =
        getExtraAttributes()?.getStringOrDefault(label)?.takeUnless { it.isBlank() }

    private fun ItemStack.getAttributeInt(label: String) =
        getExtraAttributes()?.getIntOrDefault(label)?.takeUnless { it == 0 }

    private fun ItemStack.getAttributeLong(label: String) =
        getExtraAttributes()?.getLongOrDefault(label)?.takeUnless { it == 0L }

    private fun ItemStack.getAttributeDouble(label: String) =
        getExtraAttributes()?.getDoubleOrDefault(label)?.takeUnless { it == 0.0 }

    private fun ItemStack.getAttributeBoolean(label: String) =
        getExtraAttributes()?.getBooleanOrDefault(label) ?: false

    private fun ItemStack.getAttributeByte(label: String) =
        getExtraAttributes()?.getByteOrDefault(label) ?: 0

    fun ItemStack.getExtraAttributes(): CompoundTag? {
        val data = cachedData
        if (data.lastExtraAttributesFetchTime.passedSince() < 0.1.seconds) {
            return data.lastExtraAttributes
        }
        val extraAttributes = get(DataComponents.CUSTOM_DATA)?.copyTag()
        data.lastExtraAttributes = extraAttributes
        data.lastExtraAttributesFetchTime = SimpleTimeMark.now()
        return extraAttributes
    }

    class GemstoneSlot(private val type: GemstoneType, private val quality: GemstoneQuality) {
        fun getInternalName() = "${quality.name}_${type.name}_GEM".toInternalName()
    }

    enum class GemstoneQuality(private val displayName: String, private val color: LorenzColor) {
        ROUGH("Rough", LorenzColor.WHITE),
        FLAWED("Flawed", LorenzColor.GREEN),
        FINE("Fine", LorenzColor.BLUE),
        FLAWLESS("Flawless", LorenzColor.DARK_PURPLE),
        PERFECT("Perfect", LorenzColor.GOLD),
        ;

        override fun toString() = displayName
        fun toDisplayString() = "${color.getChatColor()}$displayName"

        companion object {

            fun getByNameOrNull(name: String) = entries.firstOrNull { it.name.lowercase() == name.lowercase() }
        }
    }

    enum class GemstoneType(val displayName: String, private val color: LorenzColor) {
        JADE("Jade", LorenzColor.GREEN),
        AMBER("Amber", LorenzColor.GOLD),
        TOPAZ("Topaz", LorenzColor.YELLOW),
        SAPPHIRE("Sapphire", LorenzColor.BLUE),
        AMETHYST("Amethyst", LorenzColor.DARK_PURPLE),
        JASPER("Jasper", LorenzColor.LIGHT_PURPLE),
        RUBY("Ruby", LorenzColor.RED),
        OPAL("Opal", LorenzColor.WHITE),
        ONYX("Onyx", LorenzColor.DARK_GRAY),
        AQUAMARINE("Aquamarine", LorenzColor.AQUA),
        CITRINE("Citrine", LorenzColor.DARK_RED),
        PERIDOT("Peridot", LorenzColor.DARK_GREEN),
        ;

        override fun toString() = displayName
        fun toDisplayString() = "${color.getChatColor()}$displayName"

        companion object {

            fun getByNameOrNull(name: String) = entries.firstOrNull { it.name == name || it.displayName == name }
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
        ONYX('8'),
        AQUAMARINE('3'),
        CITRINE('4'),
        PERIDOT('2'),
        COMBAT('4'),
        DEFENSIVE('a'),
        MINING('5'),
        UNIVERSAL('f'),
        ;

        companion object {

            fun getByName(name: String): GemstoneSlotType =
                entries.firstOrNull { name.uppercase(Locale.ENGLISH).contains(it.name) }
                    ?: error("Unknown GemstoneSlotType: '$name'")

            fun getColorCode(name: String) = getByName(name).colorCode
        }
    }
}
