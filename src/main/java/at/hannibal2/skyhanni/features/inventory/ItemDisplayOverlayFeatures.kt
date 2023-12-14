package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
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
import at.hannibal2.skyhanni.features.garden.pests.PestAPI
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBottleOfJyrreSeconds
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEdition
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getRanchersSpeed
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.matchMatcher
import at.hannibal2.skyhanni.utils.matches
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ItemDisplayOverlayFeatures {
    // TODO USE SH-REPO
    private val config get() = SkyHanniMod.feature.inventory

    // TODO Add to SH-REPO
    private val masterStarPattern = "(?<tier>[A-Z])+_MASTER_STAR".toPattern()
    private val masterSkullPattern = "MASTER_SKULL_TIER_(?<tier>\\d)".toPattern()
    private val dungeonHeadPattern = "(GOLD|DIAMOND)_(?<floor>[A-Z])+_HEAD".toPattern()
    private val petLevelPattern = "\\[Lvl (?<level>.*)] .*".toPattern()
    private val kuudraKeyPattern = "KUUDRA_(?:(?<tier>.+)_)?TIER_KEY".toPattern()
    private val gardenVacuumPattern = "§7Vacuum Bag: §6(?<amount>\\d*) Pests?".toPattern()
    private val harvestPattern = "§7§7You may harvest §6(?<amount>.).*".toPattern()
    private val bingoGoalRankPattern =
        "(§.)*You were the (§.)*(?<rank>\\w+)(?<ordinal>(st|nd|rd|th)) (§.)*to".toPattern()

    private val newYearCake = "NEW_YEAR_CAKE".asInternalName()
    private val ranchersBoots = "RANCHERS_BOOTS".asInternalName()
    private val larvaHook = "LARVA_HOOK".asInternalName()
    private val bottleOfJyrre = "NEW_BOTTLE_OF_JYRRE".asInternalName()

    var done = false

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        val itemName = item.cleanName()
        val internalName = item.getInternalName()
        val chestName = InventoryUtils.openInventoryName()

        return when {
            isMasterStar(internalName) -> getMasterStarTip(internalName)
            isMasterSkull(internalName) -> getMasterSkullTip(internalName)
            isDungeonHead(internalName) -> getDungeonHeadTip(internalName)
            isNewYearCake(internalName) -> getNewYearCakeTip(item)
            isPet(itemName) -> getPetTip(itemName)
            isMinionTier(itemName, item) -> getMinionTierTip(itemName)
            isSack(item) -> getSackTip(itemName)
            isKuudraKey(internalName) -> getKuudraKeyTip(internalName)
            isSkillLevel(item) -> getSkillLevelTip(item, itemName)
            isCollectionLevel() -> getCollectionLevelTip(item)
            isRanchersBoots(internalName) -> getRanchersBootsTip(item)
            isLarvaHook(internalName) -> getLarvaHookTip(item)
            isDungeonPotion(item, internalName) -> getDungeonPotionTip(item)
            isVacuumGarden(item) -> getVacuumGardenTip(item)
            isBottleOfJyrre(item) -> getBottleOfJyrreTip(item)
            isEditionNumber(item) -> getEditionNumberTip(item)
            isBingoGoalRank(chestName, item) -> getBingoGoalRankTip(item)
            else -> ""
        }
    }

    // <editor-fold desc="Stack Size Implementations">
    private fun isMasterStar(item: NEUInternalName): Boolean =
        MASTER_STAR_TIER.isSelected() && masterStarPattern.matches(item)

    private fun getMasterStarTip(internalName: NEUInternalName): String {
        var tier = ""
        dungeonHeadPattern.matchMatcher(internalName) { tier = group("tier") }
        return when (tier) {
            "FIRST" -> "1"
            "SECOND" -> "2"
            "THIRD" -> "3"
            "FOURTH" -> "4"
            "FIFTH" -> "5"
            else -> ""
        }
    }

    private fun isMasterSkull(itemName: NEUInternalName): Boolean =
        MASTER_SKULL_TIER.isSelected() && masterSkullPattern.matches(itemName)

    private fun getMasterSkullTip(internalName: NEUInternalName): String {
        var tier = ""
        masterStarPattern.matchMatcher(internalName) { tier = group(tier) }
        return tier
    }

    private fun isDungeonHead(internalName: NEUInternalName): Boolean =
        DUNGEON_HEAD_FLOOR_NUMBER.isSelected() && dungeonHeadPattern.matches(internalName)

    private fun getDungeonHeadTip(internalName: NEUInternalName): String {
        var floor = ""
        dungeonHeadPattern.matchMatcher(internalName) { floor = group("type") }
        return when (floor) {
            "BONZO" -> "1"
            "SCARF" -> "2"
            "PROFESSOR" -> "3"
            "THORN" -> "4"
            "LIVID" -> "5"
            "SADAN" -> "6"
            "NECRON" -> "7"
            else -> ""
        }
    }

    private fun isNewYearCake(internalName: NEUInternalName): Boolean =
        NEW_YEAR_CAKE.isSelected() && internalName == newYearCake

    private fun getNewYearCakeTip(item: ItemStack): String {
        return "§b" + item.getExtraAttributes()?.getInteger("new_years_cake")
    }

    private fun isPet(itemName: String): Boolean {
        return PET_LEVEL.isSelected() && !InventoryUtils.openInventoryName()
            .endsWith("Sea Creature Guide") && ItemUtils.isPet(itemName)
    }

    private fun getPetTip(itemName: String): String {
        petLevelPattern.matchMatcher(itemName) {
            val rawLevel = group("level")
            val level = rawLevel.toIntOrNull()
                ?: throw IllegalStateException("pet level not found for item name '$itemName'")
            if (level != ItemUtils.maxPetLevel(itemName)) {
                return "$level"
            }
        }
        return ""
    }

    private fun isMinionTier(itemName: String, item: ItemStack): Boolean {
        return MINION_TIER.isSelected() && itemName.contains(" Minion ") &&
            !itemName.contains("Recipe") && item.getLore().any { it.contains("Place this minion") }
    }

    private fun getMinionTierTip(itemName: String): String {
        val array = itemName.split(" ")
        val last = array[array.size - 1]
        return last.romanToDecimal().toString()
    }

    private fun isSack(item: ItemStack): Boolean = config.displaySackName && ItemUtils.isSack(item)
    private fun getSackTip(itemName: String): String {
        return (if (itemName.contains("Enchanted")) "§5" else "") + grabSackName(itemName).substring(0, 2)
    }
    private fun grabSackName(name: String): String {
        val split = name.split(" ")
        val text = split[0]
        for (line in arrayOf("Large", "Medium", "Small", "Enchanted")) {
            if (text == line) return grabSackName(name.substring(text.length + 1))
        }
        return text
    }

    private fun isKuudraKey(internalName: NEUInternalName): Boolean =
        KUUDRA_KEY.isSelected() && kuudraKeyPattern.matches(internalName)

    private fun getKuudraKeyTip(internalName: NEUInternalName): String {
        return when (internalName) {
            "KUUDRA_TIER_KEY".asInternalName() -> "§a1"
            "KUUDRA_HOT_TIER_KEY".asInternalName() -> "§22"
            "KUUDRA_BURNING_TIER_KEY".asInternalName() -> "§e3"
            "KUUDRA_FIERY_TIER_KEY".asInternalName() -> "§64"
            "KUUDRA_INFERNAL_TIER_KEY".asInternalName() -> "§c5"
            else -> ""
        }
    }

    private fun getSkillLevelTip(item: ItemStack, itemName: String): String {
        if (CollectionAPI.isCollectionTier0(item.getLore())) return "0"
        val split = itemName.split(" ")
        if (!itemName.contains("Dungeon")) {
            val text = split.last()
            if (split.size < 2) return "0"
            return "" + text.romanToDecimalIfNecessary()
        }
        return ""
    }

    private fun isSkillLevel(item: ItemStack): Boolean {
        return SKILL_LEVEL.isSelected() && InventoryUtils.openInventoryName() == "Your Skills" &&
            item.getLore().any { it.contains("Click to view!") }
    }

    private fun isCollectionLevel() =
        COLLECTION_LEVEL.isSelected() && InventoryUtils.openInventoryName().endsWith(" Collections")

    private fun getCollectionLevelTip(item: ItemStack): String {
        val lore = item.getLore()
        if (lore.any { it.contains("Click to view!") }) {
            if (CollectionAPI.isCollectionTier0(lore)) return "0"
            item.name?.let {
                if (it.startsWith("§e")) {
                    val text = it.split(" ").last()
                    return "" + text.romanToDecimalIfNecessary()
                }
            }
        }
        return ""
    }

    private fun isRanchersBoots(internalName: NEUInternalName): Boolean =
        RANCHERS_BOOTS_SPEED.isSelected() && internalName == ranchersBoots

    private fun getRanchersBootsTip(item: ItemStack): String {
         item.getRanchersSpeed()?.let {
                return if (it > 400 && PetAPI.currentPet?.contains("Black Cat") == false) {
                    "§c$it"
                } else {
                    "§a$it"
            }
        }
        return ""
    }

    private fun isLarvaHook(internalName: NEUInternalName) = LARVA_HOOK.isSelected() && internalName == larvaHook
    private fun getLarvaHookTip(item: ItemStack): String {
        for (line in item.getLore()) {
            harvestPattern.matchMatcher(line) {
                val amount = group("amount").toInt()
                return when {
                    amount > 4 -> "§a$amount"
                    amount > 2 -> "§e$amount"
                    else -> "§c$amount"
                }
            }
        }
        return ""
    }

    private fun isDungeonPotion(item: ItemStack, internalName: NEUInternalName): Boolean {
        return DUNGEON_POTION_LEVEL.isSelected() && internalName == "POTION".asInternalName() && item.getExtraAttributes()
            ?.getString("potion_name") == "Dungeon"
    }

    private fun getDungeonPotionTip(item: ItemStack): String {
        val potionLevel = item.getExtraAttributes()?.getInteger("potion_level") ?: return ""
        return when (potionLevel) {
            in 1..2 -> "§f$potionLevel"
            in 3..4 -> "§a$potionLevel"
            in 5..6 -> "§9$potionLevel"
            else -> "§5$potionLevel"
        }
    }

    private fun isVacuumGarden(item: ItemStack) =
        VACUUM_GARDEN.isSelected() && item.getInternalNameOrNull() in PestAPI.vacuumVariants

    private fun getVacuumGardenTip(item: ItemStack): String {
        for (line in item.getLore()) {
            gardenVacuumPattern.matchMatcher(line) {
                val pests = group("amount").formatNumber()
                return if (config.vacuumBagCap) {
                    if (pests > 39) "§640" else "$pests"
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
        return ""
    }

    private fun getBottleOfJyrreTip(item: ItemStack): String {
        val seconds = item.getBottleOfJyrreSeconds() ?: 0
        return "§a${(seconds / 3600)}"
    }

    private fun isBottleOfJyrre(item: ItemStack) =
        BOTTLE_OF_JYRRE.isSelected() && item.getInternalNameOrNull() == bottleOfJyrre

    private fun isEditionNumber(item: ItemStack) = EDITION_NUMBER.isSelected() && item.getEdition() != null
    private fun getEditionNumberTip(item: ItemStack): String {
        item.getEdition()?.let { edition ->
            if (edition < 1_000) {
                return "§6$edition"
            }
        }
        return ""
    }

    private fun getBingoGoalRankTip(item: ItemStack): String {
        for (line in item.getLore()) {
            bingoGoalRankPattern.matchMatcher(line) {
                val rank = group("rank").formatNumber()
                if (rank < 10000) return "§6${NumberUtil.format(rank)}"
            }
        }
        return ""
    }

    private fun isBingoGoalRank(chestName: String, item: ItemStack) =
        BINGO_GOAL_RANK.isSelected() && chestName == "Bingo Card" && item.getLore().lastOrNull() == "§aGOAL REACHED"
    //</editor-fold>

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(11, "inventory.itemNumberAsStackSize") { element ->
            ConfigUtils.migrateIntArrayListToEnumArrayList(element, ItemNumberEntry::class.java)
        }
    }

    fun ItemNumberEntry.isSelected() = config.itemNumberAsStackSize.contains(this)
}
