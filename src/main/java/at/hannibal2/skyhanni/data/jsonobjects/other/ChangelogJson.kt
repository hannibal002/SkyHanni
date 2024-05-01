package at.hannibal2.skyhanni.data.jsonobjects.other

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ChangelogJson(
    @Expose
    val body: String,
    @Expose
    @SerializedName("tag_name")
    val tagName: String,
)
