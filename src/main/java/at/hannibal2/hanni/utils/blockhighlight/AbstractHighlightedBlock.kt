package at.hannibal2.hanni.utils.blockhighlight

import at.hannibal2.hanni.utils.LorenzVec

/**
 * An abstract class that represents a location to be highlighted by a [HanniBlockHighlighter]
 */
abstract class AbstractHighlightedBlock(loc: LorenzVec) {

    val location = loc.roundToBlock()

    /**
     * Additional criteria that the highlighted block must fulfill
     */
    abstract fun extraCondition(): Boolean

}
