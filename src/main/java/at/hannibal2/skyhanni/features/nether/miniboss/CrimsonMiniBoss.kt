package at.hannibal2.skyhanni.features.nether.miniboss

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.ServerTimeMark
import net.minecraft.world.phys.AABB
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class CrimsonMiniBoss(
    val displayName: String,
    val area: AABB,
) {
    BLADESOUL(
        "Bladesoul",
        LorenzVec(-330, 80, -486).axisAlignedTo(LorenzVec(-257, 107, -545)),
    ),
    MAGE_OUTLAW(
        "Mage Outlaw",
        LorenzVec(-200, 98, -843).axisAlignedTo(LorenzVec(-162, 116, -878)),
    ),
    BARBARIAN_DUKE_X(
        "Barbarian Duke X",
        LorenzVec(-550, 101, -890).axisAlignedTo(LorenzVec(-522, 131, -918)),
    ),
    ASHFANG(
        "Ashfang",
        LorenzVec(-462, 155, -1035).axisAlignedTo(LorenzVec(-507, 131, -955)),
    ),
    MAGMA_CUBE(
        "Magma Boss",
        LorenzVec(-318, 59, -751).axisAlignedTo(LorenzVec(-442, 90, -851)),
    ),
    ;

    var doneToday: Boolean = false

    private var intLocation: LorenzVec? = null
    private var intDisplayItem: NeuInternalName = "${name}_MINIBOSS".toInternalName()

    val location: LorenzVec? get() = intLocation
    val displayItem: NeuInternalName get() = intDisplayItem

    private fun setLocation(location: LorenzVec?) {
        this.intLocation = location
    }

    private fun setDisplayItem(displayItem: NeuInternalName) {
        this.intDisplayItem = displayItem
    }

    var nextSpawnTime: ServerTimeMark? = null
    var possibleSpawnTime: Pair<ServerTimeMark, ServerTimeMark>? = null
    var foundBeacon: Boolean? = null
    var spawned: Boolean? = null
    var lastSeenArea: ServerTimeMark = ServerTimeMark.farPast()

    fun isTimerKnown(): Boolean {
        val timer = nextSpawnTime ?: return false
        return timer.passedSince() < 2.minutes + 5.seconds
    }

    fun isSpawningSoon(): Boolean {
        if (spawned == true) return false
        val timer = nextSpawnTime ?: return false
        return timer.passedSince() in 0.seconds..10.seconds
    }

    fun isSpawned(): Boolean {
        if (spawned == true) return true
        val timer = nextSpawnTime ?: return false
        return (timer.passedSince() - 2.minutes) in 0.seconds..20.seconds
    }

    companion object {
        fun getByDisplayName(displayName: String): CrimsonMiniBoss? =
            entries.firstOrNull { it.displayName.equals(displayName, ignoreCase = true) }

        fun addRepoData(displayName: String, displayItem: NeuInternalName, location: LorenzVec?) {
            val target = entries.firstOrNull { it.displayName == displayName } ?: return
            target.setLocation(location)
            target.setDisplayItem(displayItem)
        }
    }
}
