package at.hannibal2.skyhanni.events.utils

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * When the "pre init" phase of mod loading is done.
 */
@PrimaryFunction("onPreInitFinished")
object PreInitFinishedEvent : SkyHanniEvent()
