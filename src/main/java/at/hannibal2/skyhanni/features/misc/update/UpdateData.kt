package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.utils.system.ModVersion

/**
 * A SkyHanni release offered by an [UpdateSource].
 *
 * @param versionName the name of the release, as shown to the user
 * @param version the version of the release, used to determine whether it is newer than the running one
 * @param downloadPage the page on which the release can be downloaded manually
 */
data class UpdateData(
    val versionName: String,
    val version: ModVersion,
    val downloadPage: String,
)
