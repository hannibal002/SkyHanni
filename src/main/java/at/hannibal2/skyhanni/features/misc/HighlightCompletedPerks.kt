package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightCompletedPerks {
    private val patternGroup = RepoPattern.group("misc.highlightmaxperks")
    private val shopChestPattern by patternGroup.pattern(
        "shop.chestname",
        "(?:\\S+ Essence|Community) Shop"
    )
    private val upgradePattern by patternGroup.pattern(
        "upgrade.loreline",
        "§.\\S+ Upgrade"
    )
    private val maxedOutPattern by patternGroup.pattern(
        "maxed.loreline",
        "(?:§.)*(?:Maxed out!|UNLOCKED)"
    )

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        val chestName = InventoryUtils.openInventoryName()
        if (!shopChestPattern.matches(chestName)) return
        val slots = event.gui.inventorySlots.inventorySlots
        highlightItems(slots, chestName)
    }

    private fun highlightItems(slots: List<Slot>, chestName: String) {
        for (s in slots) {
            if (!s.hasStack) continue
            val itemLore = s.stack.getLore()
            if (itemLore.isEmpty()) continue
            if (!upgradePattern.matches(itemLore.first()) && chestName == "Community Shop") continue
            if (!maxedOutPattern.matches(itemLore.last())) continue
            s highlight LorenzColor.GREEN
        }
    }

    private fun isEnabled(): Boolean = LorenzUtils.inSkyBlock && SkyHanniMod.feature.misc.highlightCompletedPerks
}
