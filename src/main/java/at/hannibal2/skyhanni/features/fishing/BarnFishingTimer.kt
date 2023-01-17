package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.StringUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.PositionedSound
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BarnFishingTimer {

    private val barnLocation = LorenzVec(108, 89, -252)

    private var tick = 0
    private var rightLocation = false
    private var currentCount = 0
    private var startTime = 0L

    private var sound = object : PositionedSound(ResourceLocation("random.orb")) {
        init {
            volume = 50f
            repeat = false
            repeatDelay = 0
            attenuationType = ISound.AttenuationType.NONE
        }
    }

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
            sound.playSound()
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

    private fun countMobs() = Minecraft.getMinecraft().theWorld.loadedEntityList
        .filterIsInstance<EntityArmorStand>()
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
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.fishing.barnTimer) return
        if (!rightLocation) return

        if (currentCount == 0) return

        val duration = System.currentTimeMillis() - startTime

        val barnTimerAlertTime = SkyHanniMod.feature.fishing.barnTimerAlertTime * 1_000
        val color = if (duration > barnTimerAlertTime) "§c" else "§e"
        val format = StringUtils.formatDuration(duration / 1000, decimalFormat = true)
        val text = "$color$format §8(§e$currentCount §bsea creatures§8)"

        SkyHanniMod.feature.fishing.barnTimerPos.renderString(text)
    }
}