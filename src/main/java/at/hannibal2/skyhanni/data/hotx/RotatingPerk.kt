package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.utils.RegexUtils.matches
import java.util.regex.Pattern

interface RotatingPerk {
    val displayDescription: String
    val chatPattern: Pattern
    val itemPattern: Pattern
}

/**
 * One rotating perk of a tree, e.g. Sky Mall, Lottery or Beekeeper.
 * A tree can have more than one of them.
 */
class RotatingPerkSlot<Data : HotxData<*>, Perk : RotatingPerk>(
    val entry: Data,
    val perks: List<Perk>,
) {
    var currentPerk: Perk? = null
        private set

    init {
        perks.forEach {
            it.chatPattern
            it.itemPattern
        }
    }

    fun trySetFromChat(perkText: String): Boolean {
        currentPerk = perks.firstOrNull { it.chatPattern.matches(perkText) } ?: return false
        return true
    }

    /**
     * Unlike [trySetFromChat] this also clears the perk when nothing matches,
     * because the item always belongs to this slot while a chat message may belong to another one.
     */
    fun setFromItem(perkText: String): Boolean {
        currentPerk = perks.firstOrNull { it.itemPattern.matches(perkText) }
        return currentPerk != null
    }

    fun clear() {
        currentPerk = null
    }
}
