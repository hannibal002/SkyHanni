package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.ChatUtils

/**
 * Implementation of `Resettable` for all objects with an underlying SkyHanniTracker.
 */
abstract class SimpleTracker: Resettable {
    abstract val tracker: SkyHanniTracker<*>
    override val name
        get() = tracker.name

    override fun resetCommand() = tracker.resetCommand()
}
