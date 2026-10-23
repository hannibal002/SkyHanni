package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object LowHealthAlert {
    private val config get() = SkyHanniMod.feature.dungeon.lowHealthAlert
    private val soundConfig get() = config.lowHealthAlertSound
    private var lastAlert: TitleContext? = null

    /**
     * REGEX-TEST: §a[H] §6Eisengolem §7[Lv48]
     * REGEX-TEST: §e[M] §b04032006 §a7,361§c❤
     */
    @Suppress("MaxLineLength")
    val teammatesPattern by RepoPattern.pattern(
        "dungeon.low-health-alert.teammates",
        "(?:§.)*(?<classAbbv>\\[\\w]) (?:§.)*(?<username>\\w{2,16}) (?:(?:§.)*(?<classLevel>\\[Lvl?(?<level>[\\w,.]+)?]?)|(?:§(?<color>.))*(?<health>[\\w,.]+)(?:§.)*.?)",
    )

    private val alertSound get() = SoundUtils.createSound(soundConfig.alertSound, soundConfig.pitch, isWarning = true)

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        if (!isEnabled()) return
        teammatesPattern.matchAll(event.added) {
            val username = group("username")
            val color = group("color")
            val health = group("health")
            if (color != "c" || health == "DEAD") return

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
    fun playTestSound() = alertSound.playSound()

    private fun isEnabled() =
        config.enabled && DungeonApi.active && (!config.onlyWhileHealer || DungeonApi.playerClass == HEALER)
}
