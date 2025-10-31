package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.title.TitleContext
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.ScoreboardUpdateEvent
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matchAll
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds

@HanniModule
object LowHealthAlert {

    private val config get() = HanniMod.feature.dungeon.lowHealthAlert
    private val soundConfig get() = config.lowHealthAlertSound
    private var lastAlert: TitleContext? = null

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        if (!isEnabled()) return
        ScoreboardPattern.teammatesPattern.matchAll(event.added) {
            val username = group("username")
            val color = group("color")
            val health = group("health")
            if (color != "c" || health == "DEAD") return

            val alertSound = SoundUtils.createSound(soundConfig.alertSound, soundConfig.pitch)
            SoundUtils.repeatSound(100, soundConfig.repeatSound, alertSound)
            lastAlert?.stop()
            TitleManager.sendTitle(
                "§c$username §ais low",
                "§c$health❤",
                1.seconds
            )?.let {
                lastAlert = it
            }
        }
    }

    @JvmStatic
    fun playTestSound() {
        with(soundConfig) {
            SoundUtils.createSound(alertSound, pitch).playSound()
        }
    }

    private fun isEnabled() =
        config.enabled && DungeonApi.active && (!config.onlyWhileHealer || DungeonApi.playerClass == DungeonApi.DungeonClass.HEALER)
}
