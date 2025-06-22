package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.features.event.bingo.BingoConfig
import at.hannibal2.skyhanni.config.features.event.carnival.CarnivalConfig
import at.hannibal2.skyhanni.config.features.event.diana.DianaConfig
import at.hannibal2.skyhanni.config.features.event.gifting.GiftingConfig
import at.hannibal2.skyhanni.config.features.event.hoppity.HoppityEggsConfig
import at.hannibal2.skyhanni.config.features.event.waypoints.LobbyWaypointsConfig
import at.hannibal2.skyhanni.config.features.event.winter.WinterConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class EventConfig {
    @Category(name = "Bingo", desc = "Monthly Bingo Event settings")
    @Expose
    var bingo: BingoConfig = BingoConfig()

    @Category(name = "Diana", desc = "Diana's Mythological Burrows")
    @Expose
    var diana: DianaConfig = DianaConfig()

    @Category(name = "Winter", desc = "Winter Season on Jerry's Island")
    @Expose
    var winter: WinterConfig = WinterConfig()

    @Category(name = "Gifting", desc = "Giving and receiving gifts")
    @Expose
    var gifting: GiftingConfig = GiftingConfig()

    @Expose
    @Category(name = "Hoppity Eggs", desc = "Features for the Hoppity event that happens every SkyBlock spring.")
    var hoppityEggs: HoppityEggsConfig = HoppityEggsConfig()

    @ConfigOption(name = "City Project", desc = "")
    @Accordion
    @Expose
    var cityProject: CityProjectConfig = CityProjectConfig()

    @ConfigOption(name = "Mayor Jerry's Jerrypocalypse", desc = "")
    @Accordion
    @Expose
    var jerry: MayorJerryConfig = MayorJerryConfig()

    @ConfigOption(name = "The Great Spook", desc = "")
    @Accordion
    @Expose
    var spook: GreatSpookConfig = GreatSpookConfig()

    @Expose
    @Category(name = "The Carnival", desc = "Features for games at §eThe Carnival §7when §bFoxy §7is Mayor.")
    var carnival: CarnivalConfig = CarnivalConfig()

    // comment in if the event is needed again
    //    @ConfigOption(name = "300þ Anniversary Celebration", desc = "Features for the 300þ year of SkyBlock")
    @Accordion
    @Expose
    var century: CenturyConfig = CenturyConfig()

    @ConfigOption(name = "400þ Anniversary Celebration", desc = "Features for the 400þ year of SkyBlock.")
    @Accordion
    @Expose
    var anniversaryCelebration400: AnniversaryCelebration400Config = AnniversaryCelebration400Config()

    @ConfigOption(name = "Year of the Seal", desc = "Features for Year of the Seals.")
    @Accordion
    @Expose
    var yearOfTheSeal: YearOfTheSealConfig = YearOfTheSealConfig()

    @Category(name = "Lobby Waypoints", desc = "Lobby Event Waypoint settings")
    @Expose
    var lobbyWaypoints: LobbyWaypointsConfig = LobbyWaypointsConfig()
}
