package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import net.minecraft.network.chat.Component

data class SeaCreature(
    val name: String,
    val fishingExperience: Int,
    val rgbColor: Int,
    val rare: Boolean,
    val rarity: LorenzRarity,
    val lootshareSphereOverride: Boolean?,
    val oldNames: List<String> = emptyList(),
) {

    val displayName: Component get() = componentBuilder {
        appendWithColor(name, rgbColor) {
            bold = rare
        }
    }
}

