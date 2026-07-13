package at.hannibal2.skyhanni.events.experiments

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/**
 * Fired when an Ultra Rare reward is uncovered in the Superpairs Experimentation Table game.
 *
 * @param dropName The display name of the uncovered item.
 * @param isBook Whether the drop is an enchanted book. False for non-book Ultra Rare items,
 *               which are detected via the post-experiment summary rather than the card flip lore.
 */
class TableRareUncoverEvent(val dropName: String, val isBook: Boolean = true) : SkyHanniEvent()
