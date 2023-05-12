package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event

abstract class LorenzEvent : Event() {

    private val eventName by lazy {
        this::class.simpleName
    }

    fun postAndCatch(): Boolean {
        return runCatching {
            MinecraftForge.EVENT_BUS.post(this)
        }.onFailure {
            if (it is NoSuchMethodError) {
                LorenzUtils.chat("§c[SkyHanni] You need to use a newer version of NotEnoughUpdates (alpha-11 or newer)! If you need help downloading it, go to the skyhanni discord.")
            } else {
                it.printStackTrace()
                LorenzUtils.chat("§cSkyHanni ${SkyHanniMod.version} caught and logged an ${it::class.simpleName ?: "error"} at ${eventName}: ${it.message}")
            }
        }.getOrDefault(isCanceled)
    }
}