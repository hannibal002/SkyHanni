package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object LowHealthAlert {

    private val config get() = SkyHanniMod.feature.dungeon.lowHealthAlert
    private val soundConfig get() = config.lowHealthAlertSound
    private var lastAlert: TitleManager.TitleContext? = null

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
            TitleManager.sendTitle("§c$username §ais low", "§c$health❤", 1.seconds)?.let {
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
