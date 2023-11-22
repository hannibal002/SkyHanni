package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class AccountUpgradeReminder {
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


    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.repeatSeconds(1)) {
            checkReminder()
        }
    }

    // TODO: Merge this logic with CityProjectFeatures reminder to reduce duplication
    private fun checkReminder() {
        if (!isEnabled()) return
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        if (ReminderUtils.isBusy()) return
        if (LorenzUtils.skyBlockArea == "Community Center") return

        val upgrade = playerSpecific.currentAccountUpgrade ?: return
        val nextCompletionTime = nextCompletionTime ?: return
        if (!nextCompletionTime.isInPast()) return

        if (lastReminderSend.passedSince() < 30.seconds) return
        lastReminderSend = SimpleTimeMark.now()

        LorenzUtils.clickableChat(
            "The §a$upgrade §eupgrade has completed! §c(Click to disable these reminders)",
            "shstopaccountupgradereminder"
        )
    }

    @SubscribeEvent
    fun onInventoryLoad(event: InventoryFullyOpenedEvent) {
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
        val result = clickedItemLore.firstNotNullOfOrNull {
            durationRegex.matchEntire(it)
        } ?: return
        duration = result.groups[1]!!.value.toInt().days
    }

    @SubscribeEvent
    fun onUpgradeStarted(event: LorenzChatEvent) {
        if (claimedRegex.matches(event.message)) {
            clearUpgrade()
        } else {
            val upgrade = startedRegex.matchEntire(event.message)?.groups?.get(1)?.value ?: return
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
        private val durationRegex = "§8Duration: (\\d{1,3})d".toRegex()
        private val startedRegex = "§eYou started the §r§a(.+) §r§eupgrade!".toRegex()
        private val claimedRegex = "§eYou claimed the §r§a.+ §r§eupgrade!".toRegex()

        private fun isEnabled() = SkyHanniMod.feature.misc.accountUpgradeReminder

        fun disable() {
            SkyHanniMod.feature.misc.accountUpgradeReminder = false
        }
    }
}
