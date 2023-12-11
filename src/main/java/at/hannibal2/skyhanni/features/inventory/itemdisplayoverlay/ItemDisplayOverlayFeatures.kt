package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.ABIPHONE
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.ARMADILLO
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.BEASTMASTER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.BLOOD_GOD
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.BOTTLE_OF_JYRRE
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.CAMPFIRE
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.COMPACTOR_DELETOR
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.DUNGEON_HEAD_FLOOR_NUMBER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.DUNGEON_POTION_LEVEL
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.EDITION_NUMBER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.FRUIT_BOWL
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.KUUDRA_KEY
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.LARVA_HOOK
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.MASTER_SKULL_TIER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.MASTER_STAR_TIER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.MINION_TIER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.NECRONS_LADDER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.NEW_YEAR_CAKE
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.PET_LEVEL
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.RANCHERS_BOOTS_SPEED
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.SHREDDER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.SOULFLOW
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.STACKING_ENCHANTMENT
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.STORAGE_TIER
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.VACUUM_GARDEN
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig.ItemNumberEntry.YETI_ROD
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.pests.PestAPI
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAuctionNumber
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBloodGodKills
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBottleOfJyrreSeconds
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEdition
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFishesCaught
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFruitBowlNames
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getNecronHandlesFound
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPrehistoricEggBlocksWalked
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.matches
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.floor
import kotlin.math.log10

object ItemDisplayOverlayFeatures : AbstractStackSize() {

    // TODO USE SH-REPO
    private val config get() = configItemStackSize

    // TODO repo
    // private val xOutOfYNoColorRequiredPattern = ((".*: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?(?<total>[0-9]+).*").toPattern())
    private val masterStarPattern = (("(?<tier>[A-Z])+_MASTER_STAR").toPattern())
    private val rancherBootsSpeedCapLoreLinePattern = (("§7Current Speed Cap: §a(?<cap>.*)").toPattern())
    private val petLevelItemNamePattern = (("\\[Lvl (?<level>.*)] .*").toPattern())
    private val shredderBonusDamageLoreLinePattern = (("(§.)?Bonus Damage \\([0-9]+ cap\\): (§.)?(?<dmgbonus>[0-9]+)").toPattern())
    private val bottleOfJerryLoreLinePattern = (("(§.)?Intelligence Bonus: (§.)?(?<intelbonus>[0-9]+)").toPattern())
    private val gardenVacuumLoreLinePattern = (("§7Vacuum Bag: §6(?<amount>[0-9,]+) Pests?").toPattern())
    private val masterSkullInternalNamePattern = (("MASTER_SKULL_TIER_(?<tier>\\d)").toPattern())
    private val dungeonBossHeadInternalNamePattern = (("(GOLD(EN)?|DIAMOND)_(?<dungeonBoss>[\\w]+)_HEAD").toPattern())
    private val minionTierItemNamePattern = (("([\\w]+ Minion [\\w]+).*(?<!Recipes)\$").toPattern())
    private val enchantedItemSackItemNamePattern = (("Enchanted .*").toPattern())
    private val kuudraKeyItemNamePattern = (("([\\w ]+)?Kuudra Key").toPattern())
    private val kuudraKeyInternalNamePattern = (("KUUDRA_(?<tier>[\\w]+)_KEY").toPattern())
    private val larvaHookLoreLinePattern = (("§7§7You may harvest §6(?<amount>.).*").toPattern())
    private val armadilloRarityLorePattern = (("(§.)*(?<rarity>COMMON|UNCOMMON|RARE|EPIC|LEGENDARY)").toPattern())
    private val beastmasterCrestInternalNamePattern = (("BEASTMASTER_CREST_[\\w]*").toPattern())
    private val beastmasterCrestKillsProgressPattern = (("(§.)*Your kills: (§.)*(?<progress>[\\w,]+)(§.)*\\/(?<total>[\\w,]+)").toPattern())
    private val campfireTalismanTierInternalNamePattern = (("CAMPFIRE_TALISMAN_(?<tier>[\\d]+)").toPattern())
    private val isNotAuctionHouseChestNamePattern = (("^(?:(?!Auction).)*\$").toPattern())
    private val internalizedSoulflowLoreLinePattern = (("(§.)*Internalized: (§.)*(?<leading>[0-9]+)(?<trailing>,[0-9]{0,3})⸎ Soulflow").toPattern())
    private val storageChestInternalNamePattern = ((".*_ENCHANTED_CHEST").toPattern())
    private val storageChestItemNamePattern = ((".* Storage").toPattern())
    private val personalCompactorDeletorChestNamePattern = (("Personal (Compactor .*|Deletor .*)").toPattern())
    private val personalCompactorDeletorEnabledItemNamePattern = (("(§.)*(Compactor|Deletor) Currently O(?<toggle>FF|N)!").toPattern())
    private val personalCompactorDeletorInternalNamePattern = (("PERSONAL_(COMPACTOR|DELETOR)_(?<thousands>[\\w]+)(000)").toPattern())
    private val personalCompactorDeletorItemNamePattern = (("Personal (Compactor|Deletor) (?<thousands>[\\w]+)(000)").toPattern())
    private val abiphoneInternalNamePattern = (("ABIPHONE_.*").toPattern())
    private val doesNotIncludeDungeonStarsItemNamePattern = (("^(?:(?!✪).)*\$").toPattern())
    private val soulflowAccessoryInternalNamePattern = (("SOULFLOW_.*").toPattern())

