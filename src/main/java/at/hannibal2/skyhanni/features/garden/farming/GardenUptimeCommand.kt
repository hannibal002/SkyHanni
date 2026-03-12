package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils.chat
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenUptimeCommand {
    private val storage get() = GardenApi.storage?.gardenBpsTracker

    fun onCommand(days: Int) {
        val date = LocalDate.now()
        var totalUptime = 0.seconds

        val commandString = mutableListOf(
            "§r§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r",
            "§b${PlayerUtils.getName()}'s§e garden uptime for the past §a$days §edays:",
            ""
        )

        for (num in 0..<days) {

            val day = date.minusDays(num.toLong())
            val entry = storage?.getData(SkyHanniTracker.DisplayMode.DAY, day.toString())
            val uptime = entry?.getTotalUptime() ?: 0.seconds

            val dayString = if (day == LocalDate.now()) "Today" else day.toString()

            val outputString = "    §e$dayString:    §b${uptime.format()}"

            totalUptime += uptime
            commandString += outputString
        }

        commandString += ""
        commandString += "§bTotal Uptime: §e${totalUptime.format()}"
        commandString += "§bAverage Uptime: §e${(totalUptime / days).format()}"
        commandString += "§r§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r"

        chat(commandString.joinToString("\n"), false)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shgardenuptime") {
            description = "Shows garden uptime history for past x days, defaults to 7"
            category = CommandCategory.USERS_ACTIVE
            argCallback("days", BrigadierArguments.integer(1, 30)) { days ->
                callback { onCommand(days) }
            }
            simpleCallback {
                callback { onCommand(7) }
            }
        }
    }
}
