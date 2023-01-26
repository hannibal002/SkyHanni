package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event

abstract class LorenzEvent : Event() {

    companion object {
        val map = mutableMapOf<String, Long>()
        var lastPrint = 0L
    }

    private val eventName by lazy {
        this::class.simpleName
    }

    fun postAndCatch(): Boolean {
        return runCatching {
            if (!SkyHanniMod.feature.dev.printEventTimings) {
                return MinecraftForge.EVENT_BUS.post(this)
            }
            val start = System.currentTimeMillis()
            val result = MinecraftForge.EVENT_BUS.post(this)
            val end = System.currentTimeMillis() - start
            eventName?.let { map.put(it, end + map.getOrDefault(it, 0)) }
            if (System.currentTimeMillis() > lastPrint + 10_000) {
                lastPrint = System.currentTimeMillis()
                println(" ")
                println("Event Timings!")
                for (entry in map.sorted()) {
                    println(entry.key + ": " + entry.value + " ms")
                }
                println(" ")
                map.clear()
            }
            result
        }.onFailure {
            it.printStackTrace()
            LorenzUtils.chat("§cSkyHanni caught and logged an ${it::class.simpleName ?: "error"} at ${eventName}.")
        }.getOrDefault(isCanceled)
    }
}