package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraft.client.Minecraft
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object CurrentPing {

    private val config get() = SkyHanniMod.feature.dev

    val averagePing: Duration
        get() = previousPings.takeIf { it.isNotEmpty() }?.average()?.milliseconds ?: Duration.ZERO

    val previousPings: List<Long>
        get() = Minecraft.getInstance().debugOverlay.pingLogger.let {
            List(it.size()) { i -> it[i] }
        }

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
