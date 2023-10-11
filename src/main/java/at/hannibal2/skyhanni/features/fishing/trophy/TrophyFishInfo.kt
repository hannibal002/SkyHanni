package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import com.google.gson.annotations.Expose
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle

data class TrophyFishInfo(
    @Expose
    val displayName: String,
    @Expose
    private val description: String,
    @Expose
    private val rate: Int?,
    @Expose
    private val fillet: Map<TrophyRarity, Int>
) {

    // Credit to NotEnoughUpdates (Trophy Fish profile viewer page) for the format.
    fun getTooltip(counts: Map<TrophyRarity, Int>): ChatStyle {
        val bestFishObtained = counts.keys.maxOrNull() ?: TrophyRarity.BRONZE
        val rateString = if (rate != null) "§8[§7$rate%§8]" else ""
        val display = """
            |$displayName $rateString
            |${description.splitLines(150)}
            |
            |${TrophyRarity.DIAMOND.formattedString}: ${formatCount(counts, TrophyRarity.DIAMOND)}
            |${TrophyRarity.GOLD.formattedString}: ${formatCount(counts, TrophyRarity.GOLD)}
            |${TrophyRarity.SILVER.formattedString}: ${formatCount(counts, TrophyRarity.SILVER)}
            |${TrophyRarity.BRONZE.formattedString}: ${formatCount(counts, TrophyRarity.BRONZE)}
            |
            |§7Total: ${bestFishObtained.formatCode}${counts.values.sum().addSeparators()}
        """.trimMargin()
        return ChatStyle().setChatHoverEvent(
            HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(display))
        )
    }

    fun getFilletValue(rarity: TrophyRarity): Int {
        if (fillet == null) {
            CopyErrorCommand.logError(
                Error("fillet is null for '$displayName'"),
                "Error trying to read trophy fish info"
            )
            return -1
        }
        return fillet.getOrDefault(rarity, -1)
    }

    private fun formatCount(counts: Map<TrophyRarity, Int>, rarity: TrophyRarity): String {
        val count = counts.getOrDefault(rarity, 0)
        return if (count > 0) "§6${count.addSeparators()}" else "§c✖"
    }
}
