package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import net.minecraft.ChatFormatting

data class SeaCreature(
    val name: String,
    val fishingExperience: Int,
    val chatColor: String,
    val rare: Boolean,
    val rarity: LorenzRarity,
    val lootshareSphereOverride: Boolean?,
    val oldNames: List<String> = emptyList(),
) {

    val displayName = chatColor + legacyRare() + name

    val componentDisplayName = componentBuilder {
        append(name).withStyle(componentColor(), rare())
    }

    private fun componentColor() = chatColor[1].toLorenzColor()?.toChatFormatting() ?: ChatFormatting.DARK_RED

    private fun rare() = if (rare) ChatFormatting.BOLD else componentColor()

    private fun legacyRare() = if (rare) "§l" else ""
}

