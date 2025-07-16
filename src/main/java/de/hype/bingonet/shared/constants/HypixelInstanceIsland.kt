package de.hype.bingonet.shared.constants

enum class HypixelInstanceIsland(
    val typeName: String, val level: Int, val requirement: Int,
    val instanceType: InstanceIslandType
) {
    CATACOMBS_ENTRANCE("Entrance", 0, 0, InstanceIslandType.DEFAULT_CATACOMBS),
    CATACOMBS_FLOOR_ONE("Floor 1", 1, 1, InstanceIslandType.DEFAULT_CATACOMBS),
    CATACOMBS_FLOOR_TWO("Floor 2", 2, 3, InstanceIslandType.DEFAULT_CATACOMBS),
    CATACOMBS_FLOOR_THREE("Floor 3", 3, 5, InstanceIslandType.DEFAULT_CATACOMBS),
    CATACOMBS_FLOOR_FOUR("Floor 4", 4, 9, InstanceIslandType.DEFAULT_CATACOMBS),
    CATACOMBS_FLOOR_FIVE("Floor 5", 5, 14, InstanceIslandType.DEFAULT_CATACOMBS),
    CATACOMBS_FLOOR_SIX("Floor 6", 6, 19, InstanceIslandType.DEFAULT_CATACOMBS),
    CATACOMBS_FLOOR_SEVEN("Floor 7", 7, 24, InstanceIslandType.DEFAULT_CATACOMBS),
    MASTER_CATACOMBS_FLOOR_ONE("Mastermode Floor 1", 0, 24, InstanceIslandType.MASTER_CATACOMBS),
    MASTER_CATACOMBS_FLOOR_TWO("Mastermode Floor 2", 1, 26, InstanceIslandType.MASTER_CATACOMBS),
    MASTER_CATACOMBS_FLOOR_THREE("Mastermode Floor 3", 2, 28, InstanceIslandType.MASTER_CATACOMBS),
    MASTER_CATACOMBS_FLOOR_FOUR("Mastermode Floor 4", 3, 30, InstanceIslandType.MASTER_CATACOMBS),
    MASTER_CATACOMBS_FLOOR_FIVE("Mastermode Floor 5", 4, 32, InstanceIslandType.MASTER_CATACOMBS),
    MASTER_CATACOMBS_FLOOR_SIX("Mastermode Floor 6", 5, 34, InstanceIslandType.MASTER_CATACOMBS),
    MASTER_CATACOMBS_FLOOR_SEVEN("Mastermode Floor 7", 6, 36, InstanceIslandType.MASTER_CATACOMBS),
    KUUDRA_NORMAL("Kuudra", 0, 0, InstanceIslandType.KUUDRA),
    KUUDRA_HOT("Kuudra Hot Tier", 1, 1000, InstanceIslandType.KUUDRA),
    KUUDRA_BURNING("Kuudra Burning Tier", 2, 3000, InstanceIslandType.KUUDRA),
    KUUDRA_FIERY("Kuudra Fiery Tier", 3, 7000, InstanceIslandType.KUUDRA),
    KUUDRA_INFERNAL("Kuudra Infernal Tier", 4, 12000, InstanceIslandType.KUUDRA),
    ;

    enum class InstanceIslandType {
        DEFAULT_CATACOMBS,
        MASTER_CATACOMBS,
        KUUDRA,
        ;

        fun isCatacombs(): Boolean = this == DEFAULT_CATACOMBS || this == MASTER_CATACOMBS
    }
}
