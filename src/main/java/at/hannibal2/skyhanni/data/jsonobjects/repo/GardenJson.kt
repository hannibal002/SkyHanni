package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class GardenJson(
    @Expose @SerializedName("garden_exp") val gardenExp: List<Int>,
    @Expose @SerializedName("crop_milestones") val cropMilestones: Map<CropType, List<Int>>,
    @Expose @SerializedName("crop_milestone_community_help") val cropMilestoneCommunityHelp: Map<String, Boolean>,
    @Expose val visitors: Map<String, GardenVisitor>,
    @Expose @SerializedName("organic_matter") val organicMatter: Map<NEUInternalName, Double>,
    @Expose val fuel: Map<NEUInternalName, Double>
)

data class GardenVisitor(
    @Expose val rarity: LorenzRarity,
    @Expose @SerializedName("new_rarity") val newRarity: LorenzRarity?,
    @Expose val position: LorenzVec?,
    @Expose var skinOrType: String?,
    @Expose val mode: String,
    @Expose @SerializedName("need_items") val needItems: List<String>
)
