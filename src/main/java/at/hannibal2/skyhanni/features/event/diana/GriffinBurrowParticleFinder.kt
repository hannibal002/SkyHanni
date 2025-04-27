package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.diana.BurrowDetectEvent
import at.hannibal2.skyhanni.events.diana.BurrowDugEvent
import at.hannibal2.skyhanni.features.event.diana.DianaApi.isDianaSpade
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.init.Blocks
import net.minecraft.util.EnumParticleTypes
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GriffinBurrowParticleFinder {

    private val config get() = SkyHanniMod.feature.event.diana

    private val recentlyDugParticleBurrows = TimeLimitedSet<LorenzVec>(1.minutes)
    private val burrows = mutableMapOf<LorenzVec, Burrow>()
    private var lastDugParticleBurrow: LorenzVec? = null

    // This exists to detect the unlucky timing when the user opens a burrow before it gets fully detected
    private var fakeBurrow: LorenzVec? = null

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

        // TODO remove the workaround once we know what is going on exactly and can fix this properly
        val location = workaround(event.location)
        val burrow = burrows.getOrPut(location) { Burrow(location) }
        val oldBurrowType = burrow.type

        when (type) {
            ParticleType.FOOTSTEP -> burrow.hasFootstep = true
            ParticleType.ENCHANT -> burrow.hasEnchant = true
            ParticleType.EMPTY -> burrow.type = 0
            ParticleType.MOB -> burrow.type = 1
            ParticleType.TREASURE -> burrow.type = 2
        }

        burrow.burrowTimeToLive += 1
        if (burrow.burrowTimeToLive > 40) burrow.burrowTimeToLive = 40
        if (burrow.hasEnchant && burrow.hasFootstep && burrow.type != -1) {
            if (!burrow.found || burrow.type != oldBurrowType) {
                BurrowDetectEvent(burrow.location, burrow.getType()).post()
                burrow.found = true
            }
        }
    }

    private fun workaround(location: LorenzVec) = location.toBlockPos().down().toLorenzVec()

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onTick() {
        val isSpade = InventoryUtils.getItemInHand()?.isDianaSpade ?: false
        if (isSpade) {
            burrows.filter { (location, burrow) ->
                burrow.burrowTimeToLive -= 1
                location.distanceSqToPlayer() < 256 && burrow.burrowTimeToLive < 0
            }.forEach { (location, burrow) ->
                BurrowDugEvent(location).post()
                burrows.remove(location)
                lastDugParticleBurrow = null
            }
        }
    }

    // TODO remove the roundTo calls as they are only workarounds
    private enum class ParticleType(val check: ReceiveParticleEvent.() -> Boolean) {
        EMPTY(
            { type == EnumParticleTypes.CRIT_MAGIC && count == 4 && speed == 0.01f && offset.roundTo(2) == LorenzVec(0.5, 0.1, 0.5) },
        ),
        MOB(
            { type == EnumParticleTypes.CRIT && count == 3 && speed == 0.01f && offset.roundTo(2) == LorenzVec(0.5, 0.1, 0.5) },
        ),
        TREASURE(
            { type == EnumParticleTypes.DRIP_LAVA && count == 2 && speed == 0.01f && offset.roundTo(2) == LorenzVec(0.35, 0.1, 0.35) },
        ),
        FOOTSTEP(
            { type == EnumParticleTypes.FOOTSTEP && count == 1 && speed == 0.0f && offset.roundTo(2) == LorenzVec(0.05, 0.0, 0.05) },
        ),
        ENCHANT(
            {
                type == EnumParticleTypes.ENCHANTMENT_TABLE && count == 5 && speed == 0.05f && offset.roundTo(2) == LorenzVec(
                    0.5,
                    0.4,
                    0.5,
                )
            },
        )
    }

    @HandleEvent
    fun onWorldChange() {
        reset()
    }

    fun reset() {
        burrows.clear()
        recentlyDugParticleBurrows.clear()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        if (!config.guess) return
        val message = event.message
        if (message.startsWith("§eYou dug out a Griffin Burrow!") ||
            message == "§eYou finished the Griffin burrow chain! §r§7(4/4)"
        ) {
            BurrowApi.lastBurrowRelatedChatMessage = SimpleTimeMark.now()
            val burrow = lastDugParticleBurrow
            if (burrow != null) {
                if (!tryDig(burrow)) {
                    fakeBurrow = burrow
                }
            }
        }
        if (message == "§cDefeat all the burrow defenders in order to dig it!") {
            BurrowApi.lastBurrowRelatedChatMessage = SimpleTimeMark.now()
        }
    }

    private fun tryDig(location: LorenzVec, ignoreFound: Boolean = false): Boolean {
        val burrow = burrows[location] ?: return false
        if (!burrow.found && !ignoreFound) return false
        burrows.remove(location)
        recentlyDugParticleBurrows.add(location)
        lastDugParticleBurrow = null

        BurrowDugEvent(burrow.location).post()
        return true
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (!config.guess) return

        val location = event.position
        if (event.itemInHand?.isDianaSpade != true || location.getBlockAt() !== Blocks.grass) return

        if (location == fakeBurrow) {
            fakeBurrow = null
            // This exists to detect the unlucky timing when the user opens a burrow before it gets fully detected
            tryDig(location, ignoreFound = true)
            return
        }

        if (burrows.containsKey(location)) {
            lastDugParticleBurrow = location

            DelayedRun.runDelayed(1.seconds) {
                if (BurrowApi.lastBurrowRelatedChatMessage.passedSince() > 2.seconds) {
                    burrows.remove(location)
                }
            }
        }
    }

    class Burrow(
        var location: LorenzVec,
        var hasFootstep: Boolean = false,
        var hasEnchant: Boolean = false,
        var type: Int = -1,
        var found: Boolean = false,
        var burrowTimeToLive: Int = 0,
    ) {

        fun getType(): BurrowType {
            return when (this.type) {
                0 -> BurrowType.START
                1 -> BurrowType.MOB
                2 -> BurrowType.TREASURE
                else -> BurrowType.UNKNOWN
            }
        }
    }

    private fun isEnabled() = DianaApi.isDoingDiana()
}
