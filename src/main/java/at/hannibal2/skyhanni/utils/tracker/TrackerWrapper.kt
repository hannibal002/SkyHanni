package at.hannibal2.skyhanni.utils.tracker

class TrackerWrapper<T : TrackerData>(
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
}
