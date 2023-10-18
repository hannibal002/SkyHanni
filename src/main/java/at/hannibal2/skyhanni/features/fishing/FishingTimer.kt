package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FishingTimer {
    private val config get() = SkyHanniMod.feature.fishing.barnTimer
    private val barnLocation = LorenzVec(108, 89, -252)

    private var rightLocation = false
    private var currentCount = 0
    private var startTime = 0L
    private var inHollows = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return

        if (event.repeatSeconds(3)) {
            rightLocation = isRightLocation()
        }

        if (!rightLocation) return

        if (event.isMod(5)) checkMobs()
        if (event.isMod(7)) tryPlaySound()
        if (config.manualResetTimer.isKeyHeld()) startTime = System.currentTimeMillis()
    }

    private fun tryPlaySound() {
        if (currentCount == 0) return

        val duration = System.currentTimeMillis() - startTime
        val barnTimerAlertTime = config.alertTime * 1_000
        if (duration > barnTimerAlertTime && duration < barnTimerAlertTime + 3_000) {
            SoundUtils.playBeepSound()
        }
    }

    private fun checkMobs() {
        val newCount = countMobs()

        if (currentCount == 0 && newCount > 0) {
            startTime = System.currentTimeMillis()
        }

        currentCount = newCount
        if (newCount == 0) {
            startTime = 0
        }

        if (inHollows && newCount >= 60 && config.wormLimitAlert) {
            SoundUtils.playBeepSound()
        }
    }

    private fun countMobs() = EntityUtils.getEntities<EntityArmorStand>()
        .map { entity ->
            val name = entity.name
            val isSummonedSoul = name.contains("'")
            val hasFishingMobName = SeaCreatureManager.allFishingMobs.keys.any { name.contains(it) }
            if (hasFishingMobName && !isSummonedSoul) {
                if (name == "Sea Emperor" || name == "Rider of the Deep") 2 else 1
            } else 0
        }.sum()

    private fun isRightLocation(): Boolean {
        inHollows = false

        if (config.forStranded && LorenzUtils.isStrandedProfile) return true

        if (config.crystalHollows && IslandType.CRYSTAL_HOLLOWS.isInIsland()) {
            inHollows = true
            return true
        }

        if (!IslandType.THE_FARMING_ISLANDS.isInIsland()) {
            return LocationUtils.playerLocation().distance(barnLocation) < 50
        }

        return false
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        if (!rightLocation) return
        if (currentCount == 0) return

        val duration = System.currentTimeMillis() - startTime
        val barnTimerAlertTime = config.alertTime * 1_000
        val color = if (duration > barnTimerAlertTime) "§c" else "§e"
        val timeFormat = TimeUtils.formatDuration(duration, biggestUnit = TimeUnit.MINUTE)
        val name = if (currentCount == 1) "sea creature" else "sea creatures"
        val text = "$color$timeFormat §8(§e$currentCount §b$name§8)"

        config.pos.renderString(text, posLabel = "BarnTimer")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "fishing.barnTimer", "fishing.barnTimer.enabled")
        event.move(3, "fishing.barnTimerAlertTime", "fishing.barnTimer.alertTime")
        event.move(3, "fishing.barnTimerCrystalHollows", "fishing.barnTimer.crystalHollows")
        event.move(3, "fishing.barnTimerForStranded", "fishing.barnTimer.forStranded")
        event.move(3, "fishing.wormLimitAlert", "fishing.barnTimer.wormLimitAlert")
        event.move(3, "fishing.manualResetTimer", "fishing.barnTimer.manualResetTimer")
        event.move(3, "fishing.barnTimerPos", "fishing.barnTimer.pos")
    }
}