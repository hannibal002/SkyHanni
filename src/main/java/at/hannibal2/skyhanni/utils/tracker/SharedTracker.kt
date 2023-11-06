package at.hannibal2.skyhanni.utils.tracker

class SharedTracker<T : TrackerData>(
    private val total: T,
    private val currentSession: T,
) {
    fun modify(modifyFunction: (T) -> Unit) {
        modifyFunction(total)
        modifyFunction(currentSession)
    }

    fun get(displayMode: DisplayMode) = when (displayMode) {
        DisplayMode.TOTAL -> total
        DisplayMode.CURRENT -> currentSession
    }

    fun getCurrent() = get(TrackerUtils.currentDisplayMode)
}
