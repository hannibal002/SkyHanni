package at.hannibal2.skyhanni.features.webhooks

import com.google.gson.annotations.SerializedName

data class AllowedMentions(
    val parse: List<String>? = null, // e.g., ["users", "roles", "everyone"]
    @SerializedName("roles") val roleIds: List<String>? = null,
    @SerializedName("users") val userIds: List<String>? = null,
    @SerializedName("replied_user") val repliedUser: Boolean? = null
)
