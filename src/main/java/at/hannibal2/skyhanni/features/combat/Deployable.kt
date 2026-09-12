package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.LivingEntity

enum class Deployable(
    deployableName: String,
    val displayName: String,
    val range: Int,
    val type: DeployableType,
    val tier: Int = 0,
    val fullShaft: Boolean = false,
    var entity: LivingEntity? = null,
    var expiryTime: SimpleTimeMark = SimpleTimeMark.farPast(),
) {
    RADIANT("Radiant", "§aRadiant", 18, DeployableType.FLUX, 1),
    MANA_FLUX("Mana Flux", "§9Mana Flux", 18, DeployableType.FLUX, 2),
    OVERFLUX("Overflux", "§5Overflux", 18, DeployableType.FLUX, 3),
    PLASMAFLUX("Plasmaflux", "§d§lPlasmaflux", 20, DeployableType.FLUX, 4),
    DWARVEN_LANTERN("Dwarven Lantern", "§fDwarven Lantern", 30, DeployableType.LANTERN, 1),
    MITHRIL_LANTERN("Mithril Lantern", "§aMithril Lantern", 30, DeployableType.LANTERN, 2),
    TITANIUM_LANTERN("Titanium Lantern", "§9Titanium Lantern", 30, DeployableType.LANTERN, 3),
    GLACITE_LANTERN("Glacite Lantern", "§5Glacite Lantern", 30, DeployableType.LANTERN, 4, true),
    WILL_O_WISP("Will-o'-wisp", "§6§lWill-o'-wisp", 30, DeployableType.LANTERN, 5, true),
    BLACK_HOLE("Black Hole", "§5Black Hole", 20, DeployableType.BLACK_HOLE),
    UMBERELLA("Umberella", "§9Umberella", 30, DeployableType.UMBERELLA),
    ;

    /**
     * REGEX-TEST: Umberella 298s
     */
    val pattern by RepoPattern.pattern(
        "combat.deployable.${name.lowercase().replace("_", "-")}",
        "$deployableName (?<time>\\d+)s",
    )

    fun isInRange(entity: LivingEntity): Boolean {
        return hasShaftBuff() || range > entity.getLorenzVec().distanceToPlayer()
    }

    fun isInRange(): Boolean {
        if (hasShaftBuff()) return true
        val entity = entity ?: return false
        return range > entity.getLorenzVec().distanceToPlayer()
    }

    fun hasShaftBuff(): Boolean {
        return fullShaft && IslandType.MINESHAFT.isInIsland()
    }

    fun isActive(): Boolean {
        // A mineshaft is bigger than entity render distance
        return !expiryTime.isInPast() && isInRange() && (entity?.isRemoved == false || hasShaftBuff())
    }

    fun reset() {
        entity = null
        expiryTime = SimpleTimeMark.farPast()
    }

    override fun toString(): String {
        return displayName
    }
}

enum class DeployableType {
    FLUX,
    LANTERN,
    UMBERELLA,
    BLACK_HOLE,
    ;

    override fun toString(): String {
        return when {
            this == FLUX -> "Power Orb §7(§d§lPlasmaflux§7)"
            this == LANTERN -> "Lantern §7(§5Glacite Lantern§7)"
            this == UMBERELLA -> "§9Umberella"
            this == BLACK_HOLE -> "§5Black Hole"
            else -> "error sob emoji"
        }
    }
}
