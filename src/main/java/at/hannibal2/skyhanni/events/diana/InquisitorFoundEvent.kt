package at.hannibal2.skyhanni.events.diana

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.client.network.OtherClientPlayerEntity

class InquisitorFoundEvent(val inquisitorEntity: OtherClientPlayerEntity) : SkyHanniEvent()
