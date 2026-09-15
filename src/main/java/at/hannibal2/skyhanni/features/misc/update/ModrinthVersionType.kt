package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.config.features.About.UpdateStream

@Suppress("unused")
enum class ModrinthVersionType(vararg val allowedUpdateStreams: UpdateStream) {
    RELEASE(UpdateStream.RELEASES, UpdateStream.BETA),
    BETA(UpdateStream.BETA),
    ALPHA,
}
