package at.hannibal2.skyhanni.utils.tracker

sealed class SessionUptime {
    data class Normal(val sessionType: NormalSession) : SessionUptime()
    data class Garden(val sessionType: GardenSession) : SessionUptime()
}

enum class NormalSession {
    NORMAL,
}

enum class GardenSession {
    PEST,
    VISITOR,
    CROP,
}
