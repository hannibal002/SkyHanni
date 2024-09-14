package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.features.gui.MayorOverlay;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.List;

public class MayorOverlayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Mayor Overlay.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Appearance", desc = "Change the order of appearance of the Mayor Overlay.")
    @ConfigEditorDraggableList
    public List<MayorOverlay> mayorOverlay = new ArrayList<>(MayorOverlay.getEntries());

    @Expose
    @ConfigLink(owner = MayorOverlayConfig.class, field = "enabled")
    public Position position = new Position(10, 10, false, true);

}
