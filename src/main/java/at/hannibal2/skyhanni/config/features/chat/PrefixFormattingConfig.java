package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.data.hypixel.chat.PlayerNameFormatter;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PrefixFormattingConfig {

    @Expose
    @ConfigOption(name = "Message Text", desc = "The text after the player name and before the message.")
    @ConfigEditorText
    public String messageText = "&f: ";

    @Expose
    @ConfigOption(name = "ALl Chat Prefix", desc = "The text shown at the very left side of chat messages in all chat.")
    @ConfigEditorText
    public String all = "";

    @Expose
    @ConfigOption(name = "Party Chat Prefix", desc = "The text shown at the very left side of chat messages in Party chat.")
    @ConfigEditorText
    public String party = "&9Party &8> ";

    @Expose
    @ConfigOption(name = "Guild Chat Prefix", desc = "The text shown at the very left side of chat messages in Guild chat.")
    @ConfigEditorText
    public String guild = "&2Guild > ";

    @Expose
    @ConfigOption(name = "Private Message Prefix", desc = "The text shown at the very left side of private message chat.")
    @ConfigEditorText
    public String privateMessage = "";

    @ConfigOption(name = "Reset Formatting", desc = "Reset formatting to default text.")
    @ConfigEditorButton(buttonText = "Reset")
    public Runnable resetFormatting = PlayerNameFormatter.Companion::resetPrefix;
}
