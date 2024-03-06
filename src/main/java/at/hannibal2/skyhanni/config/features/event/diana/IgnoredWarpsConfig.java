package at.hannibal2.skyhanni.config.features.event.diana;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class IgnoredWarpsConfig {

    @Expose
    @ConfigOption(name = "Crypt", desc = "Ignore the Crypt warp point (Because it takes a long time to leave).")
    @ConfigEditorBoolean
    public boolean crypt = false;

    @Expose
    @ConfigOption(name = "Wizard", desc = "Ignore the Wizard Tower warp point (Because it is easy to fall into the rift).")
    @ConfigEditorBoolean
    public boolean wizard = false;

}
