package at.hannibal2.skyhanni.events.garden.visitor

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraftforge.client.event.RenderWorldLastEvent

class VisitorRenderEvent(val visitor: VisitorAPI.Visitor, val location: LorenzVec, val parent: RenderWorldLastEvent) : LorenzEvent()