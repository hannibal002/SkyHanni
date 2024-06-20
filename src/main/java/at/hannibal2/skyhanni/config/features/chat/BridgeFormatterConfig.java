package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class BridgeFormatterConfig {

    /* TODO: Impl these configs:
    *   - Enable Bridge
	Reformats guild bridge messages

        - Bridge bot name
	IGN of the MC account acting as the bridge. Case Sensitive.

        - Bridge format: %ign%, %msg%
	Message format that will be sent when a message is sent from Discord. Uses ^ for sender and message. Use & for color codes.
    Test mine: &4[Discord] &6&l%ign%&r:%msg%

        ? (Test bridge msg)
    * */

    @Expose
    @ConfigOption(
        name = "Enable Bridge Formatting",
        desc = "Reformat guild bridge messages."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;



    @Expose
    @ConfigOption(
        name = "Bridge Bot Name",
        desc = "IGN of the Minecraft account acting as the bridge. Case Sensitive."
    )
    @ConfigEditorText
    public String bridgeName = "BotNameHere";

    @Expose
    @ConfigOption(
        name = "Bridge Format Info",
        desc = "§7For the setting below, use §e%ign% §7for sender and §e%msg% §7for message. Use & for color codes."
    )
    @ConfigEditorInfoText
    public String info = "";

    @Expose
    @ConfigOption(
        name = "Bridge Format",
        desc = "Message format that will be sent when a message is sent from Discord. "
    )
    @ConfigEditorText
    public String bridgeFormat = "&c[Discord] &6&l%ign%&r:%msg%";

}
