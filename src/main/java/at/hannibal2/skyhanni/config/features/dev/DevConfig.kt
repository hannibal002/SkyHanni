package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.NoConfigLink
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.dev.minecraftconsole.MinecraftConsoleConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class DevConfig {

    @Expose
    @ConfigOption(name = "Repository", desc = "")
    @Accordion
    val repo: RepositoryConfig = RepositoryConfig()

    @Expose
    @ConfigOption(name = "Neu Repository", desc = "")
    @Accordion
    val neuRepo: NeuRepositoryConfig = NeuRepositoryConfig()

    @Expose
    @ConfigOption(name = "Debug", desc = "")
    @Accordion
    val debug: DebugConfig = DebugConfig()

    @Expose
    @ConfigOption(name = "Repo Pattern", desc = "")
    @Accordion
    val repoPattern: RepoPatternConfig = RepoPatternConfig()

    @Expose
    @ConfigOption(name = "Log Expiry Time", desc = "Deletes your SkyHanni logs after this time period in days.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 30f, minStep = 1f)
    var logExpiryTime: Int = 14

    @Expose
    @ConfigOption(name = "Backup Expiry Time", desc = "Deletes your backups of SkyHanni configs after this time period in days.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 30f, minStep = 1f)
    var configBackupExpiryTime: Int = 7

    @Expose
    @ConfigOption(
        name = "Chat History Length",
        desc = "The number of messages to keep in memory for §e/shchathistory§7.\n" +
            "§cExcessively high values may cause memory allocation issues."
    )
    @ConfigEditorSlider(minValue = 100f, maxValue = 5000f, minStep = 10f)
    var chatHistoryLength: Int = 100

    @Expose
    @ConfigOption(name = "Slot Number", desc = "Show slot number in inventory while pressing this key.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var showSlotNumberKey: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "World Edit",
        desc = "Use wood axe or command /shworldedit to render a box, similar like the WorldEdit plugin."
    )
    @ConfigEditorBoolean
    var worldEdit: Boolean = false

    @ConfigOption(name = "Parkour Waypoints", desc = "")
    @Accordion
    @Expose
    val waypoint: WaypointsConfig = WaypointsConfig()

    // TODO move these into debug config
    @Expose
    @NoConfigLink
    val debugPos: Position = Position(10, 10)

    @Expose
    @NoConfigLink
    val debugLocationPos: Position = Position(1, 160)

    @Expose
    @NoConfigLink
    val debugItemPos: Position = Position(90, 70)

    @Expose
    @ConfigLink(owner = DebugConfig::class, field = "raytracedOreblock")
    val debugOrePos: Position = Position(1, 200)

    // TODO move [these] to a ContributorAppearanceConfig, or something similar
    @Expose
    @ConfigOption(
        name = "Fancy Contributors",
        desc = "Marks §cSkyHanni's contributors §7fancy in the tab list. " +
            "§eThose are the folks that coded the mod for you for free :)"
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var fancyContributors: Boolean = true

    @Expose
    @ConfigOption(name = "Contributor Nametags", desc = "Makes SkyHanni contributors' nametags fancy too.")
    @ConfigEditorBoolean
    @FeatureToggle
    var contributorNametags: Boolean = true

    @Expose
    @ConfigOption(name = "Flip Contributors", desc = "Make SkyHanni contributors appear upside down in the world.")
    @ConfigEditorBoolean
    @FeatureToggle
    var flipContributors: Boolean = true

    @Expose
    @ConfigOption(
        name = "Spin Contributors",
        desc = "Make SkyHanni contributors spin around when you are looking at them. " +
            "§eRequires 'Flip Contributors' to be enabled."
    )
    @ConfigEditorBoolean
    var rotateContributors: Boolean = false

    @Expose
    @ConfigOption(name = "SBA Contributors", desc = "Mark SBA Contributors the same way as SkyHanni contributors.")
    @ConfigEditorBoolean
    var fancySbaContributors: Boolean = false

    @Expose
    @ConfigOption(name = "Number Format Override", desc = "Forces the number format to use the en_US locale.")
    @ConfigEditorBoolean
    var numberFormatOverride: Boolean = false

    // TODO reenable the setting once the hypixel mod api works fine
    // @Expose
    // @ConfigOption(name = "Use Hypixel Mod API", desc = "Use the Hypixel Mod API for better location data.")
    // @ConfigEditorBoolean
    // var hypixelModApi: Boolean = true

    @Expose
    @ConfigOption(
        name = "Ping API",
        //#if MC < 1.21
        desc = "Use the Hypixel Mod API to calculate your ping.",
        //#else
        //$$ desc = "Make the client always send ping packets to the server as if the debug HUD was open so that we can calculate your ping.",
        //#endif
    )
    @ConfigEditorBoolean
    var pingApi: Boolean = true

    @Expose
    @ConfigOption(
        name = "Damage Indicator",
        desc = "Enable the backend of the Damage Indicator. §cOnly disable when you know what you are doing!"
    )
    @ConfigEditorBoolean
    var damageIndicatorBackend: Boolean = true

    @Expose
    @ConfigOption(
        name = "NTP Server",
        desc = "Change the NTP-Server Address. Default is \"time.google.com\".\n§cONLY CHANGE THIS IF YOU KNOW WHAT YOU'RE DOING!"
    )
    @ConfigEditorText
    var ntpServer: String = "time.google.com"

    @Expose
    @Category(name = "Minecraft Console", desc = "Minecraft Console Settings")
    val minecraftConsoles: MinecraftConsoleConfig = MinecraftConsoleConfig()

    @Expose
    @Category(name = "Dev Tools", desc = "Tooling for devs")
    val devTool: DevToolConfig = DevToolConfig()

    // TODO move into Debug Config?
    @Expose
    @Category(name = "Debug Mob", desc = "Every Debug related to the Mob System")
    val mobDebug: DebugMobConfig = DebugMobConfig()
}
