package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread
import net.minecraft.network.chat.Component

@Thread(RENDER)
class TabListUpdateEvent(val tabList: List<Component>) : SkyHanniEvent()
