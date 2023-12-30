package at.hannibal2.skyhanni.config.features.chat.Emojis;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class EmojiReplacerConfig {
    @Expose
    @ConfigOption(name = "MVP++", desc = "Enables using emojis unlocked by having MVP++.")
    @ConfigEditorBoolean
    public boolean mvp = true;

    @Expose
    @ConfigOption(name = "5 Gifted Ranks", desc = "Enables using emojis unlocked by gifting 5 ranks.")
    @ConfigEditorBoolean
    public boolean five = true;

    @Expose
    @ConfigOption(name = "20 Gifted Ranks", desc = "Enables using emojis unlocked by gifting 20 ranks.")
    @ConfigEditorBoolean
    public boolean twenty = true;

    @Expose
    @ConfigOption(name = "50 Gifted Ranks", desc = "Enables using emojis unlocked by gifting 50 ranks.")
    @ConfigEditorBoolean
    public boolean fifty = true;

    @Expose
    @ConfigOption(name = "100 Gifted Ranks", desc = "Enables using emojis unlocked by gifting 100 ranks.")
    @ConfigEditorBoolean
    public boolean hundred = true;

    @Expose
    @ConfigOption(name = "200 Gifted Ranks", desc = "Enables using emojis unlocked by gifting 200 ranks.")
    @ConfigEditorBoolean
    public boolean twoHundred = true;
}
