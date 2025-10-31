package at.hannibal2.hanni.features.event.diana

import at.hannibal2.hanni.utils.LorenzColor
import io.github.notenoughupdates.moulconfig.ChromaColour

enum class BurrowType(val text: String, val color: ChromaColour) {
    START("§aStart", LorenzColor.GREEN.toChromaColor()),
    MOB("§cMob", LorenzColor.RED.toChromaColor()),
    TREASURE("§6Treasure", LorenzColor.GOLD.toChromaColor()),
    UNKNOWN("§fUnknown?!", LorenzColor.WHITE.toChromaColor()),
}
