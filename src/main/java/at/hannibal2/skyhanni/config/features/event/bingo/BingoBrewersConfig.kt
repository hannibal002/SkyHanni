package at.hannibal2.skyhanni.config.features.event.bingo

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BingoBrewersConfig {
    @Expose
    @ConfigOption(name = "Enable BB Integration", desc = "Whether your want to enable our BingoBrewers Port. (Since no official modern support)")
    @ConfigEditorBoolean
    @FeatureToggle
    var useBB: Boolean = false

    @Expose
    @ConfigOption(name = "Show Splashes", desc = "Show Splashes announced by Bingo Brewers")
    @ConfigEditorBoolean
    var showSplashes: Boolean = true

    @Expose
    @ConfigOption(name = "Show Private Splashes", desc = "Show Private Hub Splashes announced by Bingo Brewers")
    @ConfigEditorBoolean
    var showPrivateSplashes: Boolean = true

    @Expose
    @ConfigOption(name = "Subscribe ChChests Waypoint", desc = "Subscribe and load ChChest Waypoints from the Bingo Brewers Network.")
    @ConfigEditorBoolean
    var chestWaypoints: Boolean = true

    @Expose
    @ConfigOption(name = "Allow Server Invite", desc = "Allow Bingo Brewers to party and warp Players into the Lobby on your behalf. §cNot implemented by Indigo Polecat yet but planned.")
    @ConfigEditorBoolean
    var allowBBServerPartyManagement: Boolean = true

    @Expose
    @ConfigOption(name = "Show Packet Traffic", desc = "Shows the Packet Traffic with the Bingo Brewers Network")
    @ConfigEditorBoolean
    var showPacketTraffic = false
}
