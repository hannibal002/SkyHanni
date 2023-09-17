package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.jsonobjects.BingoJson
import at.hannibal2.skyhanni.utils.jsonobjects.BingoJson.BingoTip
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BingoCardTips {
    private var tips: Map<String, BingoTip> = emptyMap()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        event.getConstant<BingoJson>("Bingo")?.let {
            tips = it.bingo_tips
        }
    }

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Bingo Card") return

        val itemName = event.itemStack?.name ?: return
        tips[itemName.removeColor()]?.let {
            val difficulty = Difficulty.valueOf(it.difficulty.uppercase())
            event.toolTip[0] = event.toolTip[0] + " §7(" + difficulty.displayName + "§7)"

            var index = event.toolTip.indexOf("§5§o§7Reward") - 1
            event.toolTip.add(index++, "")
            event.toolTip.add(index++, "§eGuide:")
            for (line in it.note) {
                event.toolTip.add(index++, line)
            }
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Bingo Card") return

        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber != slot.slotIndex) continue
            if (slot.stack == null) continue

            val itemName = slot.stack.name ?: continue

            tips[itemName.removeColor()]?.let {
                val difficulty = Difficulty.valueOf(it.difficulty.uppercase())
                slot highlight difficulty.color.addOpacity(120)
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.event.bingo.bingoCard.bingoSplashGuide

    enum class Difficulty(rawName: String, val color: LorenzColor) {
        EASY("Easy", LorenzColor.GREEN),
        MEDIUM("Medium", LorenzColor.YELLOW),
        HARD("Hard", LorenzColor.RED),
        ;

        val displayName = color.getChatColor() + rawName
    }
}
