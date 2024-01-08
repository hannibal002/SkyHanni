package at.hannibal2.skyhanni.config.features.gui.customscoreboard;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorColour;
import io.github.moulberry.moulconfig.annotations.ConfigEditorInfoText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class BackgroundConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show a background behind the scoreboard."
    )
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @ConfigOption(
        name = "Background Color",
        desc = "The color of the background."
    )
    @ConfigEditorColour
    public String color = "0:102:0:0:0";

    @Expose
    @ConfigOption(
        name = "Use Custom Background Image",
        desc = "Put that image into a resource pack, using the path \"skyhanni/scoreboard.png\"."
    )
    @ConfigEditorBoolean
    public boolean useCustomBackgroundImage = false;

    @Expose
    @ConfigOption(
        name = "Custom Background",
        desc = "Add an image named \"scoreboard.png\" to your texture pack at \"\\assets\\skyhanni\\scoreboard.png.\" Activate the texture pack in Minecraft, then reload the game."
    )
    @ConfigEditorInfoText
    public String useless;
}
