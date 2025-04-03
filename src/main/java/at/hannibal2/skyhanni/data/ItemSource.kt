package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ItemAddEvent

@Suppress("unused")
enum class ItemSource {
    MOB_DROP,
    LOOTSHARE,
    DUNGEON_SECRET,
    DUNGEON_REWARD,
    SLAYER_BOSS,
    SLAYER_MINIBOSS,
    RNG_METER,
    FISHING,
    BLOCK_BREAK,
    CHEST,
    MINION,
    CRAFTING,
    NPC,
    BAZAAR,
    ANVIL,
    BASIC_REFORGING,
    STONE_REFORGING,
    ENCHANTING,
    EXPERIMENTATION_TABLE,
    AUCTION,
    TRADING,
    COLLECTION_REWARD,
    QUEST_REWARD,
    PET, // I don't know any cases as of now
    DRAGON,
    ARMOR_EFFECT,
    REFUND,
    BANK,
    SACKS,
    STASH,
    ENDER_CHEST,
    ITEM_COMPACT,
    DICE_ROLL,

    COMMAND,
    UNKNOWN;

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
