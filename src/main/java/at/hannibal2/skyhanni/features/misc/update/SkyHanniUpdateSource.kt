package at.hannibal2.skyhanni.features.misc.update

import moe.nea.libautoupdate.UpdateSource

@Suppress("unused")
enum class SkyHanniUpdateSource(private val displayName: String, val source: UpdateSource) {
    MODRINTH("Modrinth", ModrinthUpdateSource("byNkmv5G", "skyhanni")),
    GITHUB("GitHub", CustomGithubReleaseUpdateSource("hannibal002", "SkyHanni")),
    ;

    override fun toString() = displayName
}
