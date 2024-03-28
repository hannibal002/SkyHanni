package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class BannedClassesJson(
    @Expose
    @SerializedName("banned_classes") val bannedClasses: Map<String, List<String>>,
)
