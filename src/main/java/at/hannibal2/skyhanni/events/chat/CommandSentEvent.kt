package at.hannibal2.skyhanni.events.chat

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent

class CommandSentEvent(
    val fullCommand: String,
    val command: String,
    val args: List<String>,
) : CancellableSkyHanniEvent()
