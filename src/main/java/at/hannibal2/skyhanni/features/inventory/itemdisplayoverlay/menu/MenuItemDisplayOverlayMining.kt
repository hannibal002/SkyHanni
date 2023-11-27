package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayMining {
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val xOutOfYNoColorRequiredPattern = "(§.).* (?<useful>[0-9]+)(§.)?(\\/(§.)?(?<total>[0-9]+))?.*".toPattern()
    private val rightClickToEnableDisableLoreLinePattern = (("(§.)*Right.?click to (§.)*disable(§.)*!").toPattern())
    private val skyMallCurrentEffectLoreLinePattern = ((".*(§.)*Your Current Effect.*").toPattern())
    private val theSkymallCurrentEffectInQuestionLoreLinePattern = (("(§.)*.*■ (§.)*(?<thePerk>.+)").toPattern())
    private val hotmPerkEnabledDisabledInProgressItemNamePattern = (("§(a|e|c).*").toPattern())
    private val isNotHOTMPerkFirstCheckLoreLinePattern = (("^((?!(§.)*Level ).)*\$").toPattern())
    private val isNotHOTMPerkSecondCheckLoreLinePattern = (("^((?!(§.)*(Right|Left).click to ).)*\$").toPattern())
    private val lockedHOTMPerkLoreLinePattern = ((".*(§.)*(Requires .*|.*the Mountain!).*").toPattern())
    private val isHOTMPerkMaxedItemNamePattern = (("§a.*").toPattern())
    private val isHOTMPerkDisabledLoreLinePattern = (("(§.)*(.*)click to (§.)*(enable).*").toPattern())
    private val isHOTMTierItemNamePattern = (("Tier (?<tier>[\\w]+)").toPattern())
    private val isHOTMTierUnlockedItemNamePattern = (("§aTier (?<tier>[\\w]+)").toPattern())
    private val hotmLevelPercentProgressLoreLinePattern = ((".*Progress.*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%").toPattern())
    private val crystalsNotForCrystalNucleusLoreLinePattern = ((".*(Your Other Crystals|Jasper|Ruby).*").toPattern())
    private val crystalNotPlacedLoreLinePattern = ((".* §e✖ Not Placed").toPattern())
    private val crystalNotFoundLoreLinePattern = ((".* §c✖ Not Found").toPattern())

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.mining.isEmpty()) return ""
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.mining
        val chestName = InventoryUtils.openInventoryName()
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.Mining.CURRENT_SKYMALL_PERK) && (item.cleanName() == ("Sky Mall")) && (chestName == "Heart of the Mountain")) {
            val lore = item.getLore()
            rightClickToEnableDisableLoreLinePattern.matchMatcher(lore.last()) {
                // §8 ? §7Gain §a+100 §6? Mining Speed§7.§r
                /*
                "§8 ■ §7Gain §a+100 §6⸕ Mining Speed§7." --> " ■ Gain +100 ⸕ Mining Speed."
                "§8 ■ §7Gain §a+50 §6☘ Mining Fortune§7." --> " ■ Gain +50 ☘ Mining Fortune."
                "§8 ■ §7Gain §a+15% §7more Powder while" --> " ■ Gain +15% more Powder while"
                "§8 ■ §7Reduce Pickaxe Ability cooldown" --> " ■ Reduce Pickaxe Ability cooldown"
                "§8 ■ §7§a10x §7chance to find Goblins" --> " ■ 10x chance to find Goblins"
                "§8 ■ §7Gain §a5x §9Titanium §7drops." --> " ■ Gain 5x Titanium drops."
                "§aYour Current Effect" --> "Your Current Effect"
                */
                var currentEffectLineLocated = false
                for (line in lore) {
                    skyMallCurrentEffectLoreLinePattern.matchMatcher(line) {
                        currentEffectLineLocated = true
                    }
                    if (currentEffectLineLocated) {
                        theSkymallCurrentEffectInQuestionLoreLinePattern.matchMatcher(line) {
                            return when (group("thePerk")) {
                                "Gain §a+100 §6⸕ Mining Speed§7." -> return "§a+§6⸕"
                                "Gain §a+50 §6☘ Mining Fortune§7." -> return "§a+§6☘"
                                "Gain §a+15% §7more Powder while" -> return "§a15%"
                                "Reduce Pickaxe Ability cooldown" -> return "§a20%"
                                "10x §7chance to find Golden" -> return "§a10x"
                                "Gain §a5x §9Titanium §7drops." -> return "§a5x§9T"
                                else -> "§c!?"
                            }
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Mining.HOTM_PERK_LEVELS) && (chestName == "Heart of the Mountain")) {
            val nameWithColor = item.name ?: return ""
            hotmPerkEnabledDisabledInProgressItemNamePattern.matchMatcher(nameWithColor) {
                //§7Level 64/§8100
                val lore = item.getLore()
                if ((lore.firstOrNull() == null) || (lore.lastOrNull() == null)) return ""
//                 if (!lore.first().contains("Level ") && !lore.last().contains("Right click to ")) return ""
//                 if (lore.last().contains("the Mountain!") || lore.last().contains("Requires ")) return ""
                isNotHOTMPerkFirstCheckLoreLinePattern.matchMatcher(lore.first()) {
                    isNotHOTMPerkSecondCheckLoreLinePattern.matchMatcher(lore.last()) {
                        return ""
                    }
                }
                lockedHOTMPerkLoreLinePattern.matchMatcher(lore.last()) { return "" }
                xOutOfYNoColorRequiredPattern.matchMatcher(lore.first()) {
                    //§7Level 64/§8100
                    var colorCode = ""
                    var level = group("useful")
                    if (group("total") == null) level = "✔"
                    isHOTMPerkMaxedItemNamePattern.matchMatcher(nameWithColor) {
                        colorCode = "§a"
                    }
                    for (line in lore) {
                        isHOTMPerkDisabledLoreLinePattern.matchMatcher(line) {
                            colorCode = "§c"
                        }
                    }
                    return "$colorCode$level"
                }
            }
        }

        //the basis of all of this code was from technoblade's skycrypt profile so this might be WAY off, please have mercy
        //https://sky.shiiyu.moe/stats/Technoblade/Blueberry#Skills
        //ping @erymanthus on the skyhanni discord if you find any bugs with this
        if (stackSizeConfig.contains(StackSizeMenuConfig.Mining.HOTM_OVERALL_TIERS) && chestName == ("Heart of the Mountain")) {
            isHOTMTierItemNamePattern.matchMatcher(item.cleanName()) {
                val nameWithColor = item.name ?: return ""
                isHOTMTierUnlockedItemNamePattern.matchMatcher(nameWithColor) { return "" }
                val lore = item.getLore()
                if (lore != null && lore.isNotEmpty()) {
                    for (line in lore) {
                        hotmLevelPercentProgressLoreLinePattern.matchMatcher(line) {
                            return group("percent").replace("100", "§a✔")
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.Mining.CRYSTAL_HOLLOWS_NUCLEUS) && (chestName == "Heart of the Mountain")) {
            val nameWithColor = item.name ?: return ""
            if (nameWithColor != "§5Crystal Hollows Crystals") return ""
            val lore = item.getLore()
            var crystalsNotPlaced = 0
            var crystalsNotFound = 0
            val totalCrystals = 5 //change "5" to whatever new value Hypixel does if this value ever changes
            for (line in lore) {
                crystalsNotForCrystalNucleusLoreLinePattern.matchMatcher(line) { break }
                crystalNotPlacedLoreLinePattern.matchMatcher(line) { crystalsNotPlaced++ }
                crystalNotFoundLoreLinePattern.matchMatcher(line) { crystalsNotFound++ }
            }
            val crystalsPlaced = totalCrystals - crystalsNotPlaced - crystalsNotFound
            return "§a${crystalsPlaced}§r§e${crystalsNotPlaced}§r§c${crystalsNotFound}"
        }

        return ""
    }
}
