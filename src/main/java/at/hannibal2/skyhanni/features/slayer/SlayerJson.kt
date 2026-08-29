package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.features.slayer.RemainingSlayerKills.Mob
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class SlayerJson(

    @Expose @SerializedName("normal_mobs")
    val normalMobs: Map<SlayerType, Map<String, List<Mob>>>,

    @Expose @SerializedName("mini_bosses")
    val miniBosses: Map<SlayerType, Map<String, List<Mob>>>,

    @Expose
    val weapons: Map<SlayerType, Map<NeuInternalName, Int>>,

    @Expose
    val equipments: Map<SlayerType, Map<NeuInternalName, Int>>,

    @Expose
    val pets: Map<SlayerType, Map<String, SlayerSpecificPetData>>,

    @Expose @SerializedName("spawn_costs")
    val spawnCosts: Map<SlayerType, Map<Int, Int>>,

    @Expose @SerializedName("xp_gains")
    val xpGains: Map<SlayerType, Map<Int, Int>>,

    @Expose @SerializedName("drop_amounts")
    val dropAmounts: Map<NeuInternalName, Map<Int, String>>,

    @Expose
    val champion: List<Double>,

    @Expose @SerializedName("habanero_wisdom_per_level") val habaneroMultiplier: Double,

    @Expose @SerializedName("multiplicative_mayor_perks") val multiplicativeMayors: Map<String, Double>,

    @Expose @SerializedName("arbitrary_multiplier") val arbitraryMultiplier: Double,

    @Expose @SerializedName("aatrox_slayer_xp_buff_multiplier") val aatroxSlayerXPBuffMultiplier: Double,
)

data class SlayerSpecificPetData(
    // These are only the first halves of a pet's Internal Name, this is the name used within PetData/PetUtils for these.
    @Expose @SerializedName("proper_pet_names") val properPetNames: List<String>? = null,
    @Expose @SerializedName("scaling") val perLevelMultiplier: List<Float>,
)
