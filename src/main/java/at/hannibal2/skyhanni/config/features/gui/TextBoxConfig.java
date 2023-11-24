package at.hannibal2.skyhanni.config.features.gui;

import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class TextBoxConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enables showing the textbox while in SkyBlock.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Text", desc = "Enter text you want to display here.\n" +
        "§eUse '&' as the colour code character.\n" +
        "§eUse '\\n' as the line break character.")
    @ConfigEditorText
    public Property<String> text = Property.of("&aYour Text Here\\n&bYour new line here");

    @Expose
    public Position position = new Position(10, 80, false, true);
}
