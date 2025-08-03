package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.LorenzVec.Companion.toLorenzVec
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.expandBlock
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object ParkourWaypointSaver {

    private val config get() = SkyHanniMod.feature.dev.waypoint
    private var timeLastSaved = SimpleTimeMark.farPast()
    private var locations = mutableListOf<LorenzVec>()
    private var parkourHelper: ParkourHelper? = null

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        @Suppress("InSkyBlockEarlyReturn")
        if (!SkyBlockUtils.inSkyBlock && !config.parkourOutsideSB) return
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (NeuItems.neuHasFocus()) return
        if (SkyHanniMod.feature.dev.devTool.graph.enabled) return
        if (timeLastSaved.passedSince() < 250.milliseconds) return

        when (event.keyCode) {
            config.deleteKey -> {
                if (locations.isEmpty()) {
                    loadClipboard()
                } else {
                    if (MinecraftCompat.localPlayer.isSneaking) {
                        locations.clear()
                    } else {
                        locations = locations.dropLast(1).toMutableList()
                    }
//                     update()
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

    private fun loadClipboard() {
        SkyHanniMod.launchCoroutine {
            val clipboard = OSUtils.readFromClipboard() ?: return@launchCoroutine
            locations = clipboard.split("\n").map { line ->
                val raw = line.replace("\"", "").replace(",", "")
                raw.split(":").map { it.toDouble() }.toLorenzVec()
            }.toMutableList()
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

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        @Suppress("InSkyBlockEarlyReturn")
        if (!SkyBlockUtils.inSkyBlock && !config.parkourOutsideSB) return

        if (locations.size > 1) {
            parkourHelper?.render(event)
        } else {
            for (location in locations) {
                val aabb = location.boundingToOffset(1.0, 1.0, 1.0).expandBlock()
                // TODO add chroma color support via config
                event.drawFilledBoundingBox(aabb, LorenzColor.GREEN.toChromaColor(), 1f)
            }
        }
    }
}
