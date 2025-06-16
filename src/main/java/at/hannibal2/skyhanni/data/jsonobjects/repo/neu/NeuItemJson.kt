package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuItemJson(
    @Expose @SerializedName("itemid") val itemId: String,
    @Expose @SerializedName("displayname") val displayName: String,
    @Expose @SerializedName("nbttag") val nbtTagString: String,
    @Expose val damage: Int,
    @Expose val lore: List<String>,
    @Expose @SerializedName("internalname") val internalName: NeuInternalName,
    @Expose @SerializedName("crafttext") val craftText: String,
    @Expose @SerializedName("clickcommand") val clickCommand: String,
    @Expose @SerializedName("modver") val modVersion: String,
    @Expose val infoType: String,
    @Expose val info: List<String>
)
