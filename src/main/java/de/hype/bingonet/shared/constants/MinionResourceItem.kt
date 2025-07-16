package de.hype.bingonet.shared.constants

interface MinionResourceItem {
    val compactorLevel: Int
        get() = 1

    val displayName: String

    enum class UnusedMinionItems(override val displayName: String) : MinionResourceItem {
        RED_GIFT("Purple Candy"),
        LUSH_BERRIES("Lush Berrbries"),
        PURPLE_CANDY("Purple Candy"),
        ;
    }
}
