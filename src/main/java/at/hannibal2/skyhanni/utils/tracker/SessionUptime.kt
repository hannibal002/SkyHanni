package at.hannibal2.skyhanni.utils.tracker

sealed class SessionUptime {
    data class Normal(val sessionType: NormalSession) : SessionUptime() {
        override fun toString(): String = sessionType.toString()
    }
    data class Garden(val sessionType: GardenSession) : SessionUptime() {
        override fun toString(): String = sessionType.toString()
    }
}

enum class NormalSession(val displayName: String) {
    NORMAL("Normal"),
    ;
    override fun toString() = displayName
}

enum class GardenSession(val displayName: String) {
    PEST("Pest"),
    VISITOR("Visitor"),
    CROP("Crop"),
    ;
    override fun toString() = displayName
}
