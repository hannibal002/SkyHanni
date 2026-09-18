package at.hannibal2.skyhanni.events.garden.visitor

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Event that is called when a visitor menu in the garden is opened.
 *
 * @param visitor The visitor that was opened.
 */
@PrimaryFunction("onVisitorOpen")
class VisitorOpenEvent(val visitor: VisitorApi.Visitor) : SkyHanniEvent()
