package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.network.chat.Component

class EntityHealthDisplayEvent(var text: Component) : SkyHanniEvent()
