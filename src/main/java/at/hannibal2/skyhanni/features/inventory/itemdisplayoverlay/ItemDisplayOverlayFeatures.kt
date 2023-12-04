package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.BOTTLE_OF_JYRRE
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.DUNGEON_HEAD_FLOOR_NUMBER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.DUNGEON_POTION_LEVEL
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.EDITION_NUMBER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.KUUDRA_KEY
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.LARVA_HOOK
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.MASTER_SKULL_TIER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.MASTER_STAR_TIER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.MINION_TIER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.NEW_YEAR_CAKE
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.PET_LEVEL
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.RANCHERS_BOOTS_SPEED
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.VACUUM_GARDEN
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
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBottleOfJyrreSeconds
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEdition
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ItemDisplayOverlayFeatures {
    // TODO USE SH-REPO
    private val config get() = SkyHanniMod.feature.inventory

    // TODO repo
    // private val xOutOfYNoColorRequiredPattern = ((".*: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?(?<total>[0-9]+).*").toPattern())
    private val rancherBootsSpeedCapLoreLinePattern = (("§7Current Speed Cap: §a(?<cap>.*)").toPattern())
    private val petLevelItemNamePattern = (("\\[Lvl (?<level>.*)] .*").toPattern())
    private val shredderBonusDamageLoreLinePattern = (("(§.)?Bonus Damage \\([0-9]+ cap\\): (§.)?(?<dmgbonus>[0-9]+)").toPattern())
    private val bottleOfJerryLoreLinePattern = (("(§.)?Intelligence Bonus: (§.)?(?<intelbonus>[0-9]+)").toPattern())
    private val gardenVacuumLoreLinePattern = (("§7Vacuum Bag: §6(?<amount>[0-9,]+) Pests?").toPattern())
    private val masterSkullItemNamePattern = (("(.*)Master Skull - Tier (?<tier>.+)").toPattern())
    private val dungeonBossHeadInternalNamePattern = (("(GOLD(EN)?|DIAMOND)_(?<dungeonBoss>[\\w]+)_HEAD").toPattern())
    private val newYearCakeSpookyPieYearItemNamePattern = (("(New Year Cake|Spooky Pie) \\(Year (?<year>[\\w]+)\\)").toPattern())
    private val minionTierItemNamePattern = (("([\\w]+ Minion [\\w]+).*(?<!Recipes)\$").toPattern())
    private val enchantedItemSackItemNamePattern = (("Enchanted .*").toPattern())
    private val kuudraKeyItemNamePattern = (("([\\w ]+)?Kuudra Key").toPattern())
    private val kuudraKeyInternalNamePattern = (("KUUDRA_(?<tier>[\\w]+)_KEY").toPattern())
    private val larvaHookLoreLinePattern = (("§7§7You may harvest §6(?<amount>.).*").toPattern())
    private val dungeonLevelPotionItemNamePattern = (("Dungeon (?<level>.*) Potion").toPattern())
    private val armadilloRarityLorePattern = (("(§.)*(?<rarity>COMMON|UNCOMMON|RARE|EPIC|LEGENDARY)").toPattern())
    private val beastmasterCrestInternalNamePattern = (("BEASTMASTER_CREST_[\\w]*").toPattern())
    private val beastmasterCrestKillsProgressPattern = (("(§.)*Your kills: (§.)*(?<progress>[\\w,]+)(§.)*\\/(?<total>[\\w,]+)").toPattern())
    private val campfireTalismanTierInternalNamePattern = (("CAMPFIRE_TALISMAN_(?<tier>[\\d]+)").toPattern())
    private val auctionHouseChestNamePattern = (("^(?:(?!Auction).)*\$").toPattern())
    private val internalizedSoulflowLoreLinePattern = (("(§.)*Internalized: (§.)*(?<leading>[0-9]+)(?<trailing>,[0-9]{0,3})⸎ Soulflow").toPattern())
    private val storageChestInternalNamePattern = ((".*_ENCHANTED_CHEST").toPattern())
    private val storageChestItemNamePattern = ((".* Storage").toPattern())
    private val personalCompactorDeletorChestNamePattern = (("Personal (Compactor .*|Deletor .*)").toPattern())
    private val personalCompactorEnabledItemNamePattern = (("(§.)*(Compactor|Deletor) Currently O(?<toggle>FF|N)!").toPattern())
    private val personalCompactorDeletorInternalNamePattern = (("PERSONAL_(COMPACTOR|DELETOR)_(?<thousands>[\\w]+)(000)").toPattern())
    private val personalCompactorDeletorItemNamePattern = (("Personal (Compactor|Deletor) (?<thousands>[\\w]+)(000)").toPattern())
    private val abiphoneInternalNamePattern = (("ABIPHONE_.*").toPattern())
    private val auctionNumberLorePattern = (("§8Auction .*").toPattern())
    private val editionNumberLorePattern = (("§8Edition .*").toPattern())
    private val doesNotIncludeDungeonStarsItemNamePattern = (("^(?:(?!✪).)*\$").toPattern())
    private val tieredEnchants = listOf(
        "compact",
        "cultivating",
        "champion",
        "expertise",
        "hecatomb",
    )
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

        if (MASTER_SKULL_TIER.isSelected()) {
            masterSkullItemNamePattern.matchMatcher(itemName) {
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

        if (NEW_YEAR_CAKE.isSelected() && itemName.startsWith("New Year Cake (")) {
            return "§b" + itemName.between("(Year ", ")")
        }

        if (PET_LEVEL.isSelected()) {
            val chestName = InventoryUtils.openInventoryName()
            if (!chestName.endsWith("Sea Creature Guide") && ItemUtils.isPet(itemName)) {
                petLevelItemNamePattern.matchMatcher(itemName) {
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

        if (RANCHERS_BOOTS_SPEED.isSelected() && itemName.contains("Rancher's Boots")) {
            for (line in item.getLore()) {
                rancherBootsSpeedCapLoreLinePattern.matchMatcher(line) {
                    return group("cap")
                }
            }
        }

        if (LARVA_HOOK.isSelected() && itemName.contains("Larva Hook")) {
            for (line in item.getLore()) {
                larvaHookLoreLinePattern.matchMatcher(line) {
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
                dungeonLevelPotionItemNamePattern.matchMatcher(it.removeColor()) {
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
                gardenVacuumLoreLinePattern.matchMatcher(line) {
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

        return ""
    }

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
        event.move(12, "inventory.itemNumberAsStackSize", "inventory.stackSize.itemNumber")
    }

    fun ItemNumberEntry.isSelected() = config.stackSize.itemNumberAsStackSize.contains(this)
}
