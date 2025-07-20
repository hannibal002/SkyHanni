package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FilterTypesConfig {

    @Expose
    @ConfigOption(name = "Annoying Spam", desc = "")
    @Accordion
    val spam: AnnoyingSpamFilterConfig = AnnoyingSpamFilterConfig()

    @Expose
    @ConfigOption(name = "Combat", desc = "")
    @Accordion
    val combat: CombatFilterConfig = CombatFilterConfig()

    @Expose
    @ConfigOption(name = "Crystal Nucleus", desc = "")
    @Accordion
    val crystalNucleus: CrystalNucleusConfig = CrystalNucleusConfig()

    @Expose
    @ConfigOption(name = "Dungeons", desc = "")
    @Accordion
    val dungeon: DungeonFilterConfig = DungeonFilterConfig()

    @Expose
    @ConfigOption(name = "Events", desc = "")
    @Accordion
    val eventsFilter: EventsFilterConfig = EventsFilterConfig()

    @Expose
    @ConfigOption(name = "Foraging", desc = "")
    @OnlyModern
    @Accordion
    val foraging: ForagingFilterConfig = ForagingFilterConfig()

    @Expose
    @ConfigOption(name = "Garden", desc = "")
    @Accordion
    val garden: GardenFilterConfig = GardenFilterConfig()

    @Expose
    @ConfigOption(name = "Hunting", desc = "")
    @OnlyModern
    @Accordion
    val hunting: HuntingFilterConfig = HuntingFilterConfig()

    @Expose
    @ConfigOption(name = "Hypixel Messages", desc = "")
    @Accordion
    val hypixelMessages: HypixelMessagesConfig = HypixelMessagesConfig()

    @Expose
    @ConfigOption(name = "Notifications", desc = "")
    @Accordion
    val uselessNotifications: UselessNotificationsFilterConfig = UselessNotificationsFilterConfig()

    @Expose
    @ConfigOption(name = "Party", desc = "")
    @Accordion
    val party: PartyFilterConfig = PartyFilterConfig()

    @Expose
    @ConfigOption(name = "Powder Mining", desc = "")
    @Accordion
    val powderMining: PowderMiningConfig = PowderMiningConfig()

    @Expose
    @ConfigOption(name = "Slayers", desc = "")
    @Accordion
    val slayers: SlayerFilterConfig = SlayerFilterConfig()

    @Expose
    @ConfigOption(name = "Transactions", desc = "")
    @Accordion
    val transaction: TransactionConfig = TransactionConfig()

    @Expose
    @ConfigOption(name = "Useless Drops", desc = "")
    @Accordion
    val uselessDrops: UselessDropsFilterConfig = UselessDropsFilterConfig()

    @Expose
    @ConfigOption(name = "Warnings", desc = "")
    @Accordion
    val warnings: WarningsFilterConfig = WarningsFilterConfig()

    @Expose
    @ConfigOption(name = "Stash Messages", desc = "")
    @Accordion
    val stashMessages: StashConfig = StashConfig()
}
