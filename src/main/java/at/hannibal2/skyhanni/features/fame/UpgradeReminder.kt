package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object UpgradeReminder {
    private val config get() = SkyHanniMod.feature.misc

    private val patternGroup = RepoPattern.group("fame.upgrades")

    private val accountUpgradePattern by patternGroup.pattern(
        "account",
        "§8Account Upgrade",
    )
    private val profileUpgradePattern by patternGroup.pattern(
        "profile",
        "§8Profile Upgrade",
    )
    private val upgradeDurationPattern by patternGroup.pattern(
        "duration",
        "§8Duration: (?<duration>.+)",
    )
    private val upgradeStartedPattern by patternGroup.pattern(
        "started",
        "§eYou started the §r§a(?<upgrade>.+) §r§eupgrade!",
    )
    private val upgradeClaimedPattern by patternGroup.pattern(
        "claimed",
        "§eYou claimed the §r§a(?<upgrade>.+) §r§eupgrade!",
    )

    @Suppress("UnusedPrivateProperty")
    private val upgradePattern by patternGroup.pattern(
        "upgrade",
        "§eClick to start upgrade!",
    )

    private var currentProfileUpgrade: CommunityShopUpgrade?
        get() = ProfileStorageData.profileSpecific?.communityShopProfileUpgrade
        set(value) {
            ProfileStorageData.profileSpecific?.communityShopProfileUpgrade = value
        }

    private var currentAccountUpgrade: CommunityShopUpgrade?
        get() = ProfileStorageData.playerSpecific?.communityShopAccountUpgrade
        set(value) {
            ProfileStorageData.playerSpecific?.communityShopAccountUpgrade = value
        }

    private var inInventory = false
    private var clickedUpgradeType: UpgradeType? = null
    private var clickedUpgrade: CommunityShopUpgrade? = null
    private var lastReminderSend = SimpleTimeMark.farPast()

    // TODO: (for 0.27) merge this logic with reminder manager
    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (ReminderUtils.isBusy()) return
        if (inInventory || SkyBlockUtils.graphArea == "Community Center") return
        if (lastReminderSend.passedSince() < 30.seconds) return

        currentProfileUpgrade?.sendReminderIfClaimable()
        currentAccountUpgrade?.sendReminderIfClaimable()

        lastReminderSend = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = event.inventoryName == "Community Shop" &&
            event.inventoryItems[11]?.isStainedGlassPane(ColoredBlockCompat.LIME) == true
        if (!inInventory) return

        handleItems(event.inventoryItems)
    }

    private fun handleItems(items: Map<Int, ItemStack>) {
        val hasProfileUpgrade = foundActiveUpgrade(items, 27..35)
        if (!hasProfileUpgrade && currentProfileUpgrade != null) {
            ChatUtils.chat("§eRemoved invalid Profile Upgrade information.")
            currentProfileUpgrade = null
        }

        val hasAccountUpgrade = foundActiveUpgrade(items, 36..44)
        if (!hasAccountUpgrade && currentAccountUpgrade != null) {
            ChatUtils.chat("§eRemoved invalid Account Upgrade information.")
            currentAccountUpgrade = null
        }
    }

    private fun foundActiveUpgrade(items: Map<Int, ItemStack>, slots: IntRange): Boolean {
        for (slot in slots) {
            val item = items[slot] ?: continue
            val lore = item.getLore()
            val isUpgrading = lore.any { it == "§aCurrently upgrading!" }
            val isDone = lore.any { it == "§cClick to claim!" }
            val isReadyForUpgrade = lore.any { it == "§eClick to start upgrade!" }
            if (isUpgrading || isDone) {
                startUpgrade(UpgradeType.fromItem(item), CommunityShopUpgrade.fromItem(item))
                return true
            }
            if (isReadyForUpgrade) continue
        }
        return false
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inInventory) return
        val item = event.item ?: return
        clickedUpgradeType = UpgradeType.fromItem(item) ?: return
        clickedUpgrade = CommunityShopUpgrade.fromItem(item) ?: return
    }

    private fun startUpgrade(type: UpgradeType?, upgrade: CommunityShopUpgrade?) {
        upgrade?.start() ?: return
        when (type) {
            UpgradeType.PROFILE -> currentProfileUpgrade = upgrade
            UpgradeType.ACCOUNT -> currentAccountUpgrade = upgrade
            else -> return
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        if (upgradeStartedPattern.matches(event.message)) {
            startUpgrade(clickedUpgradeType, clickedUpgrade)
            return
        }

        upgradeClaimedPattern.matchMatcher(event.message) {
            val claimedUpgradeName = group("upgrade")
            when (claimedUpgradeName) {
                currentProfileUpgrade?.name -> currentProfileUpgrade = null
                currentAccountUpgrade?.name -> currentAccountUpgrade = null
            }
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.accountUpgradeReminder

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(
            49,
            "#player.currentAccountUpgrade",
            "#player.communityShopAccountUpgrade.name",
        )

        event.move(
            49,
            "#player.nextAccountUpgradeCompletionTime",
            "#player.communityShopAccountUpgrade.completionTime",
        )
    }

    class CommunityShopUpgrade(
        @Expose val name: String?,
        @Expose var completionTime: SimpleTimeMark = SimpleTimeMark.farFuture(),
    ) {
        private var duration: Duration = Duration.ZERO

        fun start() {
            this.completionTime = SimpleTimeMark.now() + duration
        }

        fun sendReminderIfClaimable() {
            if (this.name == null || this.completionTime.isInFuture()) return
            ChatUtils.clickToActionOrDisable(
                "The §a$name §eupgrade has completed!",
                config::accountUpgradeReminder,
                actionName = "warp to Elizabeth",
                action = {
                    HypixelCommands.warp("elizabeth")
                },
            )
        }

        companion object {
            fun fromItem(item: ItemStack): CommunityShopUpgrade? {
                val name = item.hoverName.formattedTextCompatLeadingWhiteLessResets()
                val lore = item.getLore()
                val upgrade = CommunityShopUpgrade(name)
                upgrade.duration = upgradeDurationPattern.firstMatcher(lore) {
                    val durationStr = group("duration")
                    if (durationStr == "Instant!") return null
                    TimeUtils.getDuration(durationStr)
                } ?: Duration.ZERO
                return upgrade
            }
        }
    }

    enum class UpgradeType {
        PROFILE,
        ACCOUNT,
        ;

        companion object {
            fun fromItem(item: ItemStack): UpgradeType? {
                val lore = item.getLore()
                return when {
                    accountUpgradePattern.anyMatches(lore) -> ACCOUNT
                    profileUpgradePattern.anyMatches(lore) -> PROFILE
                    else -> null
                }
            }
        }
    }
}
