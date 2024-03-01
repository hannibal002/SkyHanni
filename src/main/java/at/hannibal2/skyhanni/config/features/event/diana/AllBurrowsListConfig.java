package at.hannibal2.skyhanni.config.features.event.diana;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class AllBurrowsListConfig {

    @Expose
    @ConfigOption(name = "Save Found Burrows", desc = "Save the location of every found burrow in a locally stored list.")
    @ConfigEditorBoolean
    public boolean save = true;

    @Expose
    @ConfigOption(name = "Show All Burrows", desc = "Show the list of all ever found burrows in the world.")
    @ConfigEditorBoolean
    public boolean showAll = false;
}
