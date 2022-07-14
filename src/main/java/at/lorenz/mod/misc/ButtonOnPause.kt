package at.lorenz.mod.misc

import at.lorenz.mod.LorenzMod
import at.lorenz.mod.utils.LorenzUtils
import at.lorenz.mod.config.config.ConfigEditor
import at.lorenz.mod.config.core.GuiScreenElementWrapper
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ButtonOnPause {
    private val buttonId = System.nanoTime().toInt()

    @SubscribeEvent
    fun onGuiAction(event: GuiScreenEvent.ActionPerformedEvent.Post) {
        if (!LorenzUtils.isOnHypixel) return

        if (LorenzMod.feature.misc.configButtonOnPause && event.gui is GuiIngameMenu && event.button.id == buttonId) {
            LorenzMod.screenToOpen = GuiScreenElementWrapper(
                ConfigEditor(
                    LorenzMod.feature
                )
            )
        }
    }

    @SubscribeEvent
    fun onGuiInitPost(event: GuiScreenEvent.InitGuiEvent.Post) {
        if (!LorenzUtils.isOnHypixel) return

        if (LorenzMod.feature.misc.configButtonOnPause && event.gui is GuiIngameMenu) {
            val x = event.gui.width - 105
            val x2 = x + 100
            var y = event.gui.height - 22
            var y2 = y + 20
            val sorted = event.buttonList.sortedWith { a, b -> b.yPosition + b.height - a.yPosition + a.height }
            for (button in sorted) {
                val otherX = button.xPosition
                val otherX2 = button.xPosition + button.width
                val otherY = button.yPosition
                val otherY2 = button.yPosition + button.height
                if (otherX2 > x && otherX < x2 && otherY2 > y && otherY < y2) {
                    y = otherY - 20 - 2
                    y2 = y + 20
                }
            }
            event.buttonList.add(GuiButton(buttonId, x, 0.coerceAtLeast(y), 100, 20, "Lorenz Mod"))
        }
    }
}