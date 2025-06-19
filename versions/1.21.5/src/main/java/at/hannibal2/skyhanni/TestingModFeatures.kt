package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object TestingModFeatures {

    init {
        println("TestingModFeatures loaded")
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {

    }

}
