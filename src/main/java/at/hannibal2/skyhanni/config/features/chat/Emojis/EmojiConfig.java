package at.hannibal2.skyhanni.config.features.chat.Emojis;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class EmojiConfig {
    @Expose
    @ConfigOption(name = "Emoji Replacer", desc = "Disable the ones you have access to")
    @Accordion
    public EmojiReplacerConfig emojiReplace = new EmojiReplacerConfig();

    @Expose
    @ConfigOption(name = "Enabled", desc = "Simulates Hypixel's emojis.\n" +
        "Example: §r§f<3 §r§6➜§r §r§f❤")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = true;

    @Expose
    @ConfigOption(name = "Recolor Emojis", desc = "Recolors colorless emojis.\n" +
        "Example: §r§f❤ §r§6➜§r §r§c❤§r")
    @ConfigEditorBoolean
    public boolean colorEmoji = true;
}
