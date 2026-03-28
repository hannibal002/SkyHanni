package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.graph.GraphEditor
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.VectorUtils
import at.hannibal2.skyhanni.utils.VectorUtils.boundingToOffset
import at.hannibal2.skyhanni.utils.VectorUtils.copyLocations
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.expandBlock
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object ParkourWaypointSaver {

    private val config get() = DevApi.config.waypoint
    private var timeLastSaved = SimpleTimeMark.farPast()
    private var locations = mutableListOf<Vec3>()
    private var parkourHelper: ParkourHelper? = null

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        @Suppress("InSkyBlockEarlyReturn")
        if (!SkyBlockUtils.inSkyBlock && !config.parkourOutsideSB) return
        if (Minecraft.getInstance().screen != null) return
        if (GraphEditor.isEnabled()) return
        if (timeLastSaved.passedSince() < 250.milliseconds) return

        when (event.keyCode) {
            config.deleteKey -> {
                if (locations.isEmpty()) {
                    locations = VectorUtils.readListFromClipboard().toMutableList()
                } else {
                    if (PlayerUtils.isSneaking()) {
                        locations.clear()
                    } else {
                        locations.removeLast()
                    }
                    // update()
                }
            }

            config.saveKey -> {
                val newLocation = LocationUtils.getBlockBelowPlayer()
                if (locations.isNotEmpty() && newLocation == locations.last()) return
                locations.add(newLocation)
                update()
            }
        }
    }

    /**
     *       "-625:119:-962",
     *       "-626:121:-971",
     *       "-728:122:-998"
     */

    private fun update() {
        timeLastSaved = SimpleTimeMark.now()
        locations.copyLocations()
        parkourHelper = ParkourHelper(locations, emptyList()).also {
            it.showEverything = true
            it.rainbowColor = true
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        @Suppress("InSkyBlockEarlyReturn")
        if (!SkyBlockUtils.inSkyBlock && !config.parkourOutsideSB) return

        if (locations.size > 1) {
            parkourHelper?.render(event)
        } else if (locations.isNotEmpty()) {
            val aabb = locations.first().boundingToOffset(1.0, 1.0, 1.0).expandBlock()
            // TODO add chroma color support via config
            event.drawFilledBoundingBox(aabb, LorenzColor.GREEN.toChromaColor(), 1f)
        }
    }
}
