package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.network.chat.Component

class TablistFooterUpdateComponentEvent(val footer: Component) : SkyHanniEvent()
