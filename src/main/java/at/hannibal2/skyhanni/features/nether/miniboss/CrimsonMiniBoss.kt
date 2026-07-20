package at.hannibal2.skyhanni.features.nether.miniboss

import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.world.phys.AABB

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
    MAGMA_BOSS(
        "Magma Boss",
        LorenzVec(-318, 59, -751).axisAlignedTo(LorenzVec(-442, 90, -851)),
    ),
    ;

    override fun toString() = displayName

    companion object {
        fun fromName(spawnName: String) = entries.firstOrNull {
            it.displayName.equals(spawnName, true)
        }
    }
}
