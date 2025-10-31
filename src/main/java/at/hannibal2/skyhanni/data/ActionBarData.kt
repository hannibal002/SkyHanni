package at.hannibal2.hanni.data

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.events.ActionBarUpdateEvent
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.StringUtils.stripHypixelMessage
import net.minecraft.util.IChatComponent

@HanniModule
object ActionBarData {
    private var actionBar = ""
    private var debugActionBar: String? = null

    fun getActionBar() = actionBar

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestactionbar") {
            description = "Set your clipboard as a fake action bar."
            category = CommandCategory.DEVELOPER_TEST
            callback { debugCommand() }
        }
    }

    private fun debugCommand() {
        HanniMod.launchCoroutine("action bar debug command") {
            val clipboard = OSUtils.readFromClipboard()
            if (debugActionBar == clipboard) {
                debugActionBar = null
                ChatUtils.chat("Disabled action bar test!")
            } else {
                debugActionBar = clipboard
                ChatUtils.chat("Set action bar test to '$clipboard'")
            }
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Action Bar")
        debugActionBar?.let {
            event.addData {
                add("debug active!")
                add("line: '$it'")
            }
        } ?: run {
            event.addIrrelevant("not active.")
        }
    }

    @HandleEvent
    fun onWorldChange() {
        actionBar = ""
    }

    /**
     * If the action bar is modified return the new one, otherwise return null.
     */
    fun onChatReceive(component: IChatComponent): IChatComponent? {
        val message = debugActionBar ?: component.formattedText.stripHypixelMessage()

        actionBar = message
        val actionBarEvent = ActionBarUpdateEvent(actionBar, component)
        actionBarEvent.post()

        if (component.formattedText != actionBarEvent.chatComponent.formattedText) {
            return actionBarEvent.chatComponent
        }
        return null
    }
}
