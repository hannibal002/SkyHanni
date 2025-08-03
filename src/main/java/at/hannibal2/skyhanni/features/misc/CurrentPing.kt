package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.hypixelapi.HypixelEventApi
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPingPacket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
//#if MC >= 1.21
//$$ import net.minecraft.client.MinecraftClient
//#endif

@SkyHanniModule
object CurrentPing {

    private val config get() = SkyHanniMod.feature.dev

    val averagePing: Duration
        get() = previousPings.takeIf { it.isNotEmpty() }?.average()?.milliseconds ?: Duration.ZERO

    //#if MC < 1.21
    val previousPings = mutableListOf<Long>()

    private var lastPingTime = SimpleTimeMark.farPast()
    private var waitingForPacket = false

    @Suppress("UNUSED_PARAMETER")
    fun onPongPacket(packet: ClientboundPingPacket) {
        if (!isEnabled()) return
        waitingForPacket = false

        if (previousPings.size > 5) {
            previousPings.subList(0, previousPings.size - 5).clear()
        }
        previousPings.add(lastPingTime.passedSince().inWholeMilliseconds)
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(10)) return
        if (lastPingTime.passedSince() > 20.seconds) waitingForPacket = false
        requestPing()
    }

    private fun requestPing() {
        if (waitingForPacket || !SkyBlockUtils.onHypixel) return
        lastPingTime = SimpleTimeMark.now()
        waitingForPacket = true
        HypixelEventApi.sendPacket(ServerboundPingPacket())
    }

    //#else
    //$$ val previousPings: List<Long>
    //$$     get() = buildList {
    //$$         MinecraftClient.getInstance().debugHud.pingLog.let {
    //$$             for (i in 0..<it.getLength()) {
    //$$                 add(it.get(i))
    //$$             }
    //$$         }
    //$$     }
    //#endif

    fun getFormattedPing(): String =
        "Current Ping: ${averagePing.inWholeMilliseconds.addSeparators()} ms"

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shping") {
            category = CommandCategory.USERS_ACTIVE
            description = "Shows your current ping"
            simpleCallback {
                if (!isEnabled()) {
                    ChatUtils.chatAndOpenConfig(
                        "This requires you to turn on \"Ping API\". Click this message to open the config.",
                        config::pingApi,
                    )
                } else {
                    ChatUtils.chat(getFormattedPing())
                }
            }
        }
    }

    fun isEnabled() = config.pingApi

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(102, "dev.hypixelPingApi", "dev.pingApi")
    }
}
