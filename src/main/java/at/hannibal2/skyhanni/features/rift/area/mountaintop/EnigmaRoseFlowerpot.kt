package at.hannibal2.skyhanni.features.rift.area.mountaintop

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import net.minecraft.util.AxisAlignedBB

@SkyHanniModule
object EnigmaRoseFlowerpot {
    private val config get() = SkyHanniMod.feature.rift.area.mountaintop

    private val area = AxisAlignedBB(25.0, 165.0, 90.0, 52.0, 185.0, 120.0)
    private val dropLocation = LorenzVec(40, 161, 116)
    private var inArea = false

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(2)) {
            inArea = area.isPlayerInside()
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled() && !inArea) return

        event.drawWaypointFilled(dropLocation, LorenzColor.WHITE.toColor(), beacon = true)
        event.drawDynamicText(dropLocation, "Drop", 1.5)
    }

    private fun isEnabled() = IslandType.THE_RIFT.isCurrent() && config.enigmaRoseFlowerpot
}
