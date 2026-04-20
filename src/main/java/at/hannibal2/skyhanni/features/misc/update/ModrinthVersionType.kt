package at.hannibal2.skyhanni.features.misc.update

@Suppress("unused")
enum class ModrinthVersionType(val updateStream: String? = null) {
    RELEASE("full"),
    BETA("pre"),
    ALPHA,
}
