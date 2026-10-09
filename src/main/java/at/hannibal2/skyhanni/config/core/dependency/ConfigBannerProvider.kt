package at.hannibal2.skyhanni.config.core.dependency

/**
 * Implemented by option editors that reserve vertical space for banners/panels above the
 * actual option content (e.g. "used by", dependencies, third-party warnings).
 *
 * Lets wrapper editors (like the big-description overlay) know where the real content
 * starts, so they do not trigger on the banner area.
 */
interface ConfigBannerProvider {
    /** Total vertical space reserved above the option content, including nested banners. */
    fun bannerOffset(): Int
}
