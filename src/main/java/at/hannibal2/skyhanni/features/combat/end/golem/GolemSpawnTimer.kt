package at.hannibal2.skyhanni.features.combat.end.golem

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tracks the delay between the End Stone Protector starting to rise and it becoming attackable,
 * so the spawn moment can be hit precisely. Rendered by [GolemDisplay].
 *
 * The countdown is driven by the server's own ticks, which only arrive while the server is
 * actually running. On a healthy server that is a plain one second per second; while it lags the
 * ticks stop and the countdown simply holds still. It can therefore never run fast or skip
 * numbers to catch up - it only ever waits.
 */
@SkyHanniModule
object GolemSpawnTimer {

    private val repoGroup = RepoPattern.group("combat.boss.protector.2.spawn")

    /** 21 seconds of server time. */
    private const val SPAWN_DELAY_TICKS = 420

    /** A server tick is 50ms by definition - the count itself already carries any lag. */
    private const val MILLIS_PER_TICK = 50.0

    /**
     * REGEX-TEST: §5☬ §r§dThe ground begins to shake as an End Stone Protector rises from below!
     */
    private val risingPattern by repoGroup.pattern(
        "chat.rising",
        ".*The ground begins to shake as an End Stone Protector rises from below!.*",
    )

    private var spawnTick: Int? = null

    /**
     * Remaining time until the golem can be attacked, zero once it has spawned.
     *
     * Scaling this by the measured tick rate was tried and removed: a changing rate moved the
     * displayed number up and down on its own, which is what made the countdown jump when the
     * server recovered. The tick count alone is already lag aware.
     */
    fun timeUntilSpawn(): Duration {
        val target = spawnTick ?: return 0.seconds
        val remainingTicks = target - MinecraftData.totalServerTicks
        if (remainingTicks <= 0) return 0.seconds
        return (remainingTicks * MILLIS_PER_TICK).milliseconds
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.message
        if (risingPattern.matches(message)) {
            spawnTick = MinecraftData.totalServerTicks + SPAWN_DELAY_TICKS
            return
        }
    }

    @HandleEvent
    private fun onIslandChange(event: IslandChangeEvent) {
        spawnTick = null
    }
}
