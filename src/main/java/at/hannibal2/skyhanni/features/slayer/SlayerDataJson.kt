package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.features.slayer.RemainingSlayerKills.Mob
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class SlayerDataJson(
    @Expose @SerializedName("normal_mobs")
    val normalMobs: Map<SlayerType, Map<String, List<Mob>>>,

    @Expose @SerializedName("mini_bosses")
    val miniBosses: Map<SlayerType, Map<String, List<Mob>>>,

    @Expose
    val weapons: Map<SlayerType, Map<NeuInternalName, Int>>,

    @Expose
    val equipments: Map<SlayerType, Map<NeuInternalName, Int>>,

    @Expose @SerializedName("spawn_costs")
    val spawnCosts: Map<SlayerType, Map<Int, Int>>,

    @Expose @SerializedName("xp_gains")
    val xpGains: Map<SlayerType, Map<Int, Int>>,
)
