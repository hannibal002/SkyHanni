package at.lorenz.mod.test

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import at.lorenz.mod.LorenzMod
import at.lorenz.mod.utils.GuiRender.renderString
import at.lorenz.mod.utils.LorenzLogger
import net.minecraftforge.client.event.RenderGameOverlayEvent

class LorenzTest {

    var log = LorenzLogger("debug/packets")

    companion object {
        var enabled = false
        var text = ""

        val debugLogger = LorenzLogger("debug/test")
    }
    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!LorenzMod.feature.debug.enabled) return

        if (enabled) {
            LorenzMod.feature.debug.testPos.renderString(text)
        }
    }

//    @SubscribeEvent
//    fun onReceivePacket(event: PacketEvent.ReceiveEvent) {
//        if (!LorenzMod.feature.debug.enabled) return
//
//        var packet = event.packet
//        var javaClass = packet.javaClass
//        var name = javaClass.name
//
//        if (enabled) {
//            log.log(name)
//        }
//    }
}