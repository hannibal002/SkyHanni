package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.About
import at.hannibal2.skyhanni.config.features.chat.ChatConfig
import at.hannibal2.skyhanni.config.features.combat.CombatConfig
import at.hannibal2.skyhanni.config.features.crimsonisle.CrimsonIsleConfig
import at.hannibal2.skyhanni.config.features.dev.DevConfig
import at.hannibal2.skyhanni.config.features.dungeon.DungeonConfig
import at.hannibal2.skyhanni.config.features.event.EventConfig
import at.hannibal2.skyhanni.config.features.fishing.FishingConfig
import at.hannibal2.skyhanni.config.features.foraging.ForagingConfig
import at.hannibal2.skyhanni.config.features.garden.GardenConfig
import at.hannibal2.skyhanni.config.features.gui.GuiConfig
import at.hannibal2.skyhanni.config.features.hunting.HuntingConfig
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig
import at.hannibal2.skyhanni.config.features.mining.MiningConfig
import at.hannibal2.skyhanni.config.features.misc.MiscConfig
import at.hannibal2.skyhanni.config.features.rift.RiftConfig
import at.hannibal2.skyhanni.config.features.skillprogress.SkillProgressConfig
import at.hannibal2.skyhanni.config.features.slayer.SlayerConfig
import at.hannibal2.skyhanni.config.storage.Storage
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.TimeUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.Social
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.HorizontalAlign
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory

class SkyHanniConfig : Config() {
    private val discord = MyResourceLocation("skyhanni", "social/discord.png")
    private val github = MyResourceLocation("skyhanni", "social/github.png")
    private val patreon = MyResourceLocation("skyhanni", "social/patreon.png")
    private val shSocials = listOf(
        Social.forLink("Discord".asStructuredText(), discord, "https://discord.com/invite/skyhanni-997079228510117908"),
        Social.forLink("GitHub".asStructuredText(), github, "https://github.com/hannibal002/SkyHanni"),
        Social.forLink("Patreon".asStructuredText(), patreon, "https://www.patreon.com/hannibal2"),
    )

    // in moulconfig, this value is currently bugged (version 3.5.0)
    override fun shouldAutoFocusSearchbar(): Boolean = false

    override fun alignCategory(category: ProcessedCategory, isSelected: Boolean): HorizontalAlign {
        if (TimeUtils.isAprilFoolsDay) return HorizontalAlign.RIGHT
        return super.alignCategory(category, isSelected)
    }

    override fun getSocials(): List<Social> = shSocials

    override fun saveNow() = SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "close-gui")

    override fun getTitle(): StructuredText {
        val modName = if (TimeUtils.isAprilFoolsDay) "SkyHanni".reversed() else "SkyHanni"
        return "$modName ${SkyHanniMod.VERSION} by §channibal2§r, config by §5Moulberry §rand §5nea89".asStructuredText()
    }

    /*
     * If you are adding a new category, please insert it alphabetically
     * The only exceptions to this are About and GUI, which are pinned to the top
     * and Misc and Dev, which are to be at the bottom. Thanks!
     */
    // Top
    @Expose
    @Category(name = "About", desc = "Information about SkyHanni and updates.")
    val about: About = About()

    @JvmField
    @Expose
    @Category(name = "GUI", desc = "Change the locations of GUI elements (§e/sh gui§7).")
    val gui: GuiConfig = GuiConfig()

    // Islands
    @Expose
    @Category(name = "Garden", desc = "Features for the Garden island.")
    val garden: GardenConfig = GardenConfig()

    @Expose
    @Category(name = "Crimson Isle", desc = "Things to do on the Crimson Isle/Nether island.")
    val crimsonIsle: CrimsonIsleConfig = CrimsonIsleConfig()

    @Expose
    @Category(name = "The Rift", desc = "Features for The Rift dimension.")
    val rift: RiftConfig = RiftConfig()

    // Skills
    @Expose
    @Category(name = "Fishing", desc = "Fishing stuff.")
    val fishing: FishingConfig = FishingConfig()

    @Expose
    @Category(name = "Mining", desc = "Features that help you break blocks.")
    val mining: MiningConfig = MiningConfig()

    @Expose
    @Category(name = "Foraging", desc = "Features that help you cut down trees.")
    val foraging: ForagingConfig = ForagingConfig()

    @Expose
    @Category(name = "Hunting", desc = "Features that help you hunt mobs for their shards.")
    val hunting: HuntingConfig = HuntingConfig()

    // Combat like
    @Expose
    @Category(name = "Combat", desc = "Everything combat and PvE related.")
    val combat: CombatConfig = CombatConfig()

    @Expose
    @Category(name = "Slayer", desc = "Slayer features.")
    val slayer: SlayerConfig = SlayerConfig()

    @Expose
    @Category(name = "Dungeon", desc = "Features that change the Dungeons experience in The Catacombs.")
    val dungeon: DungeonConfig = DungeonConfig()

    // Misc
    @Expose
    @Category(name = "Inventory", desc = "Change the behavior of items and the inventory.")
    val inventory: InventoryConfig = InventoryConfig()

    @Expose
    @Category(name = "Events", desc = "Stuff that is not always available.")
    val event: EventConfig = EventConfig()

    @Expose
    @Category(name = "Skill Progress", desc = "Skill Progress related config options.")
    val skillProgress: SkillProgressConfig = SkillProgressConfig()

    @Expose
    @Category(name = "Chat", desc = "Change how the chat looks.")
    val chat: ChatConfig = ChatConfig()

    @JvmField
    @Expose
    @Category(name = "Misc", desc = "Settings without a category.")
    val misc: MiscConfig = MiscConfig()

    // Bottom
    @Expose
    @Category(name = "Dev", desc = "Debug and test stuff. Developers are cool.")
    val dev: DevConfig = DevConfig()

    @Expose
    val storage: Storage = Storage()

    @Expose
    @Suppress("unused")
    var lastVersion: Int = ConfigUpdaterMigrator.CONFIG_VERSION

    @Expose
    var lastMinecraftVersion: String? = null
}
