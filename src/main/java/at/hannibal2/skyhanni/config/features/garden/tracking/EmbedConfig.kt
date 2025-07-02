package at.hannibal2.skyhanni.config.features.garden.tracking

import at.hannibal2.skyhanni.utils.ApiUtils.SkinBodyPart
import at.hannibal2.skyhanni.utils.LorenzColor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

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
    @ConfigOption(
        name = "Embed Color",
        desc = "Which color the embed sidebar should be (Chroma displays as black). " +
            "§eThis only gets used when 'Use Default' above is disabled.",
    )
    @ConfigEditorDropdown
    var color: LorenzColor = LorenzColor.YELLOW
}
