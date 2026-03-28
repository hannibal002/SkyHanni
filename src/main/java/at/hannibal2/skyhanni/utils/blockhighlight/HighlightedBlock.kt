package at.hannibal2.skyhanni.utils.blockhighlight

import net.minecraft.world.phys.Vec3

/**
 * A default highlighted block that only stores its location.
 *
 * Useful for blocks like Lushlilac in Galatea or Dark Monolith eggs in the Dwarven Mines because
 * after they are broken these blocks have their BlockState change.
 */
class HighlightedBlock(loc: Vec3) : AbstractHighlightedBlock(loc) {

    override fun extraCondition() = true
}
