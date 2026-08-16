package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @param ignoredInternalNames Items that lose the display name to another item.
 * @param ambiguousDisplayNames Display names no item wins, written lowercase and with color codes.
 */
data class ItemDisplayNamesJson(
    @Expose @SerializedName("ignored_internal_names") val ignoredInternalNames: Set<NeuInternalName>,
    @Expose @SerializedName("ambiguous_display_names") val ambiguousDisplayNames: Set<String>,
    @Expose val description: String,
)
