package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ItemAddEvent

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
    PET,
    DRAGON,
    ARMOR_EFFECT,
    REFUND,
    BANK,
    SACKS,
    STASH,
    ENDER_CHEST,
    ITEM_COPACT,
    DICE_ROLL,

    COMMAND, // TODO: remove this into its own event
    UNKNOWN;

    companion object {
        fun ItemAddEvent.isTrackerRelevant() = source in setOf(ItemSource.FISHING, ItemSource.SACKS) // TODO
    }
}
