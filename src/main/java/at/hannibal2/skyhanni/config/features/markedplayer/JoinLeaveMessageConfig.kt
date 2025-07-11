package at.hannibal2.skyhanni.config.features.markedplayer

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class JoinLeaveMessageConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Join/Leave message for marked players.")
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Players List",
        desc = "The list of players you want to be notified for.\n" +
            "§cCase sensitive, separated by commas.",
    )
    @ConfigEditorText
    val playersList: Property<String> = Property.of("hannibal2,Minikloon")

    @Expose
    @ConfigOption(name = "Use Prefix", desc = "Should the [SkyHanni] prefix be included in the join/leave message?")
    @ConfigEditorBoolean
    var usePrefix: Boolean = true

    @Expose
    @ConfigOption(
        name = "Join Message",
        desc = "Configure the message when someone joins.\n&& is replaced with the minecraft color code §.\n" +
            "%s is replaced with the player name.",
    )
    @ConfigEditorText
    var joinMessage: String = "&&b%s &&ajoined your lobby."

    @Expose
    @ConfigOption(
        name = "Left Message",
        desc = "Configure the message when someone leaves.\n&& is replaced with the minecraft color code §.\n" +
            "%s is replaced with the player name.",
    )
    @ConfigEditorText
    var leftMessage: String = "&&b%s &&cleft your lobby."

}
