package at.hannibal2.skyhanni.config.features.event.bingo

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BingoNetConfig {
    @Expose
    @ConfigOption(name = "Enable Bingo Net", desc = "Bingo Net is based on a closed Source Project by Hype_the_Time")
    @ConfigEditorBoolean
    @FeatureToggle
    var useBN: Boolean = false

    //TODO requires restart rn still so fix somehow?

    @Expose
    @ConfigOption(name = "Bingo Net Splashes", desc = "Show Splashes announced via the Bingo Net Server.")
    @ConfigEditorBoolean
    var showSplashes: Boolean = true

    @Expose
    @ConfigOption(name = "Bingo Net ChChests", desc = "Subscribe to the Bingo Net ChChests.")
    @ConfigEditorBoolean
    var chestWaypoints: Boolean = true

    @Expose
    @ConfigOption(name = "Allow Server Invite", desc = "Allows the BingoNet Server to Manage your parties. This is required for some Features.")
    @ConfigEditorBoolean
    var allowBNServerPartyManagement: Boolean = true

    @Expose
    @ConfigOption(name = "Show Bingo Chat", desc = "Bingo Chat is a Chat every Bingo Net ")
    @ConfigEditorBoolean
    var showBingoChat: Boolean = true

    @Expose
    @ConfigOption(name = "Bingo Net API/Legacy Key", desc = "API/Legacy Key can be used instead of Mojang Auth. This prevents the possible restart your client message when the Mojang Tokens expired. Leave empty to use Mojang Auth.")
    @ConfigEditorBoolean
    var BNApiKey: String = ""

    val showGoalCompletions: Boolean = false
    val showCardCompletions: Boolean = false
    //TODO hide unless you have splasher perm?
    val autoSplashStatusUpdates: Boolean = true

    val showPacketTraffic = false
}
