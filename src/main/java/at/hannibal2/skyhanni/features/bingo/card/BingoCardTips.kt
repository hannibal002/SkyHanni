package at.hannibal2.skyhanni.features.bingo.card

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.bingo.BingoAPI.getTip
import at.hannibal2.skyhanni.features.bingo.card.goals.GoalType
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BingoCardTips {
    private val config get() = SkyHanniMod.feature.event.bingo.bingoCard

    @SubscribeEvent
    fun onItemTooltipLow(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Bingo Card") return

        val gui = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return
        val slot = gui.slotUnderMouse
        val goal = BingoAPI.bingoGoals.firstOrNull { it.slot == slot.slotNumber } ?: return

        val toolTip = event.toolTip ?: return
        val bingoTip = goal.getTip() ?: return
        val communityGoal = goal.type == GoalType.COMMUNITY

        val difficulty = Difficulty.valueOf(bingoTip.difficulty.uppercase())
        toolTip[0] = toolTip[0] + " §7(" + difficulty.displayName + "§7) ${goal.done}"

        var index = if (!communityGoal) {
            toolTip.indexOf("§5§o§7Reward")
        } else {
            toolTip.indexOfFirst { it.startsWith("§5§o§7Contribution Rewards") }
        } - 1

        toolTip.add(index++, "")
        toolTip.add(index++, "§eGuide:")
        for (line in bingoTip.note) {
            toolTip.add(index++, " $line")
        }
        bingoTip.found?.let {
            toolTip.add(index++, "§7Found by: §e$it")
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

            val goal = BingoAPI.bingoGoals.firstOrNull { it.slot == slot.slotNumber } ?: continue
            if (config.hideDoneDifficulty && goal.done) continue

            val color = goal.getTip()?.let {
                val difficulty = Difficulty.valueOf(it.difficulty.uppercase())
                difficulty.color
            } ?: LorenzColor.GRAY
            slot highlight color.addOpacity(120)
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.bingoSplashGuide

    enum class Difficulty(rawName: String, val color: LorenzColor) {
        EASY("Easy", LorenzColor.GREEN),
        MEDIUM("Medium", LorenzColor.YELLOW),
        HARD("Hard", LorenzColor.RED),
        ;

        val displayName = color.getChatColor() + rawName
    }
}
