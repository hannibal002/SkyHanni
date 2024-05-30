package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class AccountUpgradeReminder {

    private val patternGroup = RepoPattern.group("accountupgradereminder")
    private val durationPattern by patternGroup.pattern(
        "duration",
        "§8Duration: (?<duration>\\d{1,3})d"
    )
    private val startedPattern by patternGroup.pattern(
        "started",
        "§eYou started the §r§a(?<upgrade>.+) §r§eupgrade!"
    )
    private val claimedPattern by patternGroup.pattern(
        "claimed",
        "§eYou claimed the §r§a.+ §r§eupgrade!"
    )

    private var inInventory = false
    private var duration: Duration? = null
    private var lastReminderSend = SimpleTimeMark.farPast()

    // TODO: find a way to save SimpleTimeMark directly in the config
    private var nextCompletionTime: SimpleTimeMark?
        get() = ProfileStorageData.playerSpecific?.nextAccountUpgradeCompletionTime?.asTimeMark()
        set(value) {
            value?.let {
                ProfileStorageData.playerSpecific?.nextAccountUpgradeCompletionTime = it.toMillis()
            }
        }

    // TODO: Merge this logic with CityProjectFeatures reminder to reduce duplication
    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (!isEnabled()) return
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        if (ReminderUtils.isBusy()) return
        if (LorenzUtils.skyBlockArea == "Community Center") return

        val upgrade = playerSpecific.currentAccountUpgrade ?: return
        val nextCompletionTime = nextCompletionTime ?: return
        if (!nextCompletionTime.isInPast()) return
        if (lastReminderSend.passedSince() < 30.seconds) return
        lastReminderSend = SimpleTimeMark.now()

        ChatUtils.clickableChat(
            "The §a$upgrade §eupgrade has completed! §c(Click to disable these reminders)",
            onClick = {
                disable()
            },
            oneTimeClick = true
        )
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        inInventory = event.inventoryName == "Community Shop"
    }

    @SubscribeEvent
    @Suppress("UNUSED_PARAMETER")
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inInventory) return
        val clickedItemLore = event.slot?.stack?.getLore() ?: return
        if (clickedItemLore.getOrNull(0) != "§8Account Upgrade") return

        clickedItemLore.firstNotNullOfOrNull {
            durationPattern.matchMatcher(it) {
                duration = group("duration").toInt().days
            }
        } ?: return
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (claimedPattern.matches(event.message)) {
            clearUpgrade()
        } else {
            val upgrade = startedPattern.matchMatcher(event.message) {
                group("upgrade")
            } ?: return
            startUpgrade(upgrade)
        }
    }

    private fun startUpgrade(upgrade: String) {
        val duration = duration ?: return
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        playerSpecific.currentAccountUpgrade = upgrade

        nextCompletionTime = SimpleTimeMark.now() + duration
    }

    private fun clearUpgrade() {
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        playerSpecific.currentAccountUpgrade = null
        nextCompletionTime = SimpleTimeMark.farPast()
    }

    companion object {
        private fun isEnabled() = SkyHanniMod.feature.misc.accountUpgradeReminder

        fun disable() {
            SkyHanniMod.feature.misc.accountUpgradeReminder = false
            ChatUtils.chat("Disabled account upgrade reminder.")
        }
    }
}
