package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils.chat
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenUptimeCommand {
    private val storage get() = GardenApi.storage?.gardenBpsTracker

    fun onCommand(args: Array<String>) {
        val dayAmount = args.getOrNull(0)?.toIntOrNull()?.coerceAtMost(31) ?: 7

        val date = LocalDate.now()
        var totalUptime = 0.seconds

        val commandString = mutableListOf(
            "§r§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r",
            "§b${PlayerUtils.getName()}'s§e garden uptime for the past §a$dayAmount §edays:",
            ""
        )

        for (num in 0..<dayAmount) {

            val day = date.minusDays(num.toLong())
            val entry = storage?.getEntry(SkyHanniTracker.DisplayMode.DAY, day)
            val uptime = entry?.getTotalUptime() ?: 0.seconds

            val dayString = if (day == LocalDate.now()) "Today" else day.toString()

            val outputString = "    §e$dayString:    §b$uptime"

            totalUptime += uptime
            commandString += outputString
        }

        commandString += ""
        commandString += "§bTotal Uptime: §e$totalUptime"
        commandString += "§bAverage Uptime: §e${(totalUptime / dayAmount)}"
        commandString += "§r§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r"

        chat(commandString.joinToString("\n"), false)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shgardenuptime") {
            description = "Shows garden uptime history for past x days, defaults to 7"
            category = CommandCategory.USERS_ACTIVE
            callback { onCommand(it) }
        }
    }
}
