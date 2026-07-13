package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import net.minecraft.network.chat.Component

@Thread(RENDER)
class TablistFooterUpdateEvent(val footer: Component) : SkyHanniEvent()
