package at.hannibal2.skyhanni.config.features.event.carnival

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FruitDiggingConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Helper for the Fruit Digging minigame.")
    @FeatureToggle
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Show found fruit", desc = "Show uncovered fruit.")
    @ConfigEditorBoolean
    var displayFoundFruit: Boolean = true

    @Expose
    @ConfigOption(name = "Found Color", desc = "Color of the fruit you just dug up.")
    @ConfigEditorColour
    var foundColor: ChromaColour = LorenzColor.GREEN.toChromaColor()

    @Expose
    @ConfigOption(name = "Show treasure", desc = "Show adjacent highest fruit from treasure dousing mode.")
    @ConfigEditorBoolean
    var displayAdjacentTreasure: Boolean = true

    @Expose
    @ConfigOption(name = "Adjacent Color", desc = "Color of the fruit clue (nearby fruit).")
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
    @ConfigOption(name = "Show fruit guesses", desc = "Show guesses for fruits that have not been dug up yet.")
    @ConfigEditorBoolean
    var displayFruitGuesses: Boolean = true

    @Expose
    @ConfigOption(name = "Fruit Guess Color", desc = "Color of the mines clue.")
    @ConfigEditorColour
    var fruitGuessColor: ChromaColour = LorenzColor.AQUA.toChromaColor()
}
