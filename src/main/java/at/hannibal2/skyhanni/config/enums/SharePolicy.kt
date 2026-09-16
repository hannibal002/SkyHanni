package at.hannibal2.skyhanni.config.enums

enum class SharePolicy(private val displayName: String) {
    ASK("Ask When Needed"),
    AUTO("Share Automatically"),
    DISABLED("Disabled"),
    ;

    override fun toString() = displayName
}
