package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.utils.VersionConstants
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonPrimitive
import moe.nea.libautoupdate.GithubReleaseUpdateData
import moe.nea.libautoupdate.GithubReleaseUpdateSource
import moe.nea.libautoupdate.UpdateData

/**
 * This class is a custom implementation of the [GithubReleaseUpdateSource] that filters assets based on the mod's version.
 */
class CustomGithubReleaseUpdateSource(owner: String, repository: String) : GithubReleaseUpdateSource(owner, repository) {

    override fun findLatestRelease(validReleases: Iterable<GithubRelease>): UpdateData {
        return validReleases.asSequence()
            .sortedByDescending { ModVersion.fromString(it.tagName) }
            .firstNotNullOfOrNull { findAsset(it) }
            ?: throw IllegalStateException("No valid release found")
    }

    public override fun getReleaseApiUrl(): String = super.getReleaseApiUrl()

    override fun findAsset(release: GithubRelease?): UpdateData? {
        release ?: return null

        return release.assets.stream()
            .filter { it.filterAsset() }
            .map { createReleaseData(release) }
            .findFirst().orElse(null)
    }

    private fun GithubRelease.Download.filterAsset(): Boolean {
        name ?: return false
        browserDownloadUrl ?: return false
        if (!name.endsWith(".jar")) return false
        return name.contains(VersionConstants.MC_VERSION) || name.contains(PlatformUtils.MC_VERSION)
    }

    private fun createReleaseData(release: GithubRelease): GithubReleaseUpdateData {
        return GithubReleaseUpdateData(
            release.name ?: release.tagName,
            JsonPrimitive(release.tagName),
            null,
            null,
            release.body,
            release.targetCommitish,
            release.created_at,
            release.publishedAt,
            release.htmlUrl,
        )
    }
}
