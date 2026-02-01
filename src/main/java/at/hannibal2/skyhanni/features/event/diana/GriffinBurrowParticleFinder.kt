package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.diana.BurrowDetectEvent
import at.hannibal2.skyhanni.events.diana.BurrowDugEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.core.particles.ParticleTypes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GriffinBurrowParticleFinder {

    private val config get() = SkyHanniMod.feature.event.diana

    private val burrows = mutableMapOf<LorenzVec, Burrow>()

    fun containsBurrow(location: LorenzVec): Boolean = burrows.containsKey(location)

    private val patternGroup = RepoPattern.group("event.diana.mythological.burrows")

    /**
     * REGEX-TEST: §eYou finished the Griffin burrow chain! §r§7(4/4)
     */
    private val finishedChainPattern by patternGroup.pattern(
        "chain-finished",
        "§eYou finished the Griffin burrow chain! §r§7\\(\\d+/\\d+\\)",
    )

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Griffin Burrow Particle Finder")

        if (!DianaApi.isDoingDiana()) {
            event.addIrrelevant("not doing diana")
            return
        }

        event.addData {
            add("burrows: ${burrows.size}")
            for (burrow in burrows.values) {
                val location = burrow.location
                val found = burrow.found
                add(location.printWithAccuracy(1))
                add(" type: " + burrow.getType())
                add(" found: $found")
                add(" ")
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB, priority = HandleEvent.LOW, receiveCancelled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!config.guess) return

        val type = ParticleType.entries.firstOrNull { it.check(event) } ?: return

        // TODO the rounding is a workaround, may need to be removed once we know what is going on exactly and can fix this properly
        val location = event.location.roundToBlock().down()

        val burrow = burrows.getOrPut(location) { Burrow(location) }
        val oldBurrowType = burrow.type

        when (type) {
            ParticleType.ENCHANT -> burrow.hasEnchant = true
            ParticleType.EMPTY -> burrow.type = 0
            ParticleType.MOB -> burrow.type = 1
            ParticleType.TREASURE -> burrow.type = 2
        }

        burrow.lastSeen = SimpleTimeMark.now()
        if (burrow.hasEnchant && burrow.hasFootstep && burrow.type != -1) {
            if (!burrow.found || burrow.type != oldBurrowType) {
                DelayedRun.runOrNextTick { BurrowDetectEvent(burrow.location, burrow.getType()).post() }
                burrow.found = true
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onTick() {
        if (GriffinBurrowHelper.shouldBurrowParticlesBeVisible()) {
            for ((location, burrow) in burrows.toMutableMap()) {
                if (burrow.lastSeen.passedSince() > 0.5.seconds) {
                    burrows.remove(location)
                }
            }
        }
    }

    // TODO move to ParticleUtils or similar
    // TODO remove the roundTo calls as they are only workarounds
    private enum class ParticleType(val check: ReceiveParticleEvent.() -> Boolean) {
        EMPTY(
            { type == ParticleTypes.ENCHANTED_HIT && count == 4 && speed == 0.01f && offset.roundTo(2) == LorenzVec(0.5, 0.1, 0.5) },
        ),
        MOB(
            { type == ParticleTypes.CRIT && count == 3 && speed == 0.01f && offset.roundTo(2) == LorenzVec(0.5, 0.1, 0.5) },
        ),
        TREASURE(
            { type == ParticleTypes.DRIPPING_LAVA && count == 2 && speed == 0.01f && offset.roundTo(2) == LorenzVec(0.35, 0.1, 0.35) },
        ),
        ENCHANT(
            {
                type == ParticleTypes.ENCHANT && count == 5 && speed == 0.05f && offset.roundTo(2) ==
                    LorenzVec(0.5, 0.4, 0.5)
            },
        )
    }

    @HandleEvent
    fun onWorldChange() {
        reset()
    }

    fun reset() {
        burrows.clear()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (!config.guess) return
        val message = event.message
        if (message.startsWith("§eYou dug out a Griffin Burrow!") ||
            finishedChainPattern.matches(message)
        ) {
            BurrowApi.lastBurrowRelatedChatMessage = SimpleTimeMark.now()
        }
        if (message == "§cDefeat all the burrow defenders in order to dig it!") {
            BurrowApi.lastBurrowRelatedChatMessage = SimpleTimeMark.now()
        }
    }

    @HandleEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        burrows.remove(event.burrowLocation)
    }

    class Burrow(
        var location: LorenzVec,
        var hasFootstep: Boolean = true,
        var hasEnchant: Boolean = false,
        var type: Int = -1,
        var found: Boolean = false,
        var lastSeen: SimpleTimeMark = SimpleTimeMark.now(),
    ) {

        fun getType(): BurrowType = when (this.type) {
            0 -> BurrowType.START
            1 -> BurrowType.MOB
            2 -> BurrowType.TREASURE
            else -> BurrowType.UNKNOWN
        }
    }

    private fun isEnabled() = DianaApi.isDoingDiana()
}
