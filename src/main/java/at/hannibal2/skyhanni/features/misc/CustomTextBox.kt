package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import net.minecraft.client.Minecraft
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CustomTextBox {
    private val config get() = SkyHanniMod.feature.misc.textBox
    private var display = listOf<List<Any>>()

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        val newList = mutableListOf<List<Any>>()
        newList.add(listOf(config.text.replace("&", "§")))
        display = newList
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!config.enabled) return
        if (!LorenzUtils.inSkyBlock) return
        if (Minecraft.getMinecraft().currentScreen is GuiScreenElementWrapper) {
            val newList = mutableListOf<List<Any>>()
            newList.add(listOf(config.text.replace("&", "§")))
            display = newList
        }
        config.position.renderStringsAndItems(display, posLabel = "Custom Text Box")
    }
}