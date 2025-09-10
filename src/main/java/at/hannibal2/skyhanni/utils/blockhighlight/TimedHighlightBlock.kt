package at.hannibal2.skyhanni.utils.blockhighlight

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A highlighted block that will un-highlight itself after a certain amount of time has passed without it being updated.
 *
 * When a block in the same spot as an already highlighted [TimedHighlightBlock] is highlighted the [update] method will be called
 * refreshing the [lastUpdate] property.
 */
class TimedHighlightBlock(loc: LorenzVec, private val expirationDuration: Duration = 0.seconds) : AbstractHighlightedBlock(loc) {
    private var lastUpdate: SimpleTimeMark = SimpleTimeMark.now()

    fun update() {
        lastUpdate = SimpleTimeMark.now()
    }

    override fun extraCondition(): Boolean {
        return lastUpdate.passedSince() < expirationDuration
    }

}
