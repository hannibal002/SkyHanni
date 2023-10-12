package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class ParkourWaypointSaver {
    private val config get() = SkyHanniMod.feature.dev.waypoint
    private var timeLastSaved = SimpleTimeMark.farPast()
    private var locations = mutableListOf<LorenzVec>()
    private var parkourHelper: ParkourHelper? = null

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (NEUItems.neuHasFocus()) return
        if (timeLastSaved.passedSince() < 250.milliseconds) return

        if (config.deleteKey == event.keyCode) {
            locations = locations.dropLast(1).toMutableList()
            update()
        }
        if (config.saveKey == event.keyCode) {
            val newLocation = LorenzVec.getBlockBelowPlayer()
            if (locations.isNotEmpty() && newLocation == locations.last()) return
            locations.add(newLocation)
            update()
        }
    }

    private fun update() {
        locations.copyLocations()
        parkourHelper = ParkourHelper(locations, emptyList()).also {
            it.showEverything = true
            it.rainbowColor = true
        }
    }

    private fun MutableList<LorenzVec>.copyLocations() {
        val resultList = mutableListOf<String>()
        timeLastSaved = SimpleTimeMark.now()
        for (location in this) {
            val x = location.x.toString().replace(",", ".")
            val y = location.y.toString().replace(",", ".")
            val z = location.z.toString().replace(",", ".")
            resultList.add("\"$x:$y:$z\"")
        }
        OSUtils.copyToClipboard(resultList.joinToString((",\n")))
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (locations.size > 1) {
            parkourHelper?.render(event)
        } else {
            for (location in locations) {
                val aabb = location.boundingToOffset(1.0, 1.0, 1.0).expandBlock()
                event.drawFilledBoundingBox_nea(aabb, LorenzColor.GREEN.toColor(), 1f)
            }
        }
    }
}