package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.TreeSet

data class HoppityEggLocationsJson(
    @Expose @SerializedName(value = "rabbit_slots", alternate = ["rabbitSlots"]) val rabbitSlots: Map<Int, Int>,
    @Expose @SerializedName(value = "other_upgrade_slots", alternate = ["otherUpgradeSlots"]) val otherUpgradeSlots: Set<Int>,
    @Expose @SerializedName(value = "no_pickblock_slots", alternate = ["noPickblockSlots"]) val noPickblockSlots: Set<Int>,
    @Expose @SerializedName(value = "destructive_slots", alternate = ["destructiveSlots"]) val destructiveSlots: Set<Int>,
    @Expose @SerializedName(value = "barn_index", alternate = ["barnIndex"]) val barnIndex: Int,
    @Expose @SerializedName(value = "info_index", alternate = ["infoIndex"]) val infoIndex: Int,
    @Expose @SerializedName(value = "production_info_index", alternate = ["productionInfoIndex"]) val productionInfoIndex: Int,
    @Expose @SerializedName(value = "prestige_index", alternate = ["prestigeIndex"]) val prestigeIndex: Int,
    @Expose @SerializedName(value = "milestone_index", alternate = ["milestoneIndex"]) val milestoneIndex: Int,
    @Expose @SerializedName(value = "leaderboard_index", alternate = ["leaderboardIndex"]) val leaderboardIndex: Int,
    @Expose @SerializedName(value = "hand_cookie_index", alternate = ["handCookieIndex"]) val handCookieIndex: Int,
    @Expose @SerializedName(value = "time_tower_index", alternate = ["timeTowerIndex"]) val timeTowerIndex: Int,
    @Expose @SerializedName(value = "shrine_index", alternate = ["shrineIndex"]) val shrineIndex: Int,
    @Expose @SerializedName(value = "coach_rabbit_index", alternate = ["coachRabbitIndex"]) val coachRabbitIndex: Int,
    @Expose @SerializedName(value = "rabbit_hitman_index", alternate = ["rabbitHitmanIndex"]) val rabbitHitmanIndex: Int,
    @Expose @SerializedName(value = "max_rabbits", alternate = ["maxRabbits"]) val maxRabbits: Int,
    @Expose @SerializedName(value = "max_prestige", alternate = ["maxPrestige"]) val maxPrestige: Int,
    @Expose @SerializedName("cf_shortcut_index") val cfShortcutIndex: Int,
    @Expose @SerializedName(value = "chocolate_milestones", alternate = ["chocolateMilestones"]) val chocolateMilestones: TreeSet<Long>,
    @Expose @SerializedName("hitman_costs") val hitmanCosts: TreeSet<Long>,
    @Expose
    @SerializedName(value = "chocolate_shop_milestones", alternate = ["chocolateShopMilestones"])
    val chocolateShopMilestones: List<MilestoneJson>,
    @Expose
    @SerializedName(value = "chocolate_factory_milestones", alternate = ["chocolateFactoryMilestones"])
    val chocolateFactoryMilestones: List<MilestoneJson>,
    @Expose
    @SerializedName(value = "resident_locations", alternate = ["residentLocations"])
    val residentLocations: Map<IslandType, List<String>>,
    @Expose
    @SerializedName(value = "api_egg_locations", alternate = ["apiEggLocations"])
    val apiEggLocations: Map<IslandType, Map<String, LorenzVec>>,
    @Expose @SerializedName(value = "special_rabbits", alternate = ["specialRabbits"]) val specialRabbits: List<String>,
)

data class MilestoneJson(
    @Expose val amount: Long,
    @Expose val rabbit: String,
)
