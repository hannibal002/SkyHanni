package at.lorenz.mod.test

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import at.lorenz.mod.LorenzMod
import at.lorenz.mod.utils.GuiRender.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class LorenzTest {
    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!LorenzMod.feature.debug.enabled) return

//        val currentScreen = Minecraft.getMinecraft().currentScreen
//
//        LorenzMod.feature.debug.testPos.renderString("currentScreen: $currentScreen")
    }
}