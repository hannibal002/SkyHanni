package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.ChunkedStatsManager.Companion.getChunkedStats
import at.hannibal2.skyhanni.features.gui.customscoreboard.ChunkedStatsManager.Companion.shouldShowChunkedStats
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.chunkedConfig

object ChunkedStats : Element() {
    override fun getDisplayPair() = getChunkedStats().chunked(chunkedConfig.maxStatsPerLine)
        .map { it.joinToString(" §f| ") }

    override fun showWhen() = shouldShowChunkedStats()

    override val configLine = "§652,763,737 §7| §d64,647 §7| §6249M §7| §b59,264 §7| §c23,495 §7| §a57,873 §7| §c♨ 0 §7| §b0❄ §7| §d756"
}
