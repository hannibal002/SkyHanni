package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.EndBossDeathEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PlayerUtils
import kotlin.time.Duration.Companion.seconds

/**
 * Shared presentation for rare End island drops: a red "DROP!" headline with the item underneath
 * in its rarity colour, plus a chat line carrying its market value. Used by both the dragon and
 * the protector alerts so the two cannot drift apart in wording or styling.
 */
@SkyHanniModule
object RareDropAlert {

    private val TITLE_DURATION = 5.seconds

    private val VOWELS = setOf('A', 'E', 'I', 'O', 'U')

    /**
     * One announcement per item and fight. The same physical drop can be seen twice - once from
     * the stand carrying it and once from the one holding its label - while genuinely different
     * drops must all be reported.
     */
    private val announced = mutableSetOf<NeuInternalName>()

    /** Cleared when the loot window opens, which is the boss death - not the summary after it. */
    @HandleEvent
    private fun onEndBossDeath(event: EndBossDeathEvent) {
        announced.clear()
    }


    /** SkyBlock rarity colours. */
    const val RARE = "§9"
    const val EPIC = "§5"
    const val LEGENDARY = "§6"

    /**
     * Pearlescent Dye, matching the dark cyan the dye itself applies. Animated chroma is not
     * possible here, because titles are plain strings and cannot carry the chroma text style.
     */
    const val DYE = "§3"

    /**
     * @param withTitle whether the drop is rare enough to interrupt the screen. Everything else
     *   is only reported in chat, so the titles stay meaningful.
     */
    data class Drop(val color: String, val label: String, val withTitle: Boolean = true)

    fun show(internalName: NeuInternalName, drop: Drop, amount: Int) {
        if (!announced.add(internalName)) return
        val suffix = if (amount > 1) " §7x$amount" else ""
        val value = valueText(internalName, amount)

        if (drop.withTitle) {
            TitleManager.sendTitle(
                "§c§lDROP!",
                subtitleText = "${drop.color}§l${drop.label}$suffix $value",
                duration = TITLE_DURATION,
            )
        }
        // "dropped" would read as having thrown the item away, which is the opposite of what
        // happened - the boss dropped it, the player obtained it.
        val subject = if (amount > 1) "§f${amount}x " else "§f${article(drop.label)} "
        ChatUtils.chat("§b${PlayerUtils.getName()} §fhas obtained $subject${drop.color}${drop.label}§r $value")
    }

    /** English article for the item name, so single drops read as a sentence. */
    private fun article(label: String) = if (label.firstOrNull()?.uppercaseChar() in VOWELS) "an" else "a"

    /** Market value of the whole stack, or a hint when the item or its price is unknown. */
    private fun valueText(internalName: NeuInternalName, amount: Int): String {
        val price = internalName.getPriceOrNull()?.times(amount)
            ?: return "§8(no price)"
        return "§7(§6+${price.shortFormat()}§7)"
    }
}
