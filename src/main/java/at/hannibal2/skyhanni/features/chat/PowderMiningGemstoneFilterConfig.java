package at.hannibal2.skyhanni.features.chat;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PowderMiningGemstoneFilterConfig {

    @Expose
    @ConfigOption(name = "Stronger Tool Messages", desc = "Hide 'You need a stronger tool..' messages.")
    @ConfigEditorBoolean
    public boolean strongerToolMessages = true;

    @Expose
    @ConfigOption(name = "Ruby", desc = "Hide Ruby gemstones under a certain quality.")
    @ConfigEditorDropdown
    public GemstoneFilterEntry rubyGemstones = GemstoneFilterEntry.FINE_ONLY;

    @Expose
    @ConfigOption(name = "Sapphire", desc = "Hide Sapphire gemstones under a certain quality.")
    @ConfigEditorDropdown
    public GemstoneFilterEntry sapphireGemstones = GemstoneFilterEntry.FINE_ONLY;

    @Expose
    @ConfigOption(name = "Amber", desc = "Hide Amber gemstones under a certain quality.")
    @ConfigEditorDropdown
    public GemstoneFilterEntry amberGemstones = GemstoneFilterEntry.FINE_ONLY;

    @Expose
    @ConfigOption(name = "Amethyst", desc = "Hide Amethyst gemstones under a certain quality.")
    @ConfigEditorDropdown
    public GemstoneFilterEntry amethystGemstones = GemstoneFilterEntry.FINE_ONLY;

    @Expose
    @ConfigOption(name = "Jade", desc = "Hide Jade gemstones under a certain quality.")
    @ConfigEditorDropdown
    public GemstoneFilterEntry jadeGemstones = GemstoneFilterEntry.FINE_ONLY;

    @Expose
    @ConfigOption(name = "Topaz", desc = "Hide Topaz gemstones under a certain quality.")
    @ConfigEditorDropdown
    public GemstoneFilterEntry topazGemstones = GemstoneFilterEntry.FINE_ONLY;

    public enum GemstoneFilterEntry {
        SHOW_ALL("Show All"),
        HIDE_ALL("Hide all"),
        FLAWED_UP("Show §aFlawed§7+"),
        FINE_ONLY("Show §9Fine §7only");

        private final String str;

        GemstoneFilterEntry(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
