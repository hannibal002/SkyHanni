package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.minecraft.network.chat.Component

data class ContributorsJson(
    @Expose val contributors: Map<String, ContributorJsonEntry>,
)

data class ContributorJsonEntry(
    @Expose val suffix: String = "§c:O",
    @Expose val componentSuffix: Component? = null,
    @Expose val spinny: Boolean = false,
    @Expose val upsideDown: Boolean = false,
    @Expose @SerializedName("display_name") val displayName: String? = null,
)
