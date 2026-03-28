package at.hannibal2.skyhanni.utils.blockhighlight

import at.hannibal2.skyhanni.utils.VectorUtils.roundToBlock
import net.minecraft.world.phys.Vec3

/**
 * An abstract class that represents a location to be highlighted by a [SkyHanniBlockHighlighter]
 */
abstract class AbstractHighlightedBlock(loc: Vec3) {

    val location = loc.roundToBlock()

    /**
     * Additional criteria that the highlighted block must fulfill
     */
    abstract fun extraCondition(): Boolean
}
