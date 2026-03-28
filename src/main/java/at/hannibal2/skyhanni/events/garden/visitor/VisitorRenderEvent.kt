package at.hannibal2.skyhanni.events.garden.visitor

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.phys.Vec3

@PrimaryFunction("onVisitorRender")
class VisitorRenderEvent(
    val visitor: VisitorApi.Visitor,
    val location: Vec3,
    val parent: SkyHanniRenderWorldEvent,
) : SkyHanniEvent()
