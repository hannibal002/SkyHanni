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
    @ConfigOption(name = "Show treasure", desc = "Show adjacent highest fruit from treasure dousing mode.")
    @ConfigEditorBoolean
    var displayAdjacentTreasure: Boolean = true

    @Expose
    @ConfigOption(name = "Adjacent Color", desc = "Color of the treasure clue (nearby fruit).")
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
    @ConfigOption(name = "Remaining Fruit Display", desc = "Show remaining Fruit Digging components.")
    @ConfigEditorBoolean
    var remainingFruitDisplay: Boolean = true

    @Expose
    @ConfigLink(owner = FruitDiggingConfig::class, field = "remainingFruitDisplay")
    val remainingFruitPosition: Position = Position(200, 20)
}
