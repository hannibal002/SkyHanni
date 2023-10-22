package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlaySBLeveling {
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.menuItemNumberSBLevelingAsStackSize.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.menuItemNumberSBLevelingAsStackSize
        val chestName = InventoryUtils.openInventoryName()
        
        if (stackSizeConfig.contains(0)) {
            if (((chestName.contains("Guide ")) || chestName.contains("Task")) && (itemName.isNotEmpty())) {
                val lore = item.getLore()
                for (line in lore) {
                    if (line.contains("Progress")) {
                        return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                    }
                }
                if (itemName.contains("✔")) return "§a✔"
            }
        }

        if (stackSizeConfig.contains(1)) {
            for (line in item.getLore()) {
                if (line.contains("Progress to Complete Category")) {
                    return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                }
            }
        }

        if (stackSizeConfig.contains(2)) {
            if ((chestName.contains("Rewards") || chestName.lowercase().contains("skyblock leveling")) && (itemName.isNotEmpty())) {
                val lore = item.getLore()
                for (line in lore) {
                    if (line.contains("Progress to ") || line.contains("Rewards Unlocked: ")) {
                        return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                    }
                }
            }
        }

        if (stackSizeConfig.contains(3)) {
            val nameWithColor = item.name ?: return ""
            if ((chestName.contains("Emblems")) && (itemName.isNotEmpty() && (nameWithColor.contains("§a")) && !(itemName.contains(" ")))) {
                val bruh = item.getLore().first().removeColor().split(" ").first().trim()
                if (bruh.toIntOrNull() is Int) return bruh
            }
        }

        return ""
    }
}
