package at.hannibal2.skyhanni.features.misc.update

@Suppress("unused")
enum class ModrinthVersionType(vararg val allowedUpdateStreams: String) {
    RELEASE("full", "pre"),
    BETA("pre"),
    ALPHA,
}
