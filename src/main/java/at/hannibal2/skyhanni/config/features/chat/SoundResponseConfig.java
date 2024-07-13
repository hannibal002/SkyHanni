package at.hannibal2.skyhanni.config.features.chat;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SoundResponseConfig {

    @Expose
    @ConfigOption(name = "Meow", desc = "Play a meow any time a meow appears in chat.")
    @ConfigEditorBoolean
    public boolean meow = false;

    @Expose
    @ConfigOption(name = "Bark", desc = "Play a bark any time a woof, arf or bark appears in chat.")
    @ConfigEditorBoolean
    public boolean bark = false;
}
