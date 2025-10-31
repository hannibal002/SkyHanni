package at.hannibal2.hanni.features.rift.area.mountaintop

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.util.AxisAlignedBB

@HanniModule
object EnigmaRoseFlowerpot {
    private val config get() = HanniMod.feature.rift.area.mountaintop

    private val area = AxisAlignedBB(25.0, 165.0, 90.0, 52.0, 185.0, 120.0)
    private val dropLocation = LorenzVec(40, 161, 116)
    private var inArea = false

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(2)) {
            inArea = area.isPlayerInside()
        }
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled() || !inArea) return

        event.drawWaypointFilled(dropLocation, LorenzColor.WHITE.toColor(), beacon = true)
        event.drawDynamicText(dropLocation, "Drop", 1.5)
    }

    private fun isEnabled() = IslandType.THE_RIFT.isCurrent() && config.enigmaRoseFlowerpot
}
