package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ExtendedChatColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.network.chat.Component

@SkyHanniModule
object SeaCreatureKillTime {
    private val config get() = SkyHanniMod.feature.fishing.seaCreatureKillTimer

    @Suppress("MaxLineLength")
    @HandleEvent
    fun onSeaCreatureDeath(event: SeaCreatureEvent.Death) {
        if (!config) return
        if (!event.seaCreature.isRare) return
        val seaCreature = event.seaCreature
        val time = seaCreature.spawnTime.passedSince()
        if (event.seenDeath) {
            ChatUtils.chat(Component.literal("${seaCreature.displayName}§e took ").append(ExtendedChatColor("#FFC600").asText(time.format(showMilliSeconds = true))).append("§e to die."))

        } else {
            val minTime = seaCreature.lastUpdate.passedSince()
            val message = "${seaCreature.displayName}§e took between " +
                "§b${minTime.format(showMilliSeconds = true)} §eand " +
                "§b${time.format(showMilliSeconds = true)}§e to die."
            ChatUtils.chat(message)
        }
    }
}
