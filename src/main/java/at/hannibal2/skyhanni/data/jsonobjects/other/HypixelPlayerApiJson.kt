package at.hannibal2.skyhanni.data.jsonobjects.other

import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class HypixelPlayerApiJson(
    @Expose val profiles: List<HypixelApiProfile>,
)

data class HypixelApiProfile(
    @Expose val members: Map<String, HypixelApiPlayer>,
    @Expose @SerializedName("cute_name") val profileName: String,
)

data class HypixelApiPlayer(
    @Expose @SerializedName("trophy_fish") val trophyFish: HypixelApiTrophyFish,
    @Expose val events: HypixelApiEvents,
    @Expose @SerializedName("nether_island_player_data") val netherData: HypixelApiFactionInfo,
    @Expose val loadout: HypixelApiLoadout?,
    @Expose val inventory: HypixelApiInventory?,
    @Expose @SerializedName("pets_data") val petsData: HypixelApiPetsData?,
    @Expose @SerializedName("accessory_bag_storage") val accessoryBagStorage: HypixelApiAccessoryBagStorage?,
)

data class HypixelApiLoadout(
    // Keyed by set id, with an additional "equipped_set" entry
    @Expose val armor: JsonObject?,
    @Expose val equipment: JsonObject?,
    @Expose val loadouts: Map<String, HypixelApiSavedLoadout>?,
)

data class HypixelApiSavedLoadout(
    @Expose val id: Int?,
    @Expose val name: String?,
    @Expose @SerializedName("armor_set_id") val armorSetId: Int?,
    @Expose @SerializedName("equipment_set_id") val equipmentSetId: Int?,
    @Expose @SerializedName("power_stone") val powerStone: String?,
    @Expose @SerializedName("pet") val petUuid: String?,
    @Expose @SerializedName("tuning_points_slot") val tuningPointsSlot: Int?,
    @Expose @SerializedName("mining_core_selected_slot") val miningCoreSelectedSlot: Int?,
    @Expose @SerializedName("foraging_core_selected_slot") val foragingCoreSelectedSlot: Int?,
)

data class HypixelApiEncodedItems(
    @Expose val type: Int?,
    @Expose val data: String?,
)

data class HypixelApiInventory(
    @Expose @SerializedName("inv_armor") val armor: HypixelApiEncodedItems?,
    @Expose @SerializedName("equipment_contents") val equipment: HypixelApiEncodedItems?,
)

data class HypixelApiPetsData(
    @Expose val pets: List<HypixelApiPet>?,
)

data class HypixelApiPet(
    @Expose val uuid: String?,
    @Expose val uniqueId: String?,
    @Expose val type: String?,
    @Expose val tier: String?,
    @Expose val heldItem: String?,
    @Expose val skin: String?,
    @Expose val exp: Double?,
)

data class HypixelApiAccessoryBagStorage(
    // Keyed by "slot_<n>", each containing stat to tuning points
    @Expose val tuning: JsonObject?,
)

data class HypixelApiEvents(
    @Expose val easter: HypixelApiEasterEvent,
)

data class HypixelApiEasterEvent(
    @Expose val rabbits: HypixelApiRabbits,
)

data class HypixelApiRabbits(
    @Expose @SerializedName("collected_locations") val collectedLocations: Map<String, List<String>>,
)

data class HypixelApiTrophyFish(
    val totalCaught: Int,
    val caught: Map<String, Int>,
)

data class HypixelApiFactionInfo(
    @Expose @SerializedName("barbarians_reputation") val barbarianReputation: Int,
    @Expose @SerializedName("mages_reputation") val mageReputation: Int,
    @Expose @SerializedName("selected_faction") val currentFaction: String?,
)
