package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector

object TrackerUtils {

    var currentDisplayMode = DisplayMode.TOTAL

    fun MutableList<List<Any>>.addDisplayModeToggle(update: () -> Unit) {
        addSelector<DisplayMode>(
            "§7Display Mode: ",
            getName = { type -> type.displayName },
            isCurrent = { it == currentDisplayMode },
            onChange = {
                currentDisplayMode = it
                update()
            }
        )
    }

    fun resetCommand(name: String, command: String, args: Array<String>, data: TrackerData, update: () -> Unit) {
        if (args.size == 1 && args[0].lowercase() == "confirm") {
            reset(data, update)
            LorenzUtils.chat("§e[SkyHanni] You reset your $name data!")
            return
        }

        LorenzUtils.clickableChat(
            "§e[SkyHanni] Are you sure you want to reset all your $name data? Click here to confirm.",
            "$command confirm"
        )
    }

    fun reset(data: TrackerData, update: () -> Unit) {
        data.reset()
        update()
    }
}
