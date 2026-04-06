package at.hannibal2.skyhanni.features.misc.discordrpc

import com.google.gson.annotations.SerializedName

internal data class ActivityTimestamps(
    val start: Long? = null,
    val end: Long? = null,
)

internal data class ActivityAssets(
    @SerializedName("large_image") val largeImage: String? = null,
    @SerializedName("large_text") val largeText: String? = null,
    @SerializedName("large_url") val largeUrl: String? = null,
    @SerializedName("small_image") val smallImage: String? = null,
    @SerializedName("small_text") val smallText: String? = null,
    @SerializedName("small_url") val smallUrl: String? = null,
)

internal data class ActivityButton(
    val label: String,
    val url: String,
)

internal data class Activity(
    val type: Int = 0,
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
            details = presence.details?.ifEmpty { null },
            state = presence.state?.ifEmpty { null },
            timestamps = if (presence.startTimestamp != null || presence.endTimestamp != null) {
                ActivityTimestamps(start = presence.startTimestamp, end = presence.endTimestamp)
            } else null,
            assets = run {
                val hasLarge = presence.largeImageKey != null || presence.largeImageText != null || presence.largeImageUrl != null
                val hasSmall = presence.smallImageKey != null || presence.smallImageText != null || presence.smallImageUrl != null
                if (hasLarge || hasSmall) ActivityAssets(
                    largeImage = presence.largeImageKey,
                    largeText = presence.largeImageText,
                    largeUrl = presence.largeImageUrl,
                    smallImage = presence.smallImageKey,
                    smallText = presence.smallImageText,
                    smallUrl = presence.smallImageUrl,
                ) else null
            },
            buttons = presence.buttons.map { ActivityButton(it.label, it.url) }.ifEmpty { null },
        ),
    ),
    nonce = nonce,
)
