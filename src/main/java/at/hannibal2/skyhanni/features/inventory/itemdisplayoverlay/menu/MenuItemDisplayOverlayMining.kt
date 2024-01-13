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
    
    private val hotmPerkLevelPattern by miningSubgroup.pattern(
        "hotm.perklevel.loreline",
        "(§.).* (?<useful>[0-9]+)(§.)?(\\/(§.)?(?<total>[0-9]+))?.*"
    )
    private val rClickTogglePattern by miningSubgroup.pattern(
        "rclick.toggle.loreline",
        "(§.)*Right.?click to (?<colorCode>§.)*disable(§.)*!"
    )
    private val currentEffectPattern by miningSubgroup.pattern(
        "skymall.currenteffect.loreline",
        ".*(§.)*Your Current Effect.*"
    )
    private val currentEffectActivePattern by miningSubgroup.pattern(
        "skymall.currenteffect.loreline",
        "(§.)*.*■ (§.)*(?<thePerk>.+)"
    )
    private val hotmColorCodePattern by miningSubgroup.pattern(
        "hotm.colorcode.itemname",
        "§(a|e|c).*"
    )
    private val notHOTMPerkFirstPattern by miningSubgroup.pattern(
        "hotm.notperk.checkone.loreline",
        "^((?!(§.)*Level ).)*\$"
    )
    private val notHOTMPerkSecondPattern by miningSubgroup.pattern(
        "hotm.notperk.checktwo.loreline",
        "^((?!(§.)*(Right|Left).click to ).)*\$"
    )
    private val lockedHOTMPerkPattern by miningSubgroup.pattern(
        "hotm.lockedperk.loreline",
        ".*(§.)*(Requires .*|.*(the )?Mountain!).*"
    )
    private val maxedHOTMPerkPattern by miningSubgroup.pattern(
        "hotm.maxedperk.itemname",
        "§a.*"
    )
    private val disabledHOTMPerkPattern by miningSubgroup.pattern(
        "hotm.disabledperk.loreline",
        "(§.)*(.*)click to (§.)*(enable).*"
    )
    private val hotmTierPattern by miningSubgroup.pattern(
        "hotm.tier.itemname",
        "Tier (?<tier>[\\w]+)"
    )
    private val hotmTierUnlockedPattern by miningSubgroup.pattern(
        "hotm.tierunlocked.itemname",
        "§aTier (?<tier>[\\w]+)"
    )
    private val hotmLevelPercentPattern by miningSubgroup.pattern(
        "hotm.levelpercent.loreline",
        ".*Progress.*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )
    private val nonNucleusCrystalsPattern by miningSubgroup.pattern(
        "crystals.nonnucleus.loreline",
        ".*(Your Other Crystals|Jasper|Ruby).*"
    )
    private val crystalNotPlacedPattern by miningSubgroup.pattern(
        "crystal.notplaced.loreline",
        ".* §e✖ Not Placed"
    )
    private val crystalNotFoundPattern by miningSubgroup.pattern(
        "crystal.notfound.loreline",
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
            rClickTogglePattern.matchMatcher(lore.last()) {
                var currentEffectLineLocated = false
                for (line in lore) {
                    currentEffectPattern.matchMatcher(line) {
                        currentEffectLineLocated = true
                    }
                    if (currentEffectLineLocated) {
                        currentEffectActivePattern.matchMatcher(line) {
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
            hotmColorCodePattern.matchMatcher(nameWithColor) {
                val lore = item.getLore()
                if ((lore.firstOrNull() == null) || (lore.lastOrNull() == null)) return ""
                notHOTMPerkFirstPattern.matchMatcher(lore.first()) {
                    notHOTMPerkSecondPattern.matchMatcher(lore.last()) {
                        return ""
                    }
                }
                lockedHOTMPerkPattern.matchMatcher(lore.last()) { return "" }
                hotmPerkLevelPattern.matchMatcher(lore.first()) {
                    var colorCode = ""
                    var level = group("useful")
                    if (group("total") == null) level = "✔"
                    maxedHOTMPerkPattern.matchMatcher(nameWithColor) {
                        colorCode = "§a"
                    }
                    for (line in lore) {
                        disabledHOTMPerkPattern.matchMatcher(line) {
                            colorCode = "§c"
                        }
                    }
                    return "$colorCode$level"
                }
                rClickTogglePattern.matchMatcher(lore.last()) {
                    return "${group("colorCode")}!!" //for hotm perks that are one-click/instant unlock
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.Mining.HOTM_OVERALL_TIERS) && chestName == ("Heart of the Mountain")) {
            hotmTierPattern.matchMatcher(item.cleanName()) {
                val nameWithColor = item.name ?: return ""
                hotmTierUnlockedPattern.matchMatcher(nameWithColor) { return "" }
                val lore = item.getLore()
                if (lore.isNotEmpty()) {
                    for (line in lore) {
                        hotmLevelPercentPattern.matchMatcher(line) {
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
                nonNucleusCrystalsPattern.matchMatcher(line) { break@loop }
                crystalNotPlacedPattern.matchMatcher(line) { crystalsNotPlaced++ }
                crystalNotFoundPattern.matchMatcher(line) { crystalsNotFound++ }
            }
            val crystalsPlaced = 5 - crystalsNotPlaced - crystalsNotFound
            return "§a${crystalsPlaced}§r§e${crystalsNotPlaced}§r§c${crystalsNotFound}"
        }

        return ""
    }
}
