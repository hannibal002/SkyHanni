package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.network.chat.Component

enum class BurrowType(val text: String) {
    START("Start"),
    MOB("Mob"),
    TREASURE("Treasure"),
    UNKNOWN("Unknown?!"),
    ;

    override fun toString() = text

    companion object {
        val config get() = SkyHanniMod.feature.event.diana.burrowCustomization

        fun BurrowType.getBurrowColour(): ChromaColour =
            when (this) {
                START -> config.startBurrowColor
                MOB -> config.mobBurrowColor
                TREASURE -> config.treasureBurrowColor
                else -> LorenzColor.WHITE.toChromaColor()
            }

        fun BurrowType.getBurrowText(): Component = componentBuilder {
            appendWithColor(this@getBurrowText.text, this@getBurrowText.getBurrowColour().toColor().rgb)
        }
    }
}
