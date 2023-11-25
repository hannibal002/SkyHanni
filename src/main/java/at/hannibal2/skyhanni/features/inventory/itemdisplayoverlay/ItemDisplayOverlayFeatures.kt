package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.countDigits
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAuctionNumber
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBloodGodKills
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEdition
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFruitBowlNames
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getNecronHandlesFound
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPrehistoricEggBlocksWalked
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getYetiRodFishesCaught
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemDisplayOverlayFeatures {
    private val rancherBootsSpeedCapPattern = "§7Current Speed Cap: §a(?<cap>.*)".toPattern()
    private val petLevelPattern = "\\[Lvl (?<level>.*)] .*".toPattern()
    private val whyHaventTheAdminsAddedShredderBonusDamageInfoToItemNBTDataYetPattern = "(§.)?Bonus Damage \\([0-9]+ cap\\): (§.)?(?<dmgbonus>[0-9]+)".toPattern()
    private val iReallyHateTheBottleOfJerryPattern = "(§.)?Intelligence Bonus: (§.)?(?<intelbonus>[0-9]+)".toPattern()
    private val xOutOfYNoColorRequiredPattern = ".*: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?(?<total>[0-9]+).*".toPattern()
    private val gardenVacuumPattern = "§7Vacuum Bag: §6(?<amount>[0-9,]+) Pests?".toPattern()
    private val garenVacuumVariants = listOf(
        "SKYMART_VACUUM".asInternalName(),
        "SKYMART_TURBO_VACUUM".asInternalName(),
        "SKYMART_HYPER_VACUUM".asInternalName(),
        "INFINI_VACUUM".asInternalName(),
        "INFINI_VACUUM_HOOVERIUS".asInternalName(),
    )
    private val tieredEnchants = listOf(
        "compact",
        "cultivating",
        "champion",
        "expertise",
        "hecatomb",
    )

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.itemNumber.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.itemNumber
        val chestName = InventoryUtils.openInventoryName()
        val internalName = item.getInternalName().asString()

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.MASTER_STAR)) {
            when (itemName) {
                "First Master Star" -> return "1"
                "Second Master Star" -> return "2"
                "Third Master Star" -> return "3"
                "Fourth Master Star" -> return "4"
                "Fifth Master Star" -> return "5"
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.MASTER_SKULL)) {
            (("(.*)Master Skull - Tier (?<tier>.+)").toPattern()).matchMatcher(itemName) {
                return group("tier")
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.DUNGEON_HEAD_FLOOR_NUMBER)) {
            (("(GOLD(EN)?|DIAMOND)_(?<dungeonBoss>[\\w]+)_HEAD").toPattern()).matchMatcher(internalName) {
                return when (group("dungeonBoss")) {
                    ("BONZO") -> "1"
                    ("SCARF") -> "2"
                    ("PROFESSOR") -> "3"
                    ("THORN") -> "4"
                    ("LIVID") -> "5"
                    ("SADAN") -> "6"
                    ("NECRON") -> "7"
                    else -> "?"
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.SKYBLOCK_YEAR)) {
            (("(New Year Cake|Spooky Pie) \\(Year (?<year>[\\w]+)\\)").toPattern()).matchMatcher(itemName) {
                return "§b${group("year")}"
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.PET_LVL) && (!chestName.endsWith("Sea Creature Guide")) && (ItemUtils.isPet(itemName))) {
            petLevelPattern.matchMatcher(itemName) {
                val level = group("level").toInt()
                if (level != ItemUtils.maxPetLevel(itemName)) {
                    return "$level"
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.MINION_TIER)) {
            (("([\\w]+ Minion [\\w]+).*(?<!Recipes)\$").toPattern()).matchMatcher(itemName) {
                for (line in item.getLore()) {
                    if (line.equals("§7Place this minion and it will")) {
                        return itemName.split(" ").last().romanToDecimal().toString()
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.displaySackName && ItemUtils.isSack(item)) {
            val sackName = grabSackName(itemName)
            //return (if (itemName.contains("Enchanted")) "§5" else "") + sackName.substring(0, 2)
            (("Enchanted .*").toPattern()).matchMatcher(itemName) {
                return "§5${sackName.substring(0, 2)}"
            }
            return "${sackName.substring(0, 2)}"
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.KUUDRA)) {
            (("([\\w ]+)?Kuudra Key").toPattern()).matchMatcher(itemName) {
                (("KUUDRA_(?<tier>[\\w]+)_KEY").toPattern()).matchMatcher(internalName) {
                    return when (group("tier")) {
                        "TIER" -> "§a1"
                        "HOT_TIER" -> "§22"
                        "BURNING_TIER" -> "§e3"
                        "FIERY_TIER" -> "§64"
                        "INFERNAL_TIER" -> "§c5"
                        else -> "§4?"
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.RANCHER_SPEED) && internalName == ("RANCHERS_BOOTS")) {
            for (line in item.getLore()) {
                rancherBootsSpeedCapPattern.matchMatcher(line) {
                    return group("cap")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.LARVA_HOOK) && internalName == ("LARVA_HOOK")) {
            for (line in item.getLore()) {
                "§7§7You may harvest §6(?<amount>.).*".toPattern().matchMatcher(line) {
                    val amount = group("amount").toInt()
                    return when {
                        amount > 4 -> "§a$amount"
                        amount > 2 -> "§e$amount"
                        else -> "§c$amount"
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.DUNGEON_POTION_LEVEL)) {
            ("Dungeon (?<level>.*) Potion".toPattern()).matchMatcher(itemName) {
                return when (val level = group("level").romanToDecimal()) {
                    in 1..2 -> "§f$level"
                    in 3..4 -> "§a$level"
                    in 5..6 -> "§9$level"
                    else -> "§5$level"
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.ARMADILLO) && (itemName == "Prehistoric Egg")) {
            val lore = item.getLore()
            if (lore.lastOrNull() == null) return ""
            val blocksWalked = item.getPrehistoricEggBlocksWalked() ?: return ""
            var rarity = ""
            for (line in lore) {
                (("(§.)*(?<rarity>COMMON|UNCOMMON|RARE|EPIC|LEGENDARY)").toPattern()).matchMatcher(line) {
                    rarity = group("rarity")
                }
            }
            val threshold = when (rarity) {
                "COMMMON" -> 4000F
                "UNCOMMON" -> 10000F
                "RARE" -> 20000F
                "EPIC" -> 40000F
                "LEGENDARY" -> 100000F
                else -> 1F
            }
            if (threshold != 1F) { return ((blocksWalked.toFloat() / threshold) * 100).toInt().toString() }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.NECRONS_LADDER) && internalName == ("NECRONS_LADDER")) {
            return "${item.getNecronHandlesFound() ?: "" }"
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.FRUIT_BOWL) && internalName == ("FRUIT_BOWL")) {
            return "${item.getFruitBowlNames()?.size}"
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.BEASTMASTER)) {
            (("BEASTMASTER_CREST_[\\w]*").toPattern()).matchMatcher(internalName) {
                for (line in item.getLore()) {
                    //§7Your kills: §21,581§8/2,500
                    /* if (line.contains("Your kills: ")) {
                        val num = line.removeColor().replace("Your kills: ", "").replace(",", "").split("/").first()
                        val denom = line.removeColor().replace("Your kills: ", "").replace(",", "").split("/").last()
                        return (((num.toFloat() / denom.toFloat()) * 100).toString().take(2))
                    } */
                    (("(§.)*Your kills: (§.)*(?<progress>[\\w,]+)(§.)*\\/(?<total>[\\w,]+)").toPattern()).matchMatcher(line) {
                        val progress = group("progress").formatNumber().toFloat()
                        val total = group("total").formatNumber().toFloat()
                        return "${((progress / total) * 100)}".take(2)
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.CAMPFIRE)) {
            (("CAMPFIRE_TALISMAN_(?<tier>[\\d]+)").toPattern()).matchMatcher(internalName) {
                return "${(group("tier").toInt() + 1)}"
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.BLOOD_GOD) && internalName == ("BLOOD_GOD_CREST")) {
            return ("${item.getBloodGodKills()?.let { countDigits(it) }}")
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.YETI_ROD) && internalName == ("YETI_ROD")) {
            val kills = "${item.getYetiRodFishesCaught()}"
            if (kills == "null") { return "" }
            if (kills.length >= 4){ return "100" }
            else { return (kills.dropLast(1)) }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.SHREDDER) && internalName == ("THE_SHREDDER")) {
            val lore = item.getLore()
            for (line in lore) {
                whyHaventTheAdminsAddedShredderBonusDamageInfoToItemNBTDataYetPattern.matchMatcher(line) {
                    return group("dmgbonus")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.JYRRE) && internalName == ("BOTTLE_OF_JYRRE")) {
            val lore = item.getLore()
            for (line in lore) {
                iReallyHateTheBottleOfJerryPattern.matchMatcher(line) {
                    return group("intelbonus")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.SOULFLOW) && internalName.startsWith("SOULFLOW_")) {
            //§7Internalized: §316,493⸎ Soulflow
            //Internalized: 16,493⸎ Soulflow
            //!(chestName.contains("Auction"))
            (("^(?:(?!Auction).)*\$").toPattern()).matchMatcher(chestName) {
                val line = item.getLore().first()
                (("(§.)*Internalized: (§.)*(?<leading>[0-9]+)(?<trailing>,[0-9]{0,3})⸎ Soulflow").toPattern()).matchMatcher(line) {
                    val soulflowCount = "${group("leading")}${group("trailing")}"
                    val soulflowCountWithoutCommas = "${soulflowCount.formatNumber()}"
                    val usefulAsString =  group("leading")
                    val suffix = when (soulflowCountWithoutCommas.length) {
                        in 1..3 -> ""
                        in 4..6 -> "k"
                        in 7..9 -> "M"
                        in 10..12 -> "B"
                        in 13..15 -> "T"
                        else -> "§b§z:)"
                    }
                    if (usefulAsString.isEmpty()) return ""
                    if (suffix == "§b§z:)") return suffix
                    else return "$usefulAsString$suffix"
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.CRUX) && internalName.startsWith("CRUX_")) {
            //  §21 §2Shy§7: §e14§7/§a25 §7kills
            val lore = item.getLore()
            var numberOfLines = 0 //"zoMG ERY WHY NOT JUST REPLACE "CRUX_TALISMAN"?!?!?" yeah i considered that too but then realized hypixel might change that one day
            var killCount = 0
            val currentKillThresholdPerMobFamily = 100 //change this in case hypixel increases the kill count to max a crux accessory
            for (line in lore) {
                ((".*: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?(?<total>[0-9]+) (§.)?kills.*").toPattern()).matchMatcher(line) {
                    val mobSpecificKillCount = group("useful").toIntOrNull() ?: 0
                    killCount += mobSpecificKillCount
                    numberOfLines++
                }
                val totalKillsNecessary = currentKillThresholdPerMobFamily * numberOfLines
                val percent = (((killCount.toFloat()) / (totalKillsNecessary.toFloat())) * 100) //keep this line here for easier debugging
                return percent.toInt().toString()
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.STORAGE_TIER)) {
            //internalName.endsWith("_ENCHANTED_CHEST")
            //itemName.endsWith(" Storage")
            ((".*_ENCHANTED_CHEST").toPattern()).matchMatcher(internalName) {
                ((".* Storage").toPattern()).matchMatcher(itemName) {
                    var colorCode = item.name ?: return ""
                    colorCode = colorCode.take(2)
                    val numSlots = when (itemName) {
                        ("Small Storage") -> "3"
                        ("Medium Storage") -> "9"
                        ("Large Storage") -> "15"
                        ("X-Large Storage") -> "21"
                        ("XX-Large Storage") -> "27"
                        else -> ""
                    }
                    return "$colorCode$numSlots"
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.COMPACTOR_DELETOR)) {
            (("Personal (Compactor .*|Deletor .*)").toPattern()).matchMatcher(chestName) {
                //§aCompactor Currently OFF!
                //§aCompactor Currently ON!
                /* if (itemName.contains(" Currently ")) {
                    return when (itemName.replace("Compactor ", "").replace("Deletor ", "")) {
                        "Currently OFF!" -> "§c§l✖"
                        "Currently ON!" -> "§a✔"
                        else -> ""
                    }
                } */
                (("(§.)*(Compactor|Deletor) Currently O(?<toggle>FF|N)!").toPattern()).matchMatcher(itemName) {
                    return when (group("toggle")) {
                        "N" ->"§a✔"
                        else -> "§c§l✖"
                    }
                }
            }
            (("PERSONAL_(COMPACTOR|DELETOR)_(?<thousands>[\\w]+)(000)").toPattern()).matchMatcher(internalName) {
                (("Personal (Compactor|Deletor) (?<thousands>[\\w]+)(000)").toPattern()).matchMatcher(itemName) {
                    return "${group("thousands")}K"
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.ABIPHONE)) {
            (("ABIPHONE_.*").toPattern()).matchMatcher(internalName) {
                return when (internalName) {
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
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.EDITION_AUCTION_NUMBER)) {
            var thatNumber = 0
            for (line in item.getLore()) {
                (("§8Auction .*").toPattern()).matchMatcher(line) {
                    thatNumber = item.getAuctionNumber() ?: 0
                }
                (("§8Edition .*").toPattern()).matchMatcher(line) {
                    thatNumber = item.getEdition() ?: 0
                }
            }
            if (thatNumber in 1..999) {
                return "$thatNumber"
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.STACKING_ENCHANTMENT)) {
            //itemName.contains("✪")
            (("^(?:(?!✪).)*\$").toPattern()).matchMatcher(itemName) {
                val possibleEnchantments = item.getEnchantments()
                if (possibleEnchantments != null) {
                    for (enchant in tieredEnchants) {
                        if (possibleEnchantments[enchant] != null && possibleEnchantments[enchant] != -1) {
                            return "${possibleEnchantments[enchant]}"
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeConfig.ItemNumber.VACCUM_PESTS)) {
            if (item.getInternalNameOrNull() in garenVacuumVariants) {
                for (line in item.getLore()) {
                    gardenVacuumPattern.matchMatcher(line) {
                        val pests = group("amount").formatNumber()
                        return if (pests >= 40) "§6§z40+" else "$pests"
                    }
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
        event.move(9, "inventory.itemNumberAsStackSize", "inventory.stackSize.itemNumber")
    }
}
