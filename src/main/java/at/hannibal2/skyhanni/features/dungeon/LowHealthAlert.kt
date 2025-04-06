package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object LowHealthAlert {

    private val config get() = SkyHanniMod.feature.dungeon.lowHealthAlert
    private val soundConfig get() = config.lowHealthAlertSoundConfig
    private var lastAlert = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        for (line in event.added) {
            ScoreboardPattern.teammatesPattern.matchMatcher(line) {
                val username = group("username")
                val color = group("color")
                val health = group("health")
                if (color == "c" && health != null && health != "DEAD") {
                    val isHealer = DungeonApi.playerClass == DungeonApi.DungeonClass.HEALER
                    val shouldAlert = isEnabled() && (!config.onlyWhileHealer || isHealer) && lastAlert.passedSince() > 1.5.seconds

                    if (shouldAlert) {
                        lastAlert = SimpleTimeMark.now()
                        val alertSound = SoundUtils.createSound(soundConfig.alertSound, soundConfig.pitch)
                        SoundUtils.repeatSound(100, soundConfig.repeatSound, alertSound)
                        TitleManager.sendTitle("§c$username §ais low", "§c$health❤", 2.seconds)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun playTestSound() {
        with(soundConfig) {
            SoundUtils.createSound(alertSound, pitch).playSound()
        }
    }

    private fun isEnabled() = config.enabled
}
