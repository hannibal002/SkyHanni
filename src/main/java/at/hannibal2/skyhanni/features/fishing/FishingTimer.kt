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
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FishingTimer {

    private val config get() = SkyHanniMod.feature.fishing.barnTimer
    private val barnLocation = LorenzVec(108, 89, -252)

    private var rightLocation = false
    private var currentCount = 0
    private var startTime = SimpleTimeMark.farPast()
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
        if (config.manualResetTimer.isKeyHeld() && Minecraft.getMinecraft().currentScreen == null) {
            startTime = SimpleTimeMark.now()
        }
    }

    private fun tryPlaySound() {
        if (currentCount == 0) return

        val passedSince = startTime.passedSince()
        val barnTimerAlertTime = (config.alertTime * 1_000).milliseconds
        if (passedSince in barnTimerAlertTime..(barnTimerAlertTime + 3.seconds)) {
            SoundUtils.playBeepSound()
        }
    }

    private fun checkMobs() {
        val newCount = countMobs()

        if (currentCount == 0 && newCount > 0) {
            startTime = SimpleTimeMark.now()
        }

        currentCount = newCount
        if (newCount == 0) {
            startTime = SimpleTimeMark.farPast()
        }

        if (inHollows && newCount >= 60 && config.wormLimitAlert) {
            SoundUtils.playBeepSound()
            LorenzUtils.sendTitle("§cWORM CAP FULL!!!", 2.seconds)
        }
    }

    private fun countMobs() =
        EntityUtils.getEntities<EntityArmorStand>().map { entity -> FishingAPI.seaCreatureCount(entity) }.sum()

    private fun isRightLocation(): Boolean {
        inHollows = false

        if (config.forStranded && LorenzUtils.isStrandedProfile) return true

        if (config.crystalHollows && IslandType.CRYSTAL_HOLLOWS.isInIsland()) {
            inHollows = true
            return true
        }

        if (config.crimsonIsle && IslandType.CRIMSON_ISLE.isInIsland()) return true

        if (config.winterIsland && IslandType.WINTER.isInIsland()) return true

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
        if (!FishingAPI.isFishing()) return

        val passedSince = startTime.passedSince()
        val barnTimerAlertTime = (config.alertTime * 1_000).milliseconds
        val color = if (passedSince > barnTimerAlertTime) "§c" else "§e"
        val timeFormat = passedSince.format(TimeUnit.MINUTE)
        val name = StringUtils.pluralize(currentCount, "sea creature")
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
