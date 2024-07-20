package at.hannibal2.skyhanni.config.features.garden;

import at.hannibal2.skyhanni.utils.LorenzColor;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.List;

public class TrackingConfig {

    @Expose
    @ConfigOption(name = "Webhook Settings", desc = "")
    @Accordion
    public WebhookConfig webhook = new WebhookConfig();

    public static class WebhookConfig {
        @Expose
        @ConfigOption(name = "URL", desc = "The URL of the webhook.")
        @ConfigEditorText
        public String url = "";

        @Expose
        @ConfigOption(name = "Interval", desc = "The interval in which status updated will be sent.")
        @ConfigEditorSlider(
            minValue = 1,
            maxValue = 10,
            minStep = 1
        )
        public int interval = 5;
    }

    @Expose
    @ConfigOption(name = "Information Displayed", desc = "Change which stats are enabled, and the order they will be displayed in.")
    @ConfigEditorDraggableList
    public List<InformationType> information = new ArrayList<>();

    public enum InformationType {
        FARMING_FORTUNE("§6Farming Fortune ☘", "FF <:farming_fortune:1263201171317854369>"),
        FARMING_WISDOM("§3Farming Wisdom ☯", "FW <:farming_wisdom:1263201172513099788>"),
        BONUS_PEST_CHANCE("§2Bonus Pest Chance ൠ", "Pest Chance <:bonus_pest_chance:1263201675724984370>"),
        SPEED("§fSpeed ✦", "Speed <:speed:1263211269134225488>"),
        STRENGTH("§cStrength ❁", "Strength <:strength:1263293916258631781>"),
        PET("§aCurrent Pet", "Pet <:pets_icon:1263221331915182280>"),
        COOKIE_BUFF("§dCookie Buff", "Cookie <:booster_cookie:1263204080940220498>"),
        GOD_POTION("§cGod Potion", "God Potion <:god_potion:1263204732390871151>"),
        JACOBS_CONTEST("§eJacob's Contest", "Contest <:hoe:1263206591218585640>"),
        ACTIVE_CROP("§aCrop", "Crop <:hoe:1263206591218585640>"),
        ANITA_BUFF("§6Anita's Artifact", "Anita's Artifact <:anitas_artifact:1263212366330335376>"),
        BPS("§eBlocks/Second", "BPS <:bps_sugar:1263285905083465729>"),
        FARMING_SINCE("§fFarming Since", "Farming For uhr");

        public final String name;
        public final String fieldName;

        InformationType(String name, String fieldName) {
            this.name = name;
            this.fieldName = fieldName;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Expose
    @ConfigOption(name = "Embed Colour", desc = "Which color the sidebar of the embed should be (Chroma displays as black).")
    @ConfigEditorDropdown
    public LorenzColor color = LorenzColor.YELLOW;

    @Expose
    @ConfigOption(name = "Message Type", desc = "Shows which way the status will be sent.")
    @ConfigEditorDropdown
    public MessageType messageType = MessageType.NEW_MESSAGE;

    public enum MessageType {
        NEW_MESSAGE("New Message"),
        EDITED_MESSAGE("Edited Message");

        private final String str;

        MessageType(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Tracking", desc = "Send an embed with the options you selected above to your specified webhook.")
    @ConfigEditorBoolean
    public boolean tracking = true;

    public enum Pet {
        BEE("Bee <:bee:1263201131064983673>"),
        CHICKEN("Chicken <:chicken:1263201132658823178>"),
        ELEPHANT("Elephant <:elephant:1263201134466830357>"),
        MOOSHROOM_COW("Mooshroom Cow <:mooshroom_cow:1263201135884374026>"),
        PIG("Pig <:pig:1263201137188802651>"),
        RABBIT("Rabbit <:rabbit:1263201138371727421>"),
        SLUG("Slug <:slug:1263201140086931511>"),
        ;

        public final String petName;

        Pet(String petName) {
            this.petName = petName;
        }

        @Override
        public String toString() {
            String cleanName = name().replace("_", " ").toLowerCase();
            cleanName = cleanName.substring(0, 1).toUpperCase() + cleanName.substring(1);
            if (cleanName.equals("mooshroom cow")) cleanName = "Mooshroom Cow";
            return cleanName;
        }
    }

    public enum Crop {
        WHEAT("Wheat", "<:wheat:1263207588296790048>"),
        POTATO("Potato", "<:potato:1263207583502569522>"),
        CARROT("Carrot", "<:carrot:1263207574472359956>"),
        PUMPKIN("Pumpkin", "<:pumpkin:1263207585004257321>"),
        MELON("Melon", "<:melon:1263207577920213083>"),
        SUGAR_CANE("Sugar Cane", "<:sugar:1263207586463748289>"),
        MUSHROOM("Mushroom", "<:mushroom:1263207580268888096>"), //TODO NEW EMOJI
        CACTUS("Cactus", "<:cactus:1263207572962414724>"),
        COCOA_BEANS("Cocoa Beans", "<:cocoa_beans:1263207576330567795>"),
        NETHER_WART("Nether Wart", "<:nether_wart:1263207581770579970>"),
        ;

        public final String name;
        public final String emoji;

        Crop(String name, String emoji) {
            this.name = name;
            this.emoji = emoji;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Medal {
        BRONZE_MEDAL("<:bronze_medal:1263205786876776541>", 60.0, 70.0),
        SILVER_MEDAL("<:silver_medal:1263205793424085053>", 30.0, 40.0),
        GOLD_MEDAL("<:gold_medal:1263205790362239036>", 10.0, 20.0),
        PLATINUM_MEDAL("<:platinum_medal:1263205791825920091>", 5.0, 10.0),
        DIAMOND_MEDAL("<:diamond_medal:1263205788675997717>", 2.0, 5.0),
        ;

        public final String emoji;
        public final Double requiredNormal;
        public final Double requiredFinnegan;

        Medal(String emoji, Double requiredNormal, Double requiredFinnegan) {
            this.emoji = emoji;
            this.requiredNormal = requiredNormal;
            this.requiredFinnegan = requiredFinnegan;
        }

        @Override
        public String toString() {
            return emoji;
        }
    }
}
