package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.features.gui.bar.BarEntry;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.List;

public class BarConfig {

    @Expose
    @ConfigOption(name = "Toggle", desc = "Enable/Disable the Bar.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Appearance", desc = "Change the appearance of the Bar.")
    @ConfigEditorDraggableList(requireNonEmpty = true)
    public List<BarEntry> entries = new ArrayList<>(BarEntry.getEntries());

    @Expose
    @ConfigOption(name = "Color", desc = "Change the color of the Bar.")
    @ConfigEditorColour
    public String color = "0:180:0:0:0";

    @Expose
    @ConfigOption(name = "Spacing", desc = "Change the spacing between the Bar entries.")
    @ConfigEditorSlider(minValue = 0, maxValue = 40, minStep = 1)
    public int spacing = 5;

}