    private val newYearCakeInternalName = (("NEW_YEAR_CAKE").asInternalName())
    private val ranchersBootsInternalName = (("RANCHERS_BOOTS").asInternalName())
    private val larvaHookInternalName = (("LARVA_HOOK").asInternalName())
    private val newBottleOfJyrreInternalName = (("NEW_BOTTLE_OF_JYRRE").asInternalName())
    private val legacyBottleOfJyrreInternalName = (("BOTTLE_OF_JYRRE").asInternalName())
    private val bloodGodCrestInternalName = (("BLOOD_GOD_CREST").asInternalName())
    private val necronsLadderInternalName = (("NECRONS_LADDER").asInternalName())
    private val prehistoricEggInternalName = (("PREHISTORIC_EGG").asInternalName())
    private val fruitBowlInternalName = (("FRUIT_BOWL").asInternalName())
    private val yetiRodInternalName = (("YETI_ROD").asInternalName())
    private val shredderInternalName = (("THE_SHREDDER").asInternalName())
    private val spookyPieInternalName = (("SPOOKY_PIE").asInternalName())
    private val potionInternalName = (("POTION").asInternalName())

    private val tieredEnchants = listOf(
        "compact",
        "cultivating",
        "champion",
        "expertise",
        "hecatomb",
    )
    private val sackPrefixes = arrayOf(
        "Large",
        "Medium",
        "Small",
        "Enchanted"
    )

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        super.onRenderItemTip(event)
    }

    override fun getStackTip(item: ItemStack): String {
        val itemName = item.cleanName()
        val internalName = item.getInternalName()
        val chestName = InventoryUtils.openInventoryName()
        val lore = item.getLore()

        return when {
            isMasterStar(internalName) -> getMasterStarTip(internalName)
            isMasterSkull(internalName) -> getMasterSkullTip(internalName)
            isDungeonHead(internalName) -> getDungeonHeadTip(internalName)
            isNewYearCake(internalName) -> getNewYearCakeTip(item)
            isPet(itemName, chestName) -> getPetTip(itemName)
            isMinionTier(itemName) -> getMinionTierTip(itemName, lore)
            isSack(item) -> getSackTip(itemName)
            isKuudraKey(internalName, itemName) -> getKuudraKeyTip(internalName)
            isRanchersBoots(internalName) -> getRanchersBootsTip(lore)
            isLarvaHook(internalName) -> getLarvaHookTip(lore)
            isDungeonPotion(item, internalName) -> getDungeonPotionTip(item)
            isVacuumGarden(item) -> getVacuumGardenTip(lore)
            isBottleOfJyrre(internalName) -> getBottleOfJyrreTip(item)
            isLegacyBottleOfJyrre(internalName) -> getLegacyBottleOfJyrreTip(lore)
            isEditionNumber(item) -> getEditionNumberTip(item)
            isAuctionNumber(item) -> getAuctionNumberTip(item)
            isSoulflowItem(internalName) -> getSoulflowTip(item, chestName)
            isBloodGodCrest(internalName) -> getBloodGodCrestTip(item)
            isNecronsLadder(internalName) -> getNecronsLadderTip(item)
            isArmadillo(internalName) -> getArmadilloTip(item)
            isCampfireAccessory(internalName) -> getCampfireTip(internalName)
            isFruitBowl(internalName) -> getFruitBowlTip(item)
            isBeastmaster(internalName) -> getBeastmasterTip(lore)
            isYetiRod(internalName) -> getYetiRodTip(item)
            isShredder(internalName) -> getShredderTip(lore)
            isMinionStorage(item) -> getMinionStorageTip(itemName)
            isCompactorOrDeletorItem(internalName, itemName) -> getCompactorOrDeletorItemTip(itemName)
            isCompactorOrDeletorChest(chestName, itemName) -> getCompactorOrDeletorStatusTip(itemName)
            isAbiphone(internalName) -> getAbiphoneTip(internalName)
            isQualifiedForStacking(itemName) -> getStackingEnchantmentTierTip(item)
            isSpookyPie(internalName) -> getSpookyPieTip(item)
            else -> ""
        }
    }

    // <editor-fold desc="Stack Size Implementations">
    private fun isMasterStar(internalName: NEUInternalName): Boolean = MASTER_STAR_TIER.isSelected() && masterStarPattern.matches(internalName)
    private fun getMasterStarTip(internalName: NEUInternalName): String {
        var tier = ""
        masterStarPattern.matchMatcher(internalName.asString()) { tier = group("tier") }
        return when (tier) {
            "FIRST" -> "1"
            "SECOND" -> "2"
            "THIRD" -> "3"
            "FOURTH" -> "4"
            "FIFTH" -> "5"
            else -> ""
        }
    }

    private fun isMasterSkull(internalName: NEUInternalName): Boolean = MASTER_SKULL_TIER.isSelected() && masterSkullInternalNamePattern.matches(internalName)
    private fun getMasterSkullTip(internalName: NEUInternalName): String {
        masterSkullInternalNamePattern.matchMatcher(internalName.asString()) { return group("tier") }
        return ""
    }

    private fun isDungeonHead(internalName: NEUInternalName): Boolean = DUNGEON_HEAD_FLOOR_NUMBER.isSelected() && dungeonBossHeadInternalNamePattern.matches(internalName)
    private fun getDungeonHeadTip(internalName: NEUInternalName): String {
        dungeonBossHeadInternalNamePattern.matchMatcher(internalName.asString()) {
            return when (group("dungeonBoss")) {
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
        return ""
    }

    private fun isNewYearCake(internalName: NEUInternalName): Boolean = NEW_YEAR_CAKE.isSelected() && internalName == newYearCakeInternalName
    private fun getNewYearCakeTip(item: ItemStack): String {
        return "§b${item.getExtraAttributes()?.getInteger("new_years_cake")}"
    }

    private fun isSpookyPie(internalName: NEUInternalName): Boolean = NEW_YEAR_CAKE.isSelected() && internalName == spookyPieInternalName
    private fun getSpookyPieTip(item: ItemStack): String {
        return "§b${(item.getExtraAttributes()?.getInteger("new_years_cake")?.plus(1))}"
    }

    private fun isPet(itemName: String, chestName: String): Boolean = PET_LEVEL.isSelected() && !chestName.endsWith("Sea Creature Guide") && ItemUtils.isPet(itemName)
    private fun getPetTip(itemName: String): String {
        petLevelItemNamePattern.matchMatcher(itemName) {
            val rawLevel = group("level")
            val level = rawLevel.toIntOrNull()
                ?: throw IllegalStateException("pet level not found for item name '$itemName'")
            if (level != ItemUtils.maxPetLevel(itemName)) {
                return "$level"
            }
        }
        return ""
    }

    private fun isMinionTier(itemName: String): Boolean = MINION_TIER.isSelected() && minionTierItemNamePattern.matches(itemName)
    private fun getMinionTierTip(itemName: String, lore: List<String>): String {
        if (lore.any { (it == ("§7Place this minion and it will")) }) {
            return "${itemName.split(" ").last().romanToDecimal()}"
        }
        return ""
    }

    private fun isSack(item: ItemStack): Boolean = config.displaySackName && ItemUtils.isSack(item)
    private fun getSackTip(itemName: String): String {
        var colorCode = ""
        enchantedItemSackItemNamePattern.matchMatcher(itemName) { colorCode = "§5" }
        return "$colorCode${grabSackName(itemName).substring(0, 2)}"
    }
    private fun grabSackName(name: String): String {
        val text = name.split(" ").first()
        for (line in sackPrefixes) {
            if (text == line) return grabSackName(name.substring(text.length + 1))
        }
        return text
    }

    private fun isKuudraKey(internalName: NEUInternalName, itemName: String): Boolean = KUUDRA_KEY.isSelected() && kuudraKeyInternalNamePattern.matches(internalName) && kuudraKeyItemNamePattern.matches(itemName)
    private fun getKuudraKeyTip(internalName: NEUInternalName): String {
        kuudraKeyInternalNamePattern.matchMatcher(internalName.asString()) {
            return when (group("tier")) {
                "TIER" -> "§a1"
                "HOT_TIER" -> "§22"
                "BURNING_TIER" -> "§e3"
                "FIERY_TIER" -> "§64"
                "INFERNAL_TIER" -> "§c5"
                else -> "§4?"
            }
        }
        return ""
    }

    private fun isRanchersBoots(internalName: NEUInternalName): Boolean = RANCHERS_BOOTS_SPEED.isSelected() && internalName == ranchersBootsInternalName
    private fun getRanchersBootsTip(lore: List<String>): String {
        for (line in lore) {
            rancherBootsSpeedCapLoreLinePattern.matchMatcher(line) {
                return group("cap")
            }
        }
        return ""
    }

    private fun isLarvaHook(internalName: NEUInternalName): Boolean = LARVA_HOOK.isSelected() && internalName == larvaHookInternalName
    private fun getLarvaHookTip(lore: List<String>): String {
        for (line in lore) {
            larvaHookLoreLinePattern.matchMatcher(line) {
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

    private fun isDungeonPotion(item: ItemStack, internalName: NEUInternalName): Boolean = DUNGEON_POTION_LEVEL.isSelected() && internalName == potionInternalName && item.getExtraAttributes()?.getString("potion_name") == "Dungeon"
    private fun getDungeonPotionTip(item: ItemStack): String {
        val potionLevel = item.getExtraAttributes()?.getInteger("potion_level") ?: return ""
        return when (potionLevel) {
            in 1..2 -> "§f$potionLevel"
            in 3..4 -> "§a$potionLevel"
            in 5..6 -> "§9$potionLevel"
            else -> "§5$potionLevel"
        }
    }

    private fun isVacuumGarden(item: ItemStack): Boolean = VACUUM_GARDEN.isSelected() && item.getInternalNameOrNull() in PestAPI.vacuumVariants
    private fun getVacuumGardenTip(lore: List<String>): String {
        for (line in lore) {
            gardenVacuumLoreLinePattern.matchMatcher(line) {
                val pests = group("amount").formatNumber()
                return if (config.vacuumBagCap) {
                    if (pests > 39) "§640" else "$pests"
                } else {
                    when {
                        pests < 40 -> "$pests"
                        pests < 1_000 -> "§6$pests"
                        pests < 100_000 -> "§c${pests / 1_000}k"
                        else -> "§4${pests / 100_000 / 10.0}m" //4 is a darker shade of color code c
                    }
                }
            }
        }
        return ""
    }
    private fun isBottleOfJyrre(internalName: NEUInternalName): Boolean = BOTTLE_OF_JYRRE.isSelected() && internalName == newBottleOfJyrreInternalName
    private fun getBottleOfJyrreTip(item: ItemStack): String {
        val seconds = item.getBottleOfJyrreSeconds() ?: 0
        return "§a${(seconds / 3600)}"
    }

    private fun isLegacyBottleOfJyrre(internalName: NEUInternalName): Boolean = BOTTLE_OF_JYRRE.isSelected() && internalName == legacyBottleOfJyrreInternalName
    private fun getLegacyBottleOfJyrreTip(lore: List<String>): String {
        for (line in lore) {
            bottleOfJerryLoreLinePattern.matchMatcher(line) { return "§2${group("intelbonus")}" } //2 is a darker shade of color code a
        }
        return ""
    }

    private fun isEditionNumber(item: ItemStack): Boolean = EDITION_NUMBER.isSelected() && item.getEdition() != null
    private fun getEditionNumberTip(item: ItemStack): String {
        item.getEdition()?.let { if (it < 1_000) { return "§6$it" } }
        return ""
    }

    private fun isAuctionNumber(item: ItemStack): Boolean = EDITION_NUMBER.isSelected() && item.getAuctionNumber() != null
    private fun getAuctionNumberTip(item: ItemStack): String {
        item.getAuctionNumber()?.let { if (it < 1_000) { return "§6$it" } }
        return ""
    }

    private fun isSoulflowItem(internalName: NEUInternalName): Boolean = SOULFLOW.isSelected() && soulflowAccessoryInternalNamePattern.matches(internalName)
    private fun getSoulflowTip(item: ItemStack, chestName: String): String {
        isNotAuctionHouseChestNamePattern.matchMatcher(chestName) {
            internalizedSoulflowLoreLinePattern.matchMatcher(item.getLore().first()) {
                return NumberUtil.format("${group("leading")}${group("trailing")}".formatNumber())
            }
        }
        return ""
    }

    private fun isBloodGodCrest(internalName: NEUInternalName): Boolean = BLOOD_GOD.isSelected() && internalName == bloodGodCrestInternalName
    private fun getBloodGodCrestTip(item: ItemStack): String {
        item.getBloodGodKills()?.let { return ("${floor(log10(it.toDouble())) + 1}") }
        return ""
    }

    private fun isNecronsLadder(internalName: NEUInternalName): Boolean = NECRONS_LADDER.isSelected() && internalName == necronsLadderInternalName
    private fun getNecronsLadderTip(item: ItemStack): String {
        item.getNecronHandlesFound().let { return "$it" }
    }

    private fun isArmadillo(internalName: NEUInternalName): Boolean = ARMADILLO.isSelected() && internalName == prehistoricEggInternalName
    private fun getArmadilloTip(item: ItemStack): String {
        val lore = item.getLore()
        if (lore.lastOrNull() == null) return ""
        val blocksWalked = item.getPrehistoricEggBlocksWalked() ?: return ""
        var rarity = ""
        for (line in lore) {
            armadilloRarityLorePattern.matchMatcher(line) { rarity = group("rarity") }
        }
        val threshold = when (rarity) {
            "COMMMON" -> 4_000F
            "UNCOMMON" -> 10_000F
            "RARE" -> 20_000F
            "EPIC" -> 40_000F
            "LEGENDARY" -> 100_000F
            else -> 1F
        }
        return if (threshold != 1F) { "${((blocksWalked.toFloat() / threshold) * 100).toInt()}" } else ""
    }

    private fun isCampfireAccessory(internalName: NEUInternalName): Boolean = CAMPFIRE.isSelected() && campfireTalismanTierInternalNamePattern.matches(internalName)
    private fun getCampfireTip(internalName: NEUInternalName): String {
        campfireTalismanTierInternalNamePattern.matchMatcher(internalName.asString()) { return "${(group("tier").toInt() + 1)}" }
        return ""
    }

    private fun isFruitBowl(internalName: NEUInternalName): Boolean = FRUIT_BOWL.isSelected() && internalName == fruitBowlInternalName
    private fun getFruitBowlTip(item: ItemStack): String {
        item.getFruitBowlNames().let { return "${it?.size}" }
    }

    private fun isBeastmaster(internalName: NEUInternalName): Boolean = BEASTMASTER.isSelected() && beastmasterCrestInternalNamePattern.matches(internalName)
    private fun getBeastmasterTip(lore: List<String>): String {
        for (line in lore) {
            //§7Your kills: §21,581§8/2,500
            /* if (line.contains("Your kills: ")) {
                val num = line.removeColor().replace("Your kills: ", "").replace(",", "").split("/").first()
                val denom = line.removeColor().replace("Your kills: ", "").replace(",", "").split("/").last()
                return (((num.toFloat() / denom.toFloat()) * 100).toString().take(2))
            } */
            beastmasterCrestKillsProgressPattern.matchMatcher(line) {
                return "${((group("progress").formatNumber().toFloat() / group("total").formatNumber().toFloat()) * 100)}".take(2)
            }
        }
        return ""
    }

    private fun isYetiRod(internalName: NEUInternalName): Boolean = YETI_ROD.isSelected() && internalName == yetiRodInternalName
    private fun getYetiRodTip(item: ItemStack): String {
        val kills = "${item.getFishesCaught()}"
        if (kills == "null") { return "" }
        return if (kills.length >= 4) {
            "100"
        } else {
            (kills.dropLast(1))
        }
    }

    private fun isShredder(internalName: NEUInternalName): Boolean = SHREDDER.isSelected() && internalName == shredderInternalName
    private fun getShredderTip(lore: List<String>): String {
        for (line in lore) {
            shredderBonusDamageLoreLinePattern.matchMatcher(line) {
                return group("dmgbonus")
            }
        }
        return ""
    }

    private fun isMinionStorage(item: ItemStack): Boolean = STORAGE_TIER.isSelected() && storageChestInternalNamePattern.matches(item.getInternalName()) && storageChestItemNamePattern.matches(item.cleanName())
    private fun getMinionStorageTip(itemName: String): String {
        val numSlots = when (itemName) {
            ("Small Storage") -> "3"
            ("Medium Storage") -> "9"
            ("Large Storage") -> "15"
            ("X-Large Storage") -> "21"
            ("XX-Large Storage") -> "27"
            else -> ""
        }
        val colorCode = when (itemName) {
            ("Small Storage") -> "f"
            ("Medium Storage") -> "a"
            ("Large Storage") -> "9"
            ("X-Large Storage") -> "c"
            ("XX-Large Storage") -> "c"
            else -> ""
        }
        return "§$colorCode$numSlots"
    }

    private fun isCompactorOrDeletorItem(internalName: NEUInternalName, itemName: String): Boolean = COMPACTOR_DELETOR.isSelected() && personalCompactorDeletorInternalNamePattern.matches(internalName) && personalCompactorDeletorItemNamePattern.matches(itemName)
    private fun getCompactorOrDeletorItemTip(itemName: String): String {
        personalCompactorDeletorItemNamePattern.matchMatcher(itemName) {
            return "${group("thousands")}K"
        }
        return ""
    }

    private fun isCompactorOrDeletorChest(chestName: String, itemName: String): Boolean = COMPACTOR_DELETOR.isSelected() && personalCompactorDeletorChestNamePattern.matches(chestName) && personalCompactorDeletorEnabledItemNamePattern.matches(itemName)
    private fun getCompactorOrDeletorStatusTip(itemName: String): String {
        personalCompactorDeletorEnabledItemNamePattern.matchMatcher(itemName) {
            return when (group("toggle")) {
                "N" ->"§a✔"
                else -> "§c§l✖"
            }
        }
        return ""
    }

    private fun isAbiphone(internalName: NEUInternalName): Boolean = ABIPHONE.isSelected() && abiphoneInternalNamePattern.matches(internalName)
    private fun getAbiphoneTip(internalName: NEUInternalName): String {
        return when (internalName.asString()) {
            "ABIPHONE_X_PLUS" -> "X"
            "ABIPHONE_X_PLUS_SPECIAL_EDITION" -> "X§b§zSE"
            "ABIPHONE_XI_ULTRA" -> "11"
            "ABIPHONE_XI_ULTRA_STYLE" -> "11§b§zS"
            "ABIPHONE_XII_MEGA" -> "12"
            "ABIPHONE_XII_MEGA_COLOR" -> "12§b§zC"
            "ABIPHONE_XIII_PRO" -> "13"
            "ABIPHONE_XIII_PRO_GIGA" -> "13§b§zG"
            "ABIPHONE_XIV_ENORMOUS" -> "14"
            "ABIPHONE_XIV_ENORMOUS_BLACK" -> "§714"
            "ABIPHONE_XIV_ENORMOUS_PURPLE" -> "§714"
            "ABIPHONE_FLIP_DRAGON" -> "Fl§b§zD"
            "ABIPHONE_FLIP_NUCLEUS" -> "Fl§b§zN"
            "ABIPHONE_FLIP_VOLCANO" -> "Fl§b§zV"
            else -> ""
        }
    }

    private fun isQualifiedForStacking(itemName: String): Boolean = STACKING_ENCHANTMENT.isSelected() && doesNotIncludeDungeonStarsItemNamePattern.matches(itemName)
    private fun getStackingEnchantmentTierTip(item: ItemStack): String {
        val possibleEnchantments = item.getEnchantments()
        if (possibleEnchantments != null) {
            for (enchant in tieredEnchants) {
                if (possibleEnchantments[enchant] != null && possibleEnchantments[enchant] != -1) {
                    return "${possibleEnchantments[enchant]}"
                }
            }
        }
        return ""
    }
    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(11, "inventory.itemNumberAsStackSize", "inventory.itemNumberAsStackSize") { element ->
            ConfigUtils.migrateIntArrayListToEnumArrayList(element, ItemNumberEntry::class.java)
        }
        event.move(12, "inventory.itemNumberAsStackSize", "inventory.stackSize.itemNumberAsStackSize")
    }

    fun ItemNumberEntry.isSelected(): Boolean = config.stackSize.itemNumberAsStackSize.contains(this)
}