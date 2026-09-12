package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.config.features.About.UpdateStream

/**
 * A place from which SkyHanni releases can be discovered.
 */
interface UpdateSource {
    /**
     * @return the newest release in [updateStream], or null if that stream has no release for the running versions
     * @throws IllegalStateException if the releases could not be fetched
     */
    suspend fun checkUpdate(updateStream: UpdateStream): UpdateData?
}
