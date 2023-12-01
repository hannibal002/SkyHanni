package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry
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
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.SECRET_BINGO_DISCOVERY
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.pests.PestAPI
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBottleOfJyrreSeconds
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEdition
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ItemDisplayOverlayFeatures {
    private val config get() = SkyHanniMod.feature.inventory

    // TODO repo
    private val rancherBootsSpeedCapPattern = "§7Current Speed Cap: §a(?<cap>.*)".toPattern()
    private val petLevelPattern = "\\[Lvl (?<level>.*)] .*".toPattern()
    private val gardenVacuumPatterm = "§7Vacuum Bag: §6(?<amount>\\d*) Pests?".toPattern()
    private val harvestPattern = "§7§7You may harvest §6(?<amount>.).*".toPattern()
    private val dungeonPotionPattern = "Dungeon (?<level>.*) Potion".toPattern()
    private val secretBingoDiscoveryPattern = "(§.)*You were the (§.)*(?<nth>[\\w]+)(?<ordinal>(st|nd|rd|th)) (§.)*to".toPattern()

    private val bottleOfJyrre = "NEW_BOTTLE_OF_JYRRE".asInternalName()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        val itemName = item.cleanName()
        val chestName = InventoryUtils.openInventoryName()

        if (MASTER_STAR_TIER.isSelected()) {
            when (itemName) {
                "First Master Star" -> return "1"
                "Second Master Star" -> return "2"
                "Third Master Star" -> return "3"
                "Fourth Master Star" -> return "4"
                "Fifth Master Star" -> return "5"
            }
        }

        if (MASTER_SKULL_TIER.isSelected() && itemName.matchRegex("(.*)Master Skull - Tier .")) {
            return itemName.substring(itemName.length - 1)
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

        if (NEW_YEAR_CAKE.isSelected() && itemName.startsWith("New Year Cake (")) {
            return "§b" + itemName.between("(Year ", ")")
        }

        if (PET_LEVEL.isSelected()) {
            val chestName = InventoryUtils.openInventoryName()
            if (!chestName.endsWith("Sea Creature Guide") && ItemUtils.isPet(itemName)) {
                petLevelPattern.matchMatcher(itemName) {
                    val rawLevel = group("level")
                    val level = rawLevel.toIntOrNull()
                        ?: throw IllegalStateException("pet level not found for item name '$itemName'")
                    if (level != ItemUtils.maxPetLevel(itemName)) {
                        return "$level"
                    }
                }
            }
        }

        if (MINION_TIER.isSelected() && itemName.contains(" Minion ") &&
            !itemName.contains("Recipe") && item.getLore().any { it.contains("Place this minion") }
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
            return when (itemName) {
                "Kuudra Key" -> "§a1"
                "Hot Kuudra Key" -> "§22"
                "Burning Kuudra Key" -> "§e3"
                "Fiery Kuudra Key" -> "§64"
                "Infernal Kuudra Key" -> "§c5"
                else -> "§4?"
            }
        }

        if (SKILL_LEVEL.isSelected() &&
            InventoryUtils.openInventoryName() == "Your Skills" &&
            item.getLore().any { it.contains("Click to view!") }
        ) {
            if (CollectionAPI.isCollectionTier0(item.getLore())) return "0"
            val split = itemName.split(" ")
            if (!itemName.contains("Dungeon")) {
                val text = split.last()
                if (split.size < 2) return "0"
                return "" + text.romanToDecimalIfNecessary()
            }
        }

        if (COLLECTION_LEVEL.isSelected() && InventoryUtils.openInventoryName().endsWith(" Collections")) {
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
        }

        if (RANCHERS_BOOTS_SPEED.isSelected() && itemName.contains("Rancher's Boots")) {
            for (line in item.getLore()) {
                rancherBootsSpeedCapPattern.matchMatcher(line) {
                    return group("cap")
                }
            }
        }

        if (LARVA_HOOK.isSelected() && itemName.contains("Larva Hook")) {
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
        }

        if (DUNGEON_POTION_LEVEL.isSelected() && itemName.startsWith("Dungeon ") && itemName.contains(" Potion")) {
            item.name?.let {
                dungeonPotionPattern.matchMatcher(it.removeColor()) {
                    return when (val level = group("level").romanToDecimal()) {
                        in 1..2 -> "§f$level"
                        in 3..4 -> "§a$level"
                        in 5..6 -> "§9$level"
                        else -> "§5$level"
                    }
                }
            }
        }

        if (VACUUM_GARDEN.isSelected() && item.getInternalNameOrNull() in PestAPI.vacuumVariants) {
            for (line in item.getLore()) {
                gardenVacuumPatterm.matchMatcher(line) {
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
        }

        if (BOTTLE_OF_JYRRE.isSelected() && item.getInternalNameOrNull() == bottleOfJyrre) {
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

        if (SECRET_BINGO_DISCOVERY.isSelected() && chestName == "Bingo Card" && item.getLore().last() == "§aGOAL REACHED") {
            for (line in item.getLore()) {
                secretBingoDiscoveryPattern.matchMatcher(line) {
                    val nth = group("nth").formatNumber()
                    if (nth < 10000) return "${NumberUtil.format(nth)}"
                }
            }
        }

        return ""
    }

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
        event.move(11, "inventory.itemNumberAsStackSize", "inventory.itemNumberAsStackSize") { element ->
            ConfigUtils.migrateIntArrayListToEnumArrayList(element, ItemNumberEntry::class.java)
        }
    }

    fun ItemNumberEntry.isSelected() = config.itemNumberAsStackSize.contains(this)
}
