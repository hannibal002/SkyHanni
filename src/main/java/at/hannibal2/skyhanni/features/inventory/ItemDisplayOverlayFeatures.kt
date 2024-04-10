package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.BINGO_GOAL_RANK
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.BOTTLE_OF_JYRRE
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.COLLECTION_LEVEL
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.DUNGEON_HEAD_FLOOR_NUMBER
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.DUNGEON_POTION_LEVEL
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.EDITION_NUMBER
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.KUUDRA_KEY
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.LARVA_HOOK
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.MASTER_SKULL_TIER
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.MASTER_STAR_TIER
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.MINION_TIER
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.NEW_YEAR_CAKE
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.PET_LEVEL
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.RANCHERS_BOOTS_SPEED
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.SKILL_LEVEL
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.VACUUM_GARDEN
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.pests.PestAPI
import at.hannibal2.skyhanni.features.skillprogress.SkillProgress
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBottleOfJyrreSeconds
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEdition
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetLevel
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getRanchersSpeed
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ItemDisplayOverlayFeatures {
    private val config get() = SkyHanniMod.feature.inventory

    private val patternGroup = RepoPattern.group("inventory.item.overlay")
    private val masterSkullPattern by patternGroup.pattern(
        "masterskull",
        "(.*)Master Skull - Tier ."
    )
    private val gardenVacuumPatterm by patternGroup.pattern(
        "vacuum",
        "§7Vacuum Bag: §6(?<amount>\\d*) Pests?"
    )
    private val harvestPattern by patternGroup.pattern(
        "harvest",
        "§7§7You may harvest §6(?<amount>.).*"
    )
    private val dungeonPotionPattern by patternGroup.pattern(
        "dungeonpotion",
        "Dungeon (?<level>.*) Potion"
    )
    private val bingoGoalRankPattern by patternGroup.pattern(
        "bingogoalrank",
        "(§.)*You were the (§.)*(?<rank>[\\w]+)(?<ordinal>(st|nd|rd|th)) (§.)*to"
    )

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        val itemName = item.cleanName()
        val internalName = item.getInternalName()
        val chestName = InventoryUtils.openInventoryName()
        val lore = item.getLore()

        if (MASTER_STAR_TIER.isSelected()) {
            when (internalName) {
                "FIRST_MASTER_STAR".asInternalName() -> return "1"
                "SECOND_MASTER_STAR".asInternalName() -> return "2"
                "THIRD_MASTER_STAR".asInternalName() -> return "3"
                "FOURTH_MASTER_STAR".asInternalName() -> return "4"
                "FIFTH_MASTER_STAR".asInternalName() -> return "5"
            }
        }

        if (MASTER_SKULL_TIER.isSelected()) {
            masterSkullPattern.matchMatcher(itemName) {
                return itemName.substring(itemName.length - 1)
            }
        }

        if (DUNGEON_HEAD_FLOOR_NUMBER.isSelected() && (itemName.contains("Golden ") || itemName.contains("Diamond "))) {
            when {
                itemName.contains("Bonzo") -> return "1"
                itemName.contains("Scarf") -> return "2"
                itemName.contains("Professor") -> return "3"
                itemName.contains("Thorn") -> return "4"
                itemName.contains("Livid") -> return "5"
                itemName.contains("Sadan") -> return "6"
                itemName.contains("Necron") -> return "7"
            }
        }

        if (NEW_YEAR_CAKE.isSelected() && internalName == "NEW_YEAR_CAKE".asInternalName()) {
            val year = item.getExtraAttributes()?.getInteger("new_years_cake")?.toString() ?: ""
            return "§b$year"
        }

        if (PET_LEVEL.isSelected()) {
            if (item.getItemCategoryOrNull() == ItemCategory.PET) {
                val level = item.getPetLevel()
                if (level != ItemUtils.maxPetLevel(itemName)) {
                    return level.toString()
                }
            }
        }

        if (MINION_TIER.isSelected() && itemName.contains(" Minion ") &&
            !itemName.contains("Recipe") && lore.any { it.contains("Place this minion") }
        ) {
            val array = itemName.split(" ")
            val last = array[array.size - 1]
            return last.romanToDecimal().toString()
        }

        if (config.displaySackName && ItemUtils.isSack(item)) {
            val sackName = grabSackName(itemName)
            return (if (itemName.contains("Enchanted")) "§5" else "") + sackName.substring(0, 2)
        }

        if (KUUDRA_KEY.isSelected() && itemName.contains("Kuudra Key")) {
            return when (internalName) {
                "KUUDRA_TIER_KEY".asInternalName() -> "§a1"
                "KUUDRA_HOT_TIER_KEY".asInternalName() -> "§22"
                "KUUDRA_BURNING_TIER_KEY".asInternalName() -> "§e3"
                "KUUDRA_FIERY_TIER_KEY".asInternalName() -> "§64"
                "KUUDRA_INFERNAL_TIER_KEY".asInternalName() -> "§c5"
                else -> "§4?"
            }
        }

        if (SKILL_LEVEL.isSelected() &&
            InventoryUtils.openInventoryName() == "Your Skills" &&
            lore.any { it.contains("Click to view!") }
        ) {
            if (CollectionAPI.isCollectionTier0(lore)) return "0"
            val split = itemName.split(" ")
            if (!itemName.contains("Dungeon")) {
                val skillName = split.first()
                val text = split.last()
                if (split.size < 2) return "0"
                val level = "" + text.romanToDecimalIfNecessary()
                val skill = SkillType.getByNameOrNull(skillName) ?: return level
                val skillInfo = SkillAPI.storage?.get(skill) ?: return level
                return if (SkillProgress.config.overflowConfig.enableInSkillMenuAsStackSize)
                    "" + skillInfo.overflowLevel else level
            }
        }

        if (COLLECTION_LEVEL.isSelected() && InventoryUtils.openInventoryName().endsWith(" Collections")) {
            if (lore.any { it.contains("Click to view!") }) {
                if (CollectionAPI.isCollectionTier0(lore)) return "0"
                val name = item.name
                if (name.startsWith("§e")) {
                    val text = name.split(" ").last()
                    return "" + text.romanToDecimalIfNecessary()
                }
            }
        }

        if (RANCHERS_BOOTS_SPEED.isSelected() && internalName == "RANCHERS_BOOTS".asInternalName()) {
            item.getRanchersSpeed()?.let {
                val isUsingBlackCat = PetAPI.isCurrentPet("Black Cat")
                val helmet = InventoryUtils.getHelmet()?.getInternalName()
                val hand = InventoryUtils.getItemInHand()?.getInternalName()
                val racingHelmet = "RACING_HELMET".asInternalName()
                val cactusKnife = "CACTUS_KNIFE".asInternalName()
                val is500 = isUsingBlackCat || helmet == racingHelmet || (GardenAPI.inGarden() && hand == cactusKnife)
                val effectiveSpeedCap = if (is500) 500 else 400
                val text = if (it > 999) "1k" else "$it"
                return if (it > effectiveSpeedCap) "§c$text" else "§a$text"
            }
        }

        if (LARVA_HOOK.isSelected() && internalName == "LARVA_HOOK".asInternalName()) {
            lore.matchFirst(harvestPattern) {
                val amount = group("amount").toInt()
                return when {
                    amount > 4 -> "§a$amount"
                    amount > 2 -> "§e$amount"
                    else -> "§c$amount"
                }
            }
        }

        if (DUNGEON_POTION_LEVEL.isSelected() && itemName.startsWith("Dungeon ") && itemName.contains(" Potion")) {
            dungeonPotionPattern.matchMatcher(item.name.removeColor()) {
                return when (val level = group("level").romanToDecimal()) {
                    in 1..2 -> "§f$level"
                    in 3..4 -> "§a$level"
                    in 5..6 -> "§9$level"
                    else -> "§5$level"
                }
            }
        }

        if (VACUUM_GARDEN.isSelected() && internalName in PestAPI.vacuumVariants && isOwnVacuum(lore)) {
            lore.matchFirst(gardenVacuumPatterm) {
                val pests = group("amount").formatLong()
                return if (config.vacuumBagCap) {
                    if (pests > 39) "§640+" else "$pests"
                } else {
                    when {
                        pests < 40 -> "$pests"
                        pests < 1_000 -> "§6$pests"
                        pests < 100_000 -> "§c${pests / 1000}k"
                        else -> "§c${pests / 100_000 / 10.0}m"
                    }
                }
            }
        }

        if (BOTTLE_OF_JYRRE.isSelected() && internalName == "NEW_BOTTLE_OF_JYRRE".asInternalName()) {
            val seconds = item.getBottleOfJyrreSeconds() ?: 0
            return "§a${(seconds / 3600)}"
        }

        if (EDITION_NUMBER.isSelected()) {
            item.getEdition()?.let { edition ->
                if (edition < 1_000) {
                    return "§6$edition"
                }
            }
        }

        if (BINGO_GOAL_RANK.isSelected() && chestName == "Bingo Card" && lore.lastOrNull() == "§aGOAL REACHED") {
            lore.matchFirst(bingoGoalRankPattern) {
                val rank = group("rank").formatLong()
                if (rank < 10000) return "§6${NumberUtil.format(rank)}"
            }
        }

        return ""
    }

    private fun isOwnVacuum(lore: List<String>) =
        lore.none { it.contains("Click to trade!") || it.contains("Starting bid:") || it.contains("Buy it now:") }

    var done = false

    private fun grabSackName(name: String): String {
        val split = name.split(" ")
        val text = split[0]
        for (line in arrayOf("Large", "Medium", "Small", "Enchanted")) {
            if (text == line) return grabSackName(name.substring(text.length + 1))
        }
        return text
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(11, "inventory.itemNumberAsStackSize") { element ->
            ConfigUtils.migrateIntArrayListToEnumArrayList(element, ItemNumberEntry::class.java)
        }
        event.transform(29, "inventory.itemNumberAsStackSize") { element ->
            fixRemovedConfigElement(element)
        }
    }

    private fun fixRemovedConfigElement(data: JsonElement): JsonElement {
        if (!data.isJsonArray) return data
        val newList = JsonArray()
        for (element in data.asJsonArray) {
            if (element.asString == "REMOVED") continue
            newList.add(element)
        }
        return newList
    }

    fun ItemNumberEntry.isSelected() = config.itemNumberAsStackSize.contains(this)
}
