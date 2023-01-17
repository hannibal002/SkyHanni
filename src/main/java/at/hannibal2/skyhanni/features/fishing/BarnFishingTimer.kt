package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BarnFishingTimer {

    private val barnLocation = LorenzVec(108, 89, -252)

    private var tick = 0
    private var rightLocation = false
    private var mobsCount = 0
    private var startTime = 0L

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.barnTimer) return

        tick++

        if (tick % 60 == 0) {
            checkIsland()
        }
//        if (tick % 20 == 0) {
        if (tick % 5 == 0) {
            checkMobs()
        }
    }

    private fun checkMobs() {
        val newCounter = countMobs()

        if (mobsCount == 0) {
            if (newCounter > 0) {
                startTimer()
            }
        }
        mobsCount = newCounter

        if (newCounter == 0) {
            startTime = 0
        }
    }

    private fun countMobs(): Int {
        val counter = Minecraft.getMinecraft().theWorld.loadedEntityList
            .filterIsInstance<EntityArmorStand>()
            .map { it.name }
            //            .count { it.startsWith("§8[§7Lv") && it.endsWith("§c❤") }
            .count { it.endsWith("§c❤") }
        return counter
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
    }

    private fun checkIsland() {
        if (LorenzUtils.skyBlockIsland == IslandType.THE_FARMING_ISLANDS) {
            rightLocation = false
            return
        }

        rightLocation = LocationUtils.playerLocation().distance(barnLocation) < 50
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.barnTimer) return
        if (!rightLocation) return

        if (mobsCount == 0) return

        val duration = System.currentTimeMillis() - startTime
        val format = StringUtils.formatDuration(duration / 1000, decimalFormat = true)
        val text = "§e$format §8(§e$mobsCount §bsea creatures§8)"

        SkyHanniMod.feature.fishing.barnTimerPos.renderString(text)
    }
}