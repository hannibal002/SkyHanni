package at.hannibal2.skyhanni.events.utils

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * When the "init" phase of mod loading is done.
 */
@PrimaryFunction("onInitFinished")
object InitFinishedEvent : SkyHanniEvent()
