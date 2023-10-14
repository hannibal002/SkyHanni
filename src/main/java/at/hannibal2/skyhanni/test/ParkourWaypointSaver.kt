package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

class ParkourWaypointSaver {
    private val config get() = SkyHanniMod.feature.dev.waypoint
    private var timeLastSaved: Long = 0
    private var locations = mutableListOf<LorenzVec>()
    private var parkourHelper: ParkourHelper? = null

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock && !config.parkourOutsideSB) return
        if (!Keyboard.getEventKeyState()) return
        if (NEUItems.neuHasFocus()) return
        if (System.currentTimeMillis() - timeLastSaved < 250) return

        Minecraft.getMinecraft().currentScreen?.let {
            if (it !is GuiInventory && it !is GuiChest && it !is GuiEditSign) return
        }

        val key = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        if (config.deleteKey == key) {
            locations = locations.dropLast(1).toMutableList()
            update()
        }
        if (config.saveKey == key) {
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
        timeLastSaved = System.currentTimeMillis()
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
        if (!LorenzUtils.inSkyBlock && !config.parkourOutsideSB) return

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