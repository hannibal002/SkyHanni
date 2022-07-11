package at.lorenz.mod.test

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import at.lorenz.mod.LorenzMod
import net.minecraftforge.client.event.RenderGameOverlayEvent

class LorenzTest {
    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!LorenzMod.feature.debug.enabled) return

//        val currentScreen = Minecraft.getMinecraft().currentScreen
//
//        LorenzMod.feature.debug.testPos.renderString("currentScreen: $currentScreen")
    }
}