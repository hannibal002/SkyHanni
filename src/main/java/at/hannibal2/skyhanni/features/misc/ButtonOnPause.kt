package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigGuiManager
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.render.gui.GuiActionPerformedEvent
import at.hannibal2.hanni.events.render.gui.InitializeGuiEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.SkyBlockUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiIngameMenu
//#if MC > 1.21
//$$ import net.minecraft.client.gui.widget.ButtonWidget
//$$ import net.minecraft.text.Text
//#endif

@HanniModule
object ButtonOnPause {

    private val config get() = HanniMod.feature.gui
    private val buttonId = System.nanoTime().toInt()

    //#if MC < 1.21
    @HandleEvent
    fun onGuiActionPerformed(event: GuiActionPerformedEvent) {
        if (!SkyBlockUtils.onHypixel) return

        if (config.configButtonOnPause && event.gui is GuiIngameMenu && event.button.id == buttonId) {
            ConfigGuiManager.openConfigGui()
        }
    }
    //#endif

    @HandleEvent
    fun onInitializeGuiPost(event: InitializeGuiEvent) {
        if (!SkyBlockUtils.onHypixel) return

        if (config.configButtonOnPause && event.gui is GuiIngameMenu) {
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
            //#if MC < 1.21
            event.buttonList.add(GuiButton(buttonId, x, 0.coerceAtLeast(y), 100, 20, "Hanni"))
            //#else
            //$$ ButtonWidget.builder(Text.of("Hanni")) {
            //$$     ConfigGuiManager.openConfigGui()
            //$$ }.dimensions(x, 0.coerceAtLeast(y), 100, 20).build()
            //#endif
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.configButtonOnPause", "gui.configButtonOnPause")
    }
}
