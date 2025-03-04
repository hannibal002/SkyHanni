package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CakeTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Tracks which Cakes you have/need.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Note",
        desc = "This feature is not compatible with the NEU Storage Overlay." +
            "Backpacks/Ender Chest will not be scanned correctly with it enabled."
    )
    @ConfigEditorInfoText
    var incompatibleNote: Boolean = false

    @Expose
    @ConfigLink(owner = CakeTrackerConfig::class, field = "enabled")
    var cakeTrackerPosition: Position = Position(300, 300, false, true)

    @Expose
    var displayType: Property<CakeTrackerDisplayType> = Property.of(CakeTrackerDisplayType.MISSING_CAKES)

    enum class CakeTrackerDisplayType(private val displayName: String) {
        MISSING_CAKES("§cMissing Cakes"),
        OWNED_CAKES("§aOwned Cakes"),
        ;

        override fun toString() = displayName
    }

    @Expose
    var displayOrderType: Property<CakeTrackerDisplayOrderType> = Property.of(CakeTrackerDisplayOrderType.OLDEST_FIRST)

    enum class CakeTrackerDisplayOrderType(private val displayName: String) {
        OLDEST_FIRST("§cOldest First"),
        NEWEST_FIRST("§aNewest First"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Price on Hover", desc = "Show the prices of cakes when hovering over them in the tracker.")
    @ConfigEditorBoolean
    var priceOnHover: Boolean = true

    @Expose
    @ConfigOption(
        name = "Missing Color",
        desc = "The color that should be used to highlight unobtained cakes in the Auction House."
    )
    @ConfigEditorColour
    var unobtainedAuctionHighlightColor: String = LorenzColor.RED.toConfigColor()

    @Expose
    @ConfigOption(
        name = "Owned Color",
        desc = "The color that should be used to highlight obtained cakes in the Auction House."
    )
    @ConfigEditorColour
    var obtainedAuctionHighlightColor: String = LorenzColor.GREEN.toConfigColor()

    @Expose
    @ConfigOption(name = "Max Height", desc = "Maximum height of the tracker.")
    @ConfigEditorSlider(minValue = 50f, maxValue = 500f, minStep = 10f)
    var maxHeight: Property<Float> = Property.of(250f)
}
