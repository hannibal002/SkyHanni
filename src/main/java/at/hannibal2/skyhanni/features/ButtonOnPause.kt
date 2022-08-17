package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.gui.config.ConfigEditor
import at.hannibal2.skyhanni.config.gui.core.GuiScreenElementWrapper
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ButtonOnPause {
    private val buttonId = System.nanoTime().toInt()

    @SubscribeEvent
    fun onGuiAction(event: GuiScreenEvent.ActionPerformedEvent.Post) {
        if (!LorenzUtils.isOnHypixel) return

        if (SkyHanniMod.feature.misc.configButtonOnPause && event.gui is GuiIngameMenu && event.button.id == buttonId) {
            SkyHanniMod.screenToOpen = GuiScreenElementWrapper(
                ConfigEditor(
                    SkyHanniMod.feature
                )
            )
        }
    }

    @SubscribeEvent
    fun onGuiInitPost(event: GuiScreenEvent.InitGuiEvent.Post) {
        if (!LorenzUtils.isOnHypixel) return

        if (SkyHanniMod.feature.misc.configButtonOnPause && event.gui is GuiIngameMenu) {
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
            event.buttonList.add(GuiButton(buttonId, x, 0.coerceAtLeast(y), 100, 20, "SkyHanni"))
        }
    }
}