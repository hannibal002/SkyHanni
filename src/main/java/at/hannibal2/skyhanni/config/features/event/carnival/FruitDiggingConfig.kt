package at.hannibal2.skyhanni.config.features.event.carnival

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FruitDiggingConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Helper for the Fruit Digging minigame.")
    @FeatureToggle
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Show un-diggable fruit", desc = "Show fruit that's destroyed or already dug.")
    @ConfigEditorBoolean
    var displayFoundFruit: Boolean = false

    @Expose
    @ConfigOption(name = "Found Color", desc = "Color of un-diggable fruit.")
    @ConfigEditorColour
    var foundColor: ChromaColour = LorenzColor.GREEN.toChromaColor()

    @Expose
    @ConfigOption(name = "Show Treasure/Anchor", desc = "Show nearby fruit clues from treasure and anchor dousing modes.")
    @ConfigEditorBoolean
    var displayAdjacentTreasure: Boolean = true

    @Expose
    @ConfigOption(name = "Adjacent Color", desc = "Color of treasure and anchor clues (nearby fruit).")
    @ConfigEditorColour
    var adjacentColor: ChromaColour = LorenzColor.GOLD.toChromaColor()

    @Expose
    @ConfigOption(name = "Show adjacent mine count", desc = "Show number of adjacent mines from mines dousing mode.")
    @ConfigEditorBoolean
    var displayAdjacentMines: Boolean = true

    @Expose
    @ConfigOption(name = "Mines Color", desc = "Color of the mines clue.")
    @ConfigEditorColour
    var minesColor: ChromaColour = LorenzColor.RED.toChromaColor()

    @Expose
    @ConfigOption(name = "Show fruit guesses", desc = "Show guesses for fruits that have not been dug up yet. This includes anchor.")
    @ConfigEditorBoolean
    var displayFruitGuesses: Boolean = true

    @Expose
    @ConfigOption(name = "Fruit Guess Color", desc = "Color of fruit guesses.")
    @ConfigEditorColour
    var fruitGuessColor: ChromaColour = LorenzColor.AQUA.toChromaColor()

    @Expose
    @ConfigOption(
        name = "Show best dig",
        desc = "Highlight the solver's recommended next dig (max expected score) and which shovel to equip.",
    )
    @ConfigEditorBoolean
    var displayBestDig: Boolean = true

    @Expose
    @ConfigOption(name = "Best Dig Color", desc = "Color of the recommended next dig highlight.")
    @ConfigEditorColour
    var bestDigColor: ChromaColour = LorenzColor.GREEN.toChromaColor()

    @Expose
    @ConfigLink(owner = FruitDiggingConfig::class, field = "displayBestDig")
    val bestDigPosition: Position = Position(200, 120)

    @Expose
    @ConfigOption(name = "Remaining Fruit Display", desc = "Show remaining Fruit Digging components.")
    @ConfigEditorBoolean
    var remainingFruitDisplay: Boolean = true

    @Expose
    @ConfigLink(owner = FruitDiggingConfig::class, field = "remainingFruitDisplay")
    val remainingFruitPosition: Position = Position(200, 20)
}
