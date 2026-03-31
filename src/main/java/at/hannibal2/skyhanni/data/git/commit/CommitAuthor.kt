package at.hannibal2.skyhanni.data.git.commit

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CommitAuthor(
    @Expose val login: String,
    @Expose val id: Int,
    @Expose @field:SerializedName("node_id") val nodeId: String,
    @Expose @field:SerializedName("avatar_url") val avatarUrl: String,
    @Expose @field:SerializedName("gravatar_id") val gravatarId: String,
    @Expose val url: String,
    @Expose @field:SerializedName("html_url") val htmlUrl: String,
    @Expose @field:SerializedName("followers_url") val followersUrl: String,
    @Expose @field:SerializedName("following_url") val followingUrl: String,
    @Expose @field:SerializedName("gists_url") val gistsUrl: String,
    @Expose @field:SerializedName("starred_url") val starredUrl: String,
    @Expose @field:SerializedName("subscriptions_url") val subscriptionsUrl: String,
    @Expose @field:SerializedName("organizations_url") val organizationsUrl: String,
    @Expose @field:SerializedName("repos_url") val reposUrl: String,
    @Expose @field:SerializedName("events_url") val eventsUrl: String,
    @Expose @field:SerializedName("received_events_url") val receivedEventsUrl: String,
    @Expose val type: String,
    @Expose @field:SerializedName("user_view_type") val userViewType: String,
    @Expose @field:SerializedName("site_admin") val siteAdmin: Boolean,
)
