package at.hannibal2.skyhanni.utils.blockhighlight

import at.hannibal2.skyhanni.utils.LorenzVec

/**
 * A default highlighted block that only stores its location
 *
 * Useful for blocks like lushlilac in the Galatea or Dark Monolith eggs in the dwarven mines because after they are broken these blocks
 * have their blockstate change.
 */
class HighlightedBlock(loc: LorenzVec) : AbstractHighlightedBlock(loc) {

    override fun extraCondition(): Boolean {
        return true
    }

}
