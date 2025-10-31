package at.hannibal2.hanni.events.diana

import at.hannibal2.hanni.api.event.HanniEvent
import net.minecraft.client.entity.EntityOtherPlayerMP

class InquisitorFoundEvent(val inquisitorEntity: EntityOtherPlayerMP) : HanniEvent()
