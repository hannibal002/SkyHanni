package at.hannibal2.skyhanni.config.features.chat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerMessagesConfig {

    @Expose
    @ConfigOption(
        name = "Message Order",
        desc = "Drag text to change the the message order for chat messages."
    )
    @ConfigEditorDraggableList
    public List<ChatPart> messageOrder = new ArrayList<>(Arrays.asList(
        ChatPart.SKYBLOCK_LEVEL,
        ChatPart.NAME,
        ChatPart.EMBLEM
    ));

    public enum ChatPart {
        SKYBLOCK_LEVEL("SkyBlock Level"),
        EMBLEM("Emblem"),
        NAME("Name"),
        CRIMSON_FACTION("Crimson Faction"),
        MODE_IRONMAN("Ironman Mode"),
        BINGO_LEVEL("Bingo Level"),
        EMPTY_CHAR("§7<space>"),
        ;

        private final String str;

        ChatPart(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Player Rank Hider", desc = "Hide player ranks in all chat messages.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean playerRankHider = false;

    @Expose
    @ConfigOption(name = "Ignore YouTube", desc = "Do not remove the rank for YouTubers")
    @ConfigEditorBoolean
    public boolean ignoreYouTube = false;

    @Expose
    @ConfigOption(name = "Chat Filter", desc = "Scan messages sent by players for blacklisted words and gray out the message if any are found.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean chatFilter = false;

    @Expose
    @ConfigOption(name = "Same Chat Color", desc = "All players, also those with ranks, have the same, white chat color.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean sameChatColor = true;
}
