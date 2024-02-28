package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.config.features.garden.laneswitch.LaneswitchFarmConfig
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sendTitle
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds

class LaneswitchNotification {

    private val config get() = GardenAPI.config.laneswitch

    private var blocksPerSecond = 0.0
    private var lastPos = LorenzVec(0, 0, 0)
    private var lastLaneSwitch = SimpleTimeMark.farPast()

    private fun coverable(from: LorenzVec, to: LorenzVec, speed: Double, time: Int): Boolean {
        return from.distance(to) <= speed * time
    }

    companion object {
        private val config get() = GardenAPI.config.laneswitch

        @JvmStatic
        fun playUserSound() {
            SoundUtils.createSound(
                config.notification.sound.notificationSound,
                config.notification.sound.notificationPitch,
                config.notification.sound.notificationVolume
            ).playSound()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!config.enabled || !GardenAPI.inGarden()) return
        if (GardenCropSpeed.averageBlocksPerSecond <= 0.0 && config.notification.farmingOnly) return

        val playerPosition = LocationUtils.playerLocation().round(2)
        blocksPerSecond = playerPosition.distance(lastPos) * 20
        this.lastPos = playerPosition

        if (lastLaneSwitch.passedSince() >= config.notification.settings.notificationTimeout.seconds) {
            val farmEnd: List<LorenzVec> = when (config.farm.farmDirection) {
                LaneswitchFarmConfig.FarmDirection.NORTH, LaneswitchFarmConfig.FarmDirection.SOUTH -> {
                    val xValue = (config.farm.plotAmount * 48).toDouble()
                    listOf(
                        LorenzVec(xValue, playerPosition.y, playerPosition.z),
                        LorenzVec(-xValue, playerPosition.y, playerPosition.z)
                    )
                }

                LaneswitchFarmConfig.FarmDirection.EAST, LaneswitchFarmConfig.FarmDirection.WEST -> {
                    val zValue = (config.farm.plotAmount * 48).toDouble()
                    listOf(
                        LorenzVec(playerPosition.x, playerPosition.y, zValue),
                        LorenzVec(playerPosition.x, playerPosition.y, -zValue)
                    )
                }

                else -> emptyList()
            }
            if (coverable(playerPosition, farmEnd[0], blocksPerSecond, config.notification.settings.notificationThreshold) || playerPosition.distance(farmEnd[1]) <= blocksPerSecond * config.notification.settings.notificationThreshold) {
                sendTitle(config.notification.settings.notificationColor.getChatColor() + config.notification.settings.notificationText, config.notification.settings.notificationDuration.seconds)
                playUserSound()
                this.lastLaneSwitch = SimpleTimeMark.now()
            }
        }
    }
}
