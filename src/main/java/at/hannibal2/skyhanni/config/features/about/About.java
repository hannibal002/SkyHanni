package at.hannibal2.skyhanni.config.features.about;

import at.hannibal2.skyhanni.features.misc.update.ConfigVersionDisplay;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class About {

    @ConfigOption(name = "Current Version", desc = "This is the SkyHanni version you are running currently")
    @ConfigVersionDisplay
    public transient Void currentVersion = null;

    @ConfigOption(name = "Auto Updates", desc = "Automatically check for updates on each startup")
    @Expose
    @ConfigEditorBoolean
    public boolean autoUpdates = true;

    @ConfigOption(name = "Update Stream", desc = "How frequently do you want updates for SkyHanni")
    @Expose
    @ConfigEditorDropdown
    public Property<UpdateStream> updateStream = Property.of(UpdateStream.RELEASES);


    @ConfigOption(name = "Used Software", desc = "Information about used software and licenses")
    @Accordion
    @Expose
    public Licenses licenses = new Licenses();

}
