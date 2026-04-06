package at.hannibal2.skyhanni.utils.tracker

/**
 * The display mode a tracker shows on first render.
 *
 * Used as the type of [at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerSettings.defaultDisplayMode].
 * [REMEMBER_LAST] defers to the most recently active mode stored per tracker name.
 */
enum class DefaultDisplayMode(val display: String, val mode: DisplayMode?) {
    TOTAL("Total", DisplayMode.TOTAL),
    SESSION("This Session", DisplayMode.SESSION),
    REMEMBER_LAST("Remember Last", null),
    ;

    override fun toString() = display
}
