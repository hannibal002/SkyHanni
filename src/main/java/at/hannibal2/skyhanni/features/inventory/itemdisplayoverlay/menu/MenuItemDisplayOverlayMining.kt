package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.AbstractMenuStackSize
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayMining : AbstractMenuStackSize() {
    // private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val hotmPerkLevelXOutOfYLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.hotmperklevelxoutofy.loreline"), ("(§.).* (?<useful>[0-9]+)(§.)?(\\/(§.)?(?<total>[0-9]+))?.*"))
    private val rightClickToEnableDisableLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.rightclicktoenabledisable.loreline"), ("(§.)*Right.?click to (§.)*disable(§.)*!"))
    private val skyMallCurrentEffectLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.skymallcurrenteffect.loreline"), (".*(§.)*Your Current Effect.*"))
    private val theSkymallCurrentEffectInQuestionLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.theskymallcurrenteffectinquestion.loreline"), ("(§.)*.*■ (§.)*(?<thePerk>.+)"))
    private val hotmPerkEnabledDisabledInProgressItemNamePattern by RepoPattern.pattern(("itemstacksize.mining.hotmperkenableddisabledinprogress.itemname"), ("§(a|e|c).*"))
    private val isNotHOTMPerkFirstCheckLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.isnothotmperkfirstcheck.loreline"), ("^((?!(§.)*Level ).)*\$"))
    private val isNotHOTMPerkSecondCheckLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.isnothotmperksecondcheck.loreline"), ("^((?!(§.)*(Right|Left).click to ).)*\$"))
    private val lockedHOTMPerkLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.lockedhotmperk.loreline"), (".*(§.)*(Requires .*|.*the Mountain!).*"))
    private val isHOTMPerkMaxedItemNamePattern by RepoPattern.pattern(("itemstacksize.mining.ishotmperkmaxed.itemname"), ("§a.*"))
    private val isHOTMPerkDisabledLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.ishotmperkdisabled.loreline"), ("(§.)*(.*)click to (§.)*(enable).*"))
    private val isHOTMTierItemNamePattern by RepoPattern.pattern(("itemstacksize.mining.ishotmtier.itemname"), ("Tier (?<tier>[\\w]+)"))
    private val isHOTMTierUnlockedItemNamePattern by RepoPattern.pattern(("itemstacksize.mining.ishotmtierunlocked.itemname"), ("§aTier (?<tier>[\\w]+)"))
    private val hotmLevelPercentProgressLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.hotmlevelpercentprogress.loreline"), (".*Progress.*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val crystalsNotForCrystalNucleusLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.crystalsnotforcrystalnucleus.loreline"), (".*(Your Other Crystals|Jasper|Ruby).*"))
    private val crystalNotPlacedLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.crystalnotplaced.loreline"), (".* §e✖ Not Placed"))
    private val crystalNotFoundLoreLinePattern by RepoPattern.pattern(("itemstacksize.mining.crystalnotfound.loreline"), (".* §c✖ Not Found"))

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
                // §7Level 64/§8100
                val lore = item.getLore()
                if ((lore.firstOrNull() == null) || (lore.lastOrNull() == null)) return ""
                // if (!lore.first().contains("Level ") && !lore.last().contains("Right click to ")) return ""
                // if (lore.last().contains("the Mountain!") || lore.last().contains("Requires ")) return ""
                isNotHOTMPerkFirstCheckLoreLinePattern.matchMatcher(lore.first()) {
                    isNotHOTMPerkSecondCheckLoreLinePattern.matchMatcher(lore.last()) {
                        return ""
                    }
                }
                lockedHOTMPerkLoreLinePattern.matchMatcher(lore.last()) { return "" }
                hotmPerkLevelXOutOfYLoreLinePattern.matchMatcher(lore.first()) {
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

        // the basis of all of this code was from technoblade's skycrypt profile so this might be WAY off, please have mercy
        // https://sky.shiiyu.moe/stats/Technoblade/Blueberry#Skills
        // o7
        // ping @erymanthus on the skyhanni discord if you find any bugs with this
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
            val totalCrystals = 5 //change "5" to whatever new value Hypixel does if this value ever changes
            loop@ for (line in lore) {
                crystalsNotForCrystalNucleusLoreLinePattern.matchMatcher(line) { break@loop }
                crystalNotPlacedLoreLinePattern.matchMatcher(line) { crystalsNotPlaced++ }
                crystalNotFoundLoreLinePattern.matchMatcher(line) { crystalsNotFound++ }
            }
            val crystalsPlaced = totalCrystals - crystalsNotPlaced - crystalsNotFound
            return "§a${crystalsPlaced}§r§e${crystalsNotPlaced}§r§c${crystalsNotFound}"
        }

        return ""
    }
}
