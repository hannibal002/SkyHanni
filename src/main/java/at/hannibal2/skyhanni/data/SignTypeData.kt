package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SignType.Companion.fromGuiScreen
import at.hannibal2.skyhanni.events.SignOpenEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.gui.inventory.GuiEditSign

@SkyHanniModule
object SignTypeData {
    @HandleEvent
    fun onGuiScreenOpen(event: GuiScreenOpenEvent) {
        val gui = event.gui as? GuiEditSign ?: return
        val signType = fromGuiScreen(gui) ?: return

        SignOpenEvent(gui, signType).post()
    }
}
