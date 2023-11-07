package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.renderables.Renderable

object TrackerUtils {

    fun MutableList<List<Any>>.addSessionResetButton(name: String, data: SharedTracker<*>?, update: () -> Unit) {
        addAsSingletonList(
            Renderable.clickAndHover(
                "§cReset session!",
                listOf(
                    "§cThis will reset your",
                    "§ccurrent session for",
                    "§c$name"
                ),
            ) {
                data?.get(DisplayMode.SESSION)?.let {
                    reset(it) {
                        update()
                    }
                }
            })
    }

    fun resetCommand(name: String, command: String, args: Array<String>, data: SharedTracker<*>?, update: () -> Unit) {
        if (args.size == 1 && args[0].lowercase() == "confirm") {
            reset(data?.get(DisplayMode.TOTAL)) {
                update()
                LorenzUtils.chat("§e[SkyHanni] You reset your $name data!")
            }
            return
        }

        LorenzUtils.clickableChat(
            "§e[SkyHanni] Are you sure you want to reset all your $name data? Click here to confirm.",
            "$command confirm"
        )
    }

    private fun reset(data: TrackerData?, update: () -> Unit) {
        data?.let {
            it.reset()
            update()
        }
    }
}
