package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.IChatComponent

class PatternHiderManager(skyHanniMod: SkyHanniMod) {

    private val referenceCounter = mutableListOf<Counter>()

    init {
        val watchdogHider = PatternHider(this, "watchdog", watchdogPattern) {
            LorenzUtils.onHypixel && SkyHanniMod.feature.chat.watchDog
        }
        skyHanniMod.loadModule(watchdogHider)
    }

    fun storeChat(chat: IChatComponent) {
        val counter = referenceCounter.find { it.chat == chat } ?: run {
            val stored = Counter(chat)
            referenceCounter.add(stored)
            stored
        }
        counter.references++
    }

    fun removeChat(chat: IChatComponent, restoreBlockedChat: Boolean) {
        val counter = referenceCounter.find { it.chat == chat }!!
        counter.references--
        if (counter.references == 0) {
            referenceCounter.remove(counter)
            if (restoreBlockedChat) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(counter.chat)
            }
        }
    }

    companion object {
        val watchdogPattern = listOf(
            Regex("§f"),
            Regex("§4\\[WATCHDOG ANNOUNCEMENT]"),
            Regex("§fWatchdog has banned §r§c§l(.*)§r§f players in the last 7 days."),
            Regex("§fStaff have banned an additional §r§c§l(.*)§r§f in the last 7 days."),
            Regex("§cBlacklisted modifications are a bannable offense!"),
            Regex("§c")
        )
    }
}

data class Counter(val chat: IChatComponent, var references: Int = 0)
