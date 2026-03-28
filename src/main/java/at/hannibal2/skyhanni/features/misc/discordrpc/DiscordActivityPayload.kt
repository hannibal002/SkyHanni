package at.hannibal2.skyhanni.features.misc.discordrpc

import com.google.gson.annotations.SerializedName

internal data class ActivityTimestamps(
    val start: Long,
)

internal data class ActivityAssets(
    @SerializedName("large_image") val largeImage: String? = null,
    @SerializedName("large_text") val largeText: String? = null,
)

internal data class ActivityButton(
    val label: String,
    val url: String,
)

internal data class Activity(
    val details: String? = null,
    val state: String? = null,
    val timestamps: ActivityTimestamps? = null,
    val assets: ActivityAssets? = null,
    val buttons: List<ActivityButton>? = null,
)

internal data class ActivityArgs(
    val pid: Int,
    val activity: Activity,
)

internal data class ActivityPayload(
    val cmd: String = "SET_ACTIVITY",
    val args: ActivityArgs,
    val nonce: String,
)

internal fun buildActivityPayload(
    presence: DiscordRichPresence,
    pid: Int,
    nonce: String,
) = ActivityPayload(
    args = ActivityArgs(
        pid = pid,
        activity = Activity(
            details = presence.details,
            state = presence.state,
            timestamps = presence.startTimestamp?.let { ActivityTimestamps(it) },
            assets = if (presence.largeImageKey != null || presence.largeImageText != null) {
                ActivityAssets(largeImage = presence.largeImageKey, largeText = presence.largeImageText)
            } else null,
            buttons = presence.buttons.map { ActivityButton(it.label, it.url) }.ifEmpty { null },
        ),
    ),
    nonce = nonce,
)
