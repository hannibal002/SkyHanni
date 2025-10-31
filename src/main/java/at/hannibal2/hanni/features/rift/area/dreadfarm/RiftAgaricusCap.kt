package at.hannibal2.hanni.features.rift.area.dreadfarm

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.BlockUtils
import at.hannibal2.hanni.utils.BlockUtils.getBlockAt
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import net.minecraft.init.Blocks

@HanniModule
object RiftAgaricusCap {

    private val config get() = RiftApi.config.area.dreadfarm
    private var startTime = SimpleTimeMark.farPast()
    private var location: LorenzVec? = null
    private var inArea: Boolean = false

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return

        location = updateLocation()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inArea = event.area == "Dreadfarm" || event.area == "West Village"
    }

    private fun updateLocation(): LorenzVec? {
        if (InventoryUtils.getItemInHand()?.getInternalName() != RiftApi.farmingTool) return null
        val currentLocation = BlockUtils.getTargetedBlock() ?: return null

        when (currentLocation.getBlockAt()) {
            Blocks.brown_mushroom -> {
                return if (location != currentLocation) {
                    startTime = SimpleTimeMark.now()
                    currentLocation
                } else {
                    if (startTime.isFarFuture()) {
                        startTime = SimpleTimeMark.now()
                    }
                    location
                }
            }

            Blocks.red_mushroom -> {
                if (location == currentLocation) {
                    startTime = SimpleTimeMark.farFuture()
                    return location
                }
            }
        }
        return null
    }

    @HandleEvent
    fun onWorldChange() {
        reset()
    }

    private fun reset() {
        startTime = SimpleTimeMark.farPast()
        location = null
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return

        val location = location?.up(0.6) ?: return

        if (startTime.isFarFuture()) {
            event.drawDynamicText(location, "§cClick!", 1.5)
            return
        }

        val format = startTime.passedSince().format(showMilliSeconds = true)
        event.drawDynamicText(location, "§b$format", 1.5)
    }

    fun isEnabled() = RiftApi.inRift() && inArea && config.agaricusCap

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "rift.area.dreadfarmConfig", "rift.area.dreadfarm")
    }
}
