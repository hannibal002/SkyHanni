package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.features.misc.update.ConfigVersionDisplay;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import io.github.moulberry.moulconfig.observer.Property;
import io.github.moulberry.notenoughupdates.util.Utils;

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

    public enum UpdateStream {
        NONE("None", "none"),
        BETA("Beta", "pre"),
        RELEASES("Full", "full");

        private final String label;
        private final String stream;

        UpdateStream(String label, String stream) {
            this.label = label;
            this.stream = stream;
        }

        public String getStream() {
            return stream;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static class Licenses {

        @ConfigOption(name = "MoulConfig", desc = "MoulConfig is available under the LGPL 3.0 License or later version")
        @ConfigEditorButton(buttonText = "GitHub")
        public Runnable moulConfig = () -> Utils.openUrl("https://github.com/NotEnoughUpdates/MoulConfig");

        @ConfigOption(name = "NotEnoughUpdates", desc = "NotEnoughUpdates is available under the LGPL 3.0 License or later version")
        @ConfigEditorButton(buttonText = "GitHub")
        public Runnable notEnoughUpdates = () -> Utils.openUrl("https://github.com/NotEnoughUpdates/NotEnoughUpdates");

        @ConfigOption(name = "Forge", desc = "Forge is available under the LGPL 3.0 license")
        @ConfigEditorButton(buttonText = "GitHub")
        public Runnable forge = () -> Utils.openUrl("https://github.com/MinecraftForge/MinecraftForge");

        @ConfigOption(name = "LibAutoUpdate", desc = "LibAutoUpdate is available under the BSD 2 Clause License")
        @ConfigEditorButton(buttonText = "Git")
        public Runnable libAutoUpdate = () -> Utils.openUrl("https://git.nea.moe/nea/libautoupdate/");

        @ConfigOption(name = "Mixin", desc = "LibAutoUpdate is available under the MIT License")
        @ConfigEditorButton(buttonText = "GitHub")
        public Runnable mixin = () -> Utils.openUrl("https://github.com/SpongePowered/Mixin/");

    }
}
