package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndBossDeathEvent
import at.hannibal2.skyhanni.events.EndBossFightEndEvent
import at.hannibal2.skyhanni.events.EndBossSpawnEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.combat.end.dragon.DragonFightState
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

/**
 * Measures how much damage per second the player dealt to an End boss.
 *
 * Each boss is timed on its own: dragons and the protector fight in the same world and regularly
 * overlap, so a single shared timer would let one fight overwrite the start of the other.
 *
 * The fight is timed in server ticks rather than the client's game time. Server ticks only arrive
 * while the server is actually running, so a lag spike stretches neither the duration nor the DPS.
 * The game time instead keeps running at 20 per second and snaps back to the server value when it
 * catches up, which would make the result jump depending on where the snap lands.
 *
 * The damage itself comes from Hypixel's own end-of-fight summary, which is exact - deriving it
 * from the scoreboard would only ever be an estimate.
 */
@SkyHanniModule
object EndDpsTracker {

    private val config get() = SkyHanniMod.feature.combat.endIsland.dps

    private const val TICKS_PER_SECOND = 20.0

    /** Server tick each boss became attackable at, removed once it died. */
    private val fightStartTick = mutableMapOf<EndBoss, Int>()

    /** Ticks each boss was alive for, kept until its summary has been read. */
    private val fightTicks = mutableMapOf<EndBoss, Int>()

    /** How long the protector's damage stays on its overlay after it died. */
    private val LINGER = 10.seconds

    /** Damage of the protector fight that just ended, shown for [LINGER] and then dropped. */
    private var finalProtectorDamage: Double? = null
    private var protectorDiedAt = SimpleTimeMark.farPast()

    @HandleEvent
    private fun onEndBossSpawn(event: EndBossSpawnEvent) {
        fightStartTick[event.boss] = MinecraftData.totalServerTicks
        fightTicks.remove(event.boss)
        if (event.boss == EndBoss.END_STONE_PROTECTOR) clearProtectorResult()
    }

    /**
     * The duration is taken here rather than at the summary, which only prints a moment later and
     * would count that delay as fight time.
     */
    @HandleEvent
    private fun onEndBossDeath(event: EndBossDeathEvent) {
        val start = fightStartTick.remove(event.boss) ?: return
        fightTicks[event.boss] = MinecraftData.totalServerTicks - start

        if (event.boss == EndBoss.END_STONE_PROTECTOR) {
            // Frozen here so the damage stops moving the moment the protector dies, and stays
            // readable for a few seconds instead of vanishing with it.
            finalProtectorDamage = DragonFightState.yourDamage
            protectorDiedAt = SimpleTimeMark.now()
        }
    }

    @HandleEvent
    private fun onEndBossFightEnd(event: EndBossFightEndEvent) {
        // Taken out even when the message is off, so a later fight never reuses a stale duration.
        val ticks = fightTicks.remove(event.boss) ?: return
        if (!config.chatMessage) return
        val seconds = (ticks / TICKS_PER_SECOND).takeIf { it > 0 } ?: return

        val label = if (event.boss == EndBoss.DRAGON) "Dragon DPS" else "Protector DPS"
        val own = "§d${(event.yourDamage / seconds).shortFormat()}"
        val duration = "§7(${seconds.roundTo(1)}s)"
        // Skipped when the player topped the damage themselves - repeating their own number
        // under a different label only reads as an error.
        val top = event.topDamage
            .takeIf { config.showTopDamager && it > 0 && it > event.yourDamage }
            ?.let { " §8| §7Top: §d${(it / seconds).shortFormat()}" }
            .orEmpty()

        // Without the prefix the line sits where Hypixel's own centered summary does; with it, the
        // indent shrinks by its width so the text still lines up.
        val indent = if (config.skyhanniMessagePrefix) 16 else 30
        ChatUtils.chat(
            "§f${" ".repeat(indent)}§r§eYour $label: $own $duration$top",
            prefix = config.skyhanniMessagePrefix,
        )
    }

    /** Damage dealt to the running protector, or the last result while it is still being shown. */
    fun displayDamage(): Double? {
        if (EndBoss.END_STONE_PROTECTOR in fightStartTick) {
            return DragonFightState.yourDamage.takeIf { it > 0 }
        }
        return finalProtectorDamage?.takeIf { protectorDiedAt.passedSince() < LINGER }
    }

    private fun clearProtectorResult() {
        finalProtectorDamage = null
        protectorDiedAt = SimpleTimeMark.farPast()
    }

    /** Also covers hopping to another server, which ends any fight that was running. */
    @HandleEvent
    private fun onWorldChange(event: WorldChangeEvent) {
        fightStartTick.clear()
        fightTicks.clear()
        clearProtectorResult()
    }
}
