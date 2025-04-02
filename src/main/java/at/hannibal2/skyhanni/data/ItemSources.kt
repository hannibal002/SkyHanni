package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ItemAddEvent

object ItemSources {
    enum class Source {
        ITEM_ADD,
        SACKS,
        COMMAND,
    }

    enum class ExactSource {
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
        REWARD,
        REFUND,
        BANK,
        SACKS,
        STASH,
        ENDER_CHEST,
        ITEM_COPACT,
        DICE_ROLL,

        OTHER,
        NONE,
    }

    fun ItemAddEvent.isTrackerRelevant() = { event: ItemAddEvent ->
        event.source == Source.ITEM_ADD && event.exactSource != ExactSource.NONE
    }
}
