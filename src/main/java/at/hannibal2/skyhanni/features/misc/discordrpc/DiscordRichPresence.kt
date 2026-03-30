package at.hannibal2.skyhanni.features.misc.discordrpc

/**
 * Represents a Discord Rich Presence activity displayed on a user's Discord profile.
 *
 * All fields are optional; null values are omitted from the payload sent to Discord.
 *
 * @param details The primary description line shown beneath the application name.
 * @param state The secondary description line shown beneath [details].
 * @param startTimestamp Unix epoch seconds marking the start of the elapsed timer, or null for no timer.
 * @param largeImageKey The asset key for the large image shown on the presence card.
 * @param largeImageText Tooltip text shown when hovering over the large image.
 * @param buttons Clickable buttons rendered on the presence card. Discord enforces a maximum of two.
 */
data class DiscordRichPresence(
    val details: String? = null,
    val state: String? = null,
    val startTimestamp: Long? = null,
    val largeImageKey: String? = null,
    val largeImageText: String? = null,
    val buttons: List<Button> = emptyList(),
) {
    /**
     * A clickable button rendered on the presence card.
     *
     * @param label Visible button text. Discord enforces a maximum of 32 characters.
     * @param url The URL opened when the button is clicked.
     */
    data class Button(val label: String, val url: String)
}
