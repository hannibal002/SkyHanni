package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.AbstractMenuStackSize
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayMining : AbstractMenuStackSize() {
    private val miningSubgroup = itemStackSizeGroup.group("mining")
    
    private val hotmPerkLevelXOutOfYLoreLinePattern by miningSubgroup.pattern(
        "hotmperklevelxoutofy.loreline",
        "(§.).* (?<useful>[0-9]+)(§.)?(\\/(§.)?(?<total>[0-9]+))?.*"
    )
    private val rightClickToEnableDisableLoreLinePattern by miningSubgroup.pattern(
        "rightclicktoenabledisable.loreline",
        "(§.)*Right.?click to (?<colorCode>§.)*disable(§.)*!"
    )
    private val skyMallCurrentEffectLoreLinePattern by miningSubgroup.pattern(
        "skymallcurrenteffect.loreline",
        ".*(§.)*Your Current Effect.*"
    )
    private val theSkymallCurrentEffectInQuestionLoreLinePattern by miningSubgroup.pattern(
        "theskymallcurrenteffectinquestion.loreline",
        "(§.)*.*■ (§.)*(?<thePerk>.+)"
    )
    private val hotmPerkEnabledDisabledInProgressItemNamePattern by miningSubgroup.pattern(
        "hotmperkenableddisabledinprogress.itemname",
        "§(a|e|c).*"
    )
    private val isNotHOTMPerkFirstCheckLoreLinePattern by miningSubgroup.pattern(
        "isnothotmperkfirstcheck.loreline",
        "^((?!(§.)*Level ).)*\$"
    )
    private val isNotHOTMPerkSecondCheckLoreLinePattern by miningSubgroup.pattern(
        "isnothotmperksecondcheck.loreline",
        "^((?!(§.)*(Right|Left).click to ).)*\$"
    )
    private val lockedHOTMPerkLoreLinePattern by miningSubgroup.pattern(
        "lockedhotmperk.loreline",
        ".*(§.)*(Requires .*|.*(the )?Mountain!).*"
    )
    private val isHOTMPerkMaxedItemNamePattern by miningSubgroup.pattern(
        "ishotmperkmaxed.itemname",
        "§a.*"
    )
    private val isHOTMPerkDisabledLoreLinePattern by miningSubgroup.pattern(
        "ishotmperkdisabled.loreline",
        "(§.)*(.*)click to (§.)*(enable).*"
    )
    private val isHOTMTierItemNamePattern by miningSubgroup.pattern(
        "ishotmtier.itemname",
        "Tier (?<tier>[\\w]+)"
    )
    private val isHOTMTierUnlockedItemNamePattern by miningSubgroup.pattern(
        "ishotmtierunlocked.itemname",
        "§aTier (?<tier>[\\w]+)"
    )
    private val hotmLevelPercentProgressLoreLinePattern by miningSubgroup.pattern(
        "hotmlevelpercentprogress.loreline",
        ".*Progress.*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )
    private val crystalsNotForCrystalNucleusLoreLinePattern by miningSubgroup.pattern(
        "crystalsnotforcrystalnucleus.loreline",
        ".*(Your Other Crystals|Jasper|Ruby).*"
    )
    private val crystalNotPlacedLoreLinePattern by miningSubgroup.pattern(
        "crystalnotplaced.loreline",
        ".* §e✖ Not Placed"
    )
    private val crystalNotFoundLoreLinePattern by miningSubgroup.pattern(
        "crystalnotfound.loreline",
        ".* §c✖ Not Found"
    )

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        super.onRenderItemTip(event)
    }

    override fun getStackTip(item: ItemStack): String {
        if (configMenuStackSize.mining.isEmpty()) return ""
        val stackSizeConfig = configMenuStackSize.mining
        val chestName = InventoryUtils.openInventoryName()
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.Mining.CURRENT_SKYMALL_PERK) && (item.cleanName() == ("Sky Mall")) && (chestName == "Heart of the Mountain")) {
            val lore = item.getLore()
            rightClickToEnableDisableLoreLinePattern.matchMatcher(lore.last()) {
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
                val lore = item.getLore()
                if ((lore.firstOrNull() == null) || (lore.lastOrNull() == null)) return ""
                isNotHOTMPerkFirstCheckLoreLinePattern.matchMatcher(lore.first()) {
                    isNotHOTMPerkSecondCheckLoreLinePattern.matchMatcher(lore.last()) {
                        return ""
                    }
                }
                lockedHOTMPerkLoreLinePattern.matchMatcher(lore.last()) { return "" }
                hotmPerkLevelXOutOfYLoreLinePattern.matchMatcher(lore.first()) {
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
                rightClickToEnableDisableLoreLinePattern.matchMatcher(lore.last()) {
                    return "${group("colorCode")}!!" //for hotm perks that are one-click/instant unlock
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.Mining.HOTM_OVERALL_TIERS) && chestName == ("Heart of the Mountain")) {
            isHOTMTierItemNamePattern.matchMatcher(item.cleanName()) {
                val nameWithColor = item.name ?: return ""
                isHOTMTierUnlockedItemNamePattern.matchMatcher(nameWithColor) { return "" }
                val lore = item.getLore()
                if (lore.isNotEmpty()) {
                    for (line in lore) {
                        hotmLevelPercentProgressLoreLinePattern.matchMatcher(line) {
                            return group("percent").convertPercentToGreenCheckmark()
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
            loop@ for (line in lore) {
                crystalsNotForCrystalNucleusLoreLinePattern.matchMatcher(line) { break@loop }
                crystalNotPlacedLoreLinePattern.matchMatcher(line) { crystalsNotPlaced++ }
                crystalNotFoundLoreLinePattern.matchMatcher(line) { crystalsNotFound++ }
            }
            val crystalsPlaced = 5 - crystalsNotPlaced - crystalsNotFound
            return "§a${crystalsPlaced}§r§e${crystalsNotPlaced}§r§c${crystalsNotFound}"
        }

        return ""
    }
}
