package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionStats
import com.google.gson.annotations.Expose

data class HoppityCollectionJson(
    @Expose val rabbits: Map<HoppityCollectionStats.RabbitCollectionRarity, List<String>>,
    @Expose val milestoneRabbits: Map<String, Long>,
    @Expose val bonuses: RabbitBonuses
)

data class RabbitBonuses(
    @Expose val generic: Map<HoppityCollectionStats.RabbitCollectionRarity, RabbitBonusEntry>,
    @Expose val special: Map<String, RabbitBonusEntry>
)

data class RabbitBonusEntry(
    @Expose val chocolatePerSecond: Int = 0,
    @Expose val chocolateMultiplier: Double = 0.0
)
