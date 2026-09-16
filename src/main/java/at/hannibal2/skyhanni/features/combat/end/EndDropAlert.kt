package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndLootFoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PlayerUtils
import kotlin.time.Duration.Companion.seconds

/**
 * Shared presentation for rare End island drops: a red "DROP!" headline with the item underneath
 * in its rarity colour, plus a chat line carrying its market value. Used by both the dragon and
 * the protector alerts so the two cannot drift apart in wording or styling.
 *
 * Drops that come from both bosses are announced here as well, because neither alert checks which
 * boss dropped what - listing such an item in both tables would announce it twice.
 */
@SkyHanniModule
object EndDropAlert {

    private val TITLE_DURATION = 5.seconds

    private val VOWELS = setOf('A', 'E', 'I', 'O', 'U')

    /** SkyBlock rarity colours. */
    const val RARE = "§9"
    const val EPIC = "§5"
    const val LEGENDARY = "§6"

    const val DYE = "§3"

    /** Drops of both bosses. The one the fight is credited to decides which toggle applies. */
    private val sharedDrops = mapOf(
        "DYE_PEARLESCENT".toInternalName() to Drop(DYE, "PEARLESCENT DYE"),
    )

    @HandleEvent
    private fun onEndLootFound(event: EndLootFoundEvent) {
        val drop = sharedDrops[event.internalName] ?: return
        val config = SkyHanniMod.feature.combat.endIsland
        val enabled = when (event.boss) {
            EndBoss.DRAGON -> config.dragon.dropAlert
            EndBoss.END_STONE_PROTECTOR -> config.golem.dropAlert
        }
        if (!enabled) return
        show(event.internalName, drop, event.amount)
    }

    /**
     * @param withTitle whether the drop is rare enough to interrupt the screen. Everything else
     *   is only reported in chat, so the titles stay meaningful.
     */
    data class Drop(val color: String, val label: String, val withTitle: Boolean = true)

    fun show(internalName: NeuInternalName, drop: Drop, amount: Int) {
        val suffix = if (amount > 1) " §7x$amount" else ""
        val value = valueText(internalName, amount)

        if (drop.withTitle) {
            TitleManager.sendTitle(
                "§c§lDROP!",
                subtitleText = "${drop.color}§l${drop.label}$suffix $value",
                duration = TITLE_DURATION,
            )
        }

        val subject = if (amount > 1) "§f${amount}x " else "§f${article(drop.label)} "
        ChatUtils.chat("§b${PlayerUtils.getName()} §fhas obtained $subject${drop.color}${drop.label}§r $value")
    }

    private fun article(label: String) = if (label.firstOrNull()?.uppercaseChar() in VOWELS) "an" else "a"

    /** Market value of the whole stack, or a hint when the item or its price is unknown. */
    private fun valueText(internalName: NeuInternalName, amount: Int): String {
        val price = internalName.getPriceOrNull()?.times(amount)
            ?: return "§8(no price)"
        return "§7(§6+${price.shortFormat()}§7)"
    }
}
