package at.hannibal2.skyhanni.utils.tracker

sealed class SessionUptime {
    data class Normal(val sessionType: NormalSession) : SessionUptime() {
        override fun toString(): String = sessionType.toString()
    }
    data class Garden(val sessionType: GardenSession) : SessionUptime() {
        override fun toString(): String = sessionType.toString()
    }

    val garden get() = when (this) {
        is Garden -> sessionType
        else -> null
    }
}

enum class NormalSession(val displayName: String) {
    NORMAL("All"),
    ;
    override fun toString() = displayName
}

enum class GardenSession(val displayName: String) {
    PEST("Pest Killing"),
    VISITOR("Accepting Visitors"),
    CROP("Breaking Crops"),
    UNKNOWN("Unknown")
    ;

    override fun toString() = displayName
}
