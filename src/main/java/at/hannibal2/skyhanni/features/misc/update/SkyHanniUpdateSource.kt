package at.hannibal2.skyhanni.features.misc.update

@Suppress("unused")
enum class SkyHanniUpdateSource(private val displayName: String, val source: UpdateSource) {
    MODRINTH("Modrinth", ModrinthUpdateSource("byNkmv5G", "skyhanni")),
    GITHUB("GitHub", GitHubReleaseUpdateSource("hannibal002", "SkyHanni")),
    ;

    override fun toString() = displayName
}
