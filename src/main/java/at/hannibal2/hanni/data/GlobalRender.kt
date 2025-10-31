package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils

/**
 * This not only toggles all mixins that render hanni elements in 2d or 3d,
 * (e.g. renderables, entity highlight, inventory background, slot number)
 * but also all render related logic of all features. (e.g. hide entites, tab list, etc)
 * With this toggle set to false, only hanni features that visually work are chat messages.
 *
 * Note that this does not change the internal data fetching and processing for non render focused features.
 *
 * The debug command is useful to instantly see if a performance problem or mod conflict is due to Hanni or not.
 */
@HanniModule
object GlobalRender {

    val renderDisabled get() = !enabled
    private var enabled = true

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shrendertoggle") {
            description = "Disables/enables the rendering of all hanni guis."
            category = CommandCategory.USERS_BUG_FIX
            callback {
                enabled = !enabled
                if (enabled) {
                    ChatUtils.chat("§aEnabled global renderer!")
                } else {
                    ChatUtils.chat("§cDisabled global renderer! Run this command again to show Hanni rendering again.")
                }
            }
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Global Render")
        if (enabled) {
            event.addIrrelevant("normal enabled")
        } else {
            event.addData {
                add("Global renderer is disabled!")
                add("No rendering-related features from Hanni will show up anywhere!")
            }
        }
    }
}
