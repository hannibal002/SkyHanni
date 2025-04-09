package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ItemAddEvent

@Suppress("unused")
enum class ItemSource(val description: String?) {
    MOB_DROP(null),
    LOOTSHARE(null),
    DUNGEON_SECRET(null),
    DUNGEON_REWARD(null),
    SLAYER_BOSS(null),
    SLAYER_MINIBOSS(null),
    RNG_METER(null),
    FISHING(null),
    BLOCK_BREAK(null),
    CHEST(null),
    MINION(null),
    CRAFTING(null),
    NPC(null),
    BAZAAR(null),
    ANVIL(null),
    BASIC_REFORGING(null),
    STONE_REFORGING(null),
    ENCHANTING(null),
    EXPERIMENTATION_TABLE(null),
    AUCTION(null),
    TRADING(null),
    COLLECTION_REWARD(null),
    QUEST_REWARD(null),
    PET(null), // I don't know any cases as of now
    DRAGON(null),
    ARMOR_EFFECT(null),
    REFUND(null),
    BANK(null),
    SACKS(null),
    STASH(null),
    ENDER_CHEST(null),
    ITEM_COMPACT(null),
    DICE_ROLL(null),

    COMMAND("Item added via command"),
    UNKNOWN("Item source is unknown");

    override fun toString(): String {
        return this.name.lowercase()
    }

    companion object {
        fun ItemAddEvent.isRelevant(): Boolean = source !in setOf(
            COMMAND,
            CRAFTING,
            CHEST,
            ENDER_CHEST,
            BAZAAR,
            BANK,
            BASIC_REFORGING,
            STONE_REFORGING,
            ENCHANTING,
            TRADING,
            AUCTION,
            ITEM_COMPACT,
        )

        fun ItemAddEvent.isTrackerRelevant() = this.isRelevant() // TODO
    }
}
