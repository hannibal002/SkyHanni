package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BarnFishingTimer {

    private val barnLocation = LorenzVec(108, 89, -252)

    private var tick = 0
    private var rightLocation = false
    private var currentCount = 0
    private var startTime = 0L

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.barnTimer) return

        tick++

        if (tick % 60 == 0) checkIsland()

        if (!rightLocation) return

        if (tick % 5 == 0) checkMobs()
        if (tick % 7 == 0) tryPlaySound()
    }

    private fun tryPlaySound() {
        if (currentCount == 0) return

        val duration = System.currentTimeMillis() - startTime
        val barnTimerAlertTime = SkyHanniMod.feature.fishing.barnTimerAlertTime * 1_000
        if (duration > barnTimerAlertTime && duration < barnTimerAlertTime + 3_000) {
            SoundUtils.playBeepSound()
        }
    }

    private fun checkMobs() {
        val newCount = countMobs()
        if (currentCount == 0) {
            if (newCount > 0) {
                startTime = System.currentTimeMillis()
            }
        }

        currentCount = newCount
        if (newCount == 0) {
            startTime = 0
        }
    }

    private fun countMobs() = EntityUtils.getAllEntities<EntityArmorStand>()
        .map { it.name }
        .count { it.endsWith("§c❤") }

    private fun checkIsland() {
        if (LorenzUtils.skyBlockIsland == IslandType.THE_FARMING_ISLANDS) {
            rightLocation = false
            return
        }

        rightLocation = LocationUtils.playerLocation().distance(barnLocation) < 50
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.barnTimer) return
        if (!rightLocation) return
        if (currentCount == 0) return

        val duration = System.currentTimeMillis() - startTime
        val barnTimerAlertTime = SkyHanniMod.feature.fishing.barnTimerAlertTime * 1_000
        val color = if (duration > barnTimerAlertTime) "§c" else "§e"
        val timeFormat = TimeUtils.formatDuration(duration, biggestUnit = TimeUnit.MINUTE)
        val name = if (currentCount == 1) "sea creature" else "sea creatures"
        val text = "$color$timeFormat §8(§e$currentCount §b$name§8)"

        SkyHanniMod.feature.fishing.barnTimerPos.renderString(text, posLabel = "BarnTimer")
    }
}