package at.hannibal2.skyhanni.config.features.garden.greenhouse

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseLayoutApi
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlinx.coroutines.Runnable

class GreenhouseLayoutImporterConfig {

    @Expose
    @ConfigOption(name = "Display Type", desc = "Change the way the layout is displayed.")
    @ConfigEditorDropdown
    var displayType: GreenhouseLayoutApi.LayoutDisplayType = GreenhouseLayoutApi.LayoutDisplayType.ALL

    @Expose
    @ConfigOption(name = "Display Radius", desc = "Change the radius in which the layout is displayed around you.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 15f, minStep = 1f)
    var displayRadius = 15f

    @Expose
    @ConfigOption(name = "Grid Rotation", desc = "Change the rotation of the layout.")
    @ConfigEditorDropdown
    var layoutRotation: Property<GreenhouseLayoutApi.LayoutRotation> = Property.of(GreenhouseLayoutApi.LayoutRotation.ZERO)

    @Expose
    @ConfigOption(name = "Input Color", desc = "Change the color of the text labels above input crops/mutations.")
    @ConfigEditorDropdown
    var inputColor: LorenzColor = LorenzColor.GOLD

    @Expose
    @ConfigOption(name = "Target Color", desc = "Change the color of the text labels above target mutations.")
    @ConfigEditorDropdown
    var targetColor: LorenzColor = LorenzColor.GOLD

    @Expose
    @ConfigOption(name = "Show Text Labels", desc = "Display info above mismatched crops and surfaces.")
    @ConfigEditorBoolean
    var showTextLabels: Boolean = true

    @ConfigOption(
        name = "Import Layout",
        desc = "Click this button to automatically import a layout from your clipboard.",
    )
    @ConfigEditorButton(buttonText = "Import")
    val importLayout: Runnable = Runnable { GreenhouseLayoutApi.updateLayoutCommand() }

    @Expose
    @ConfigOption(
        name = "Layout Link",
        desc = "Enter the layout link to display. You can alternatively run /shimportgreenhouse or click the button below.",
    )
    @ConfigEditorText
    var layout: Property<String> = Property.of("")

    @Expose
    @ConfigOption(name = "Enabled", desc = "Displays the imported layout inside your greenhouse.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false
}
