package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import java.awt.Color

@SkyHanniModule
object TestObf {

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        println("melon lord message ${event.message}")
    }

    @HandleEvent
    fun onSpawn(event: MobEvent.Spawn) {
        if (event.mob.name.contains("a")) {
            RenderLivingEntityHelper.setEntityColor(event.mob.baseEntity, Color.GREEN) { true }
        }
        if (event.mob.name.contains("b")) {
            RenderLivingEntityHelper.setEntityColor(event.mob.baseEntity, Color.PINK) { true }
        }
        if (event.mob.name.contains("c")) {
            RenderLivingEntityHelper.setEntityColor(event.mob.baseEntity, Color.ORANGE) { true }
        }
    }
}
