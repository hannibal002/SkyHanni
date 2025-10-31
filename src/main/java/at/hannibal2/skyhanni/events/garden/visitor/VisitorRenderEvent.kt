package at.hannibal2.hanni.events.garden.visitor

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.garden.visitor.VisitorApi
import at.hannibal2.hanni.utils.LorenzVec

class VisitorRenderEvent(val visitor: VisitorApi.Visitor, val location: LorenzVec, val parent: HanniRenderWorldEvent) : HanniEvent()
