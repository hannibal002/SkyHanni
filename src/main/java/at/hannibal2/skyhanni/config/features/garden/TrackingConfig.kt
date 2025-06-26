package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.ApiUtils.SkinBodyPart
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import java.util.Locale


class TrackingConfig {

    @ConfigOption(
        name = "§cNotice",
        desc = "This feature allows you to send your in-game farming stats to a Discord webhook " +
            "that §6YOU §7choose. §4NO §7sensitive data, e.g. session tokens, is ever collected or sent.",
    )
    @ConfigEditorInfoText
    var notice: String = ""

    @ConfigOption(
        name = "Source",
        desc = "Click to open the source code for this feature.\n" +
            "§eClicking this will open a webpage in your browser.",
    )
    @ConfigEditorButton(buttonText = "OPEN")
    val sourceCodeUrl: Runnable = Runnable {
        OSUtils.openBrowser(
            "https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/" +
                "hannibal2/skyhanni/features/garden/tracking/FarmingStatusTracker.kt",
        )
    }

    @Expose
    @ConfigOption(name = "Enabled", desc = "Send an embed with the options you selected below to your specified webhook.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Webhook Settings", desc = "")
    @Accordion
    val webhook: WebhookConfig = WebhookConfig()

    class WebhookConfig {
        @Expose
        @ConfigOption(name = "URL", desc = "The URL of the webhook.")
        @ConfigEditorText
        var url: String = ""

        @Expose
        @ConfigOption(
            name = "Thread ID",
            desc = "If you want the message to be sent to a thread in the webhook channel put it's id here, otherwise leave blank.",
        )
        @ConfigEditorText
        var threadId: String = ""

        @Expose
        @ConfigOption(name = "Interval", desc = "The interval in which status updated will be sent.")
        @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
        var interval: Int = 5
    }

    @Expose
    @ConfigOption(name = "Embed Settings", desc = "")
    @Accordion
    val embed: EmbedConfig = EmbedConfig()

    class EmbedConfig {
        @Expose
        @ConfigOption(name = "Information Displayed", desc = "Change which stats are enabled, and the order they will be displayed in.")
        @ConfigEditorDraggableList
        val information: MutableList<InformationType> = ArrayList()

        enum class InformationType(val display: String, val fieldName: String) {
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
            ANITA_BUFF("§6Anita Buff", "Anita Buff <:anitas_artifact:1263212366330335376>"),
            BPS("§eBlocks/Second", "BPS <:bps_sugar:1263285905083465729>"),
            FARMING_SINCE("§fFarming Since", "Farming For <:minecraftclock:1264539139911716866>");

            override fun toString(): String = display
        }

        @Expose
        @ConfigOption(name = "Skin Part", desc = "Skin Part to be displayed (image) in the top right of the embed.")
        @ConfigEditorDropdown
        var bodyPart: SkinBodyPart = SkinBodyPart.HEAD

        @Expose
        @ConfigOption(
            name = "Use Default",
            desc = "Use default Discord color scheme for the embed sidebar" +
                "(Online = Green, Idle = Yellow, Offline = Red).",
        )
        @ConfigEditorBoolean
        var useDefault: Boolean = true

        @Expose
        @ConfigOption(name = "Embed Color", desc = "Which color the embed sidebar should be (Chroma displays as black).")
        @ConfigEditorDropdown
        var color: LorenzColor = LorenzColor.YELLOW
    }

    @Expose
    @ConfigOption(name = "Message Type", desc = "Choose which way the status will be sent.")
    @ConfigEditorDropdown
    var messageType: MessageType = MessageType.NEW_MESSAGE

    enum class MessageType(private val str: String) {
        NEW_MESSAGE("New Message"),
        EDITED_MESSAGE("Edited Message");

        override fun toString(): String {
            return str
        }
    }

    enum class Pet(val petName: String) {
        BEE("Bee <:bee:1263201131064983673>"),
        CHICKEN("Chicken <:chicken:1263201132658823178>"),
        ELEPHANT("Elephant <:elephant:1263201134466830357>"),
        MOOSHROOM_COW("Mooshroom Cow <:mooshroom_cow:1263201135884374026>"),
        PIG("Pig <:pig:1263201137188802651>"),
        RABBIT("Rabbit <:rabbit:1263201138371727421>"),
        SLUG("Slug <:slug:1263201140086931511>"),
        ;

        override fun toString(): String {
            var cleanName = name.replace("_", " ").lowercase(Locale.getDefault())
            cleanName = cleanName.substring(0, 1).uppercase(Locale.getDefault()) + cleanName.substring(1)
            if (cleanName == "Mooshroom cow") cleanName = "Mooshroom Cow"
            return cleanName
        }
    }

    enum class Crop(val display: String, val emoji: String) {
        WHEAT("Wheat", "<:wheat:1263207588296790048>"),
        POTATO("Potato", "<:potato:1263207583502569522>"),
        CARROT("Carrot", "<:carrot:1263207574472359956>"),
        PUMPKIN("Pumpkin", "<:pumpkin:1263207585004257321>"),
        MELON("Melon", "<:melon:1263207577920213083>"),
        SUGAR_CANE("Sugar Cane", "<:sugar:1263207586463748289>"),
        MUSHROOM("Mushroom", "<:mushroom:1263207580268888096>"), // TODO NEW EMOJI
        CACTUS("Cactus", "<:cactus:1263207572962414724>"),
        COCOA_BEANS("Cocoa Beans", "<:cocoa_beans:1263207576330567795>"),
        NETHER_WART("Nether Wart", "<:nether_wart:1263207581770579970>"),
        ;

        override fun toString(): String = display
    }
}
