package at.hannibal2.skyhanni.features.garden.plot

import at.hannibal2.skyhanni.events.garden.GardenPlotSprayEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.sprayonator.SprayType
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.world.phys.AABB
import kotlin.time.Duration

class GardenPlot(val id: Int, var inventorySlot: Int, val box: AABB, val middle: LorenzVec) {

    var name: String
        get() = getData()?.name ?: "$id"
        set(value) {
            getData()?.name = value
        }

    var pests: Int
        get() = getData()?.pests ?: 0
        set(value) {
            getData()?.pests = value
        }

    val currentSpray: GardenPlotApi.SprayData?
        get() = this.getData()?.let { plot ->
            val expiry = plot.sprayExpiryTime?.takeIf { !it.isInPast() } ?: return null
            val type = plot.sprayType ?: return null
            return GardenPlotApi.SprayData(expiry, type)
        }

    val isSprayExpired: Boolean
        get() = this.getData()?.let {
            !it.sprayHasNotified && it.sprayExpiryTime?.isInPast() == true
        } == true

    var isBeingPasted: Boolean
        get() = this.getData()?.isBeingPasted ?: false
        set(value) {
            this.getData()?.isBeingPasted = value
        }

    var isPestCountInaccurate: Boolean
        get() = this.getData()?.isPestCountInaccurate ?: false
        set(value) {
            this.getData()?.isPestCountInaccurate = value
        }

    var uncleared: Boolean
        get() = this.getData()?.uncleared ?: false
        set(value) {
            this.getData()?.uncleared = value
        }

    var locked: Boolean
        get() = this.getData()?.locked ?: false
        set(value) {
            this.getData()?.locked = value
        }

    var greenhouse: Boolean
        get() = this.getData()?.greenhouse ?: false
        set(value) {
            this.getData()?.greenhouse = value
        }

    val tpName get() = if (isBarn()) "barn" else name

    fun markExpiredSprayAsNotified() {
        getData()?.sprayHasNotified = true
    }

    fun setSpray(spray: SprayType, duration: Duration) {
        getData()?.apply {
            sprayType = spray
            sprayExpiryTime = SimpleTimeMark.now() + duration
            sprayHasNotified = false
        }
    }

    fun isBarn() = id == 0

    fun removeSpray() {
        getData()?.apply {
            sprayType = null
            sprayExpiryTime = SimpleTimeMark.now()
            sprayHasNotified = true
            val plot = GardenPlotApi.getPlotByID(id) ?: return
            GardenPlotSprayEvent.SprayRemovedEvent(plot, sprayType)
        }
    }

    fun isPlayerInside() = GardenPlotApi.fetchCurrentPlot() == this

    fun sendTeleportTo() = HypixelCommands.teleportToPlot(tpName)

    private fun getData() = GardenApi.storage?.plotData?.getOrPut(id) {
        GardenPlotApi.PlotData(
            id = id,
            name = "$id",
            pests = 0,
            sprayExpiryTime = null,
            sprayType = null,
            sprayHasNotified = false,
            isBeingPasted = false,
            isPestCountInaccurate = false,
            locked = true,
            uncleared = false,
            greenhouse = false,
        )
    }
}
