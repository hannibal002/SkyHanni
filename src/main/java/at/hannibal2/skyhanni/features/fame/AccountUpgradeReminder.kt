package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AccountUpgradeReminder {
    private var inInventory = false
    private var durationDays = -1
    private var lastReminderSend = 0L

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
        if (System.currentTimeMillis() <= playerSpecific.nextAccountUpgradeCompletionTime ) return

        if (lastReminderSend + 30_000 > System.currentTimeMillis()) return
        lastReminderSend = System.currentTimeMillis()

        LorenzUtils.clickableChat(
            "§e[SkyHanni] The §a$upgrade §eupgrade has completed! §c(Click to disable these reminders)",
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
        durationDays = clickedItemLore.firstNotNullOf {
            durationRegex.matchEntire(it)
        }.groups[1]!!.value.toInt()
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
        if (durationDays == -1) return
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        playerSpecific.currentAccountUpgrade = upgrade
        playerSpecific.nextAccountUpgradeCompletionTime = System.currentTimeMillis() + durationDays * MILLIS_IN_DAY
    }

    private fun clearUpgrade() {
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        playerSpecific.currentAccountUpgrade = null
        playerSpecific.nextAccountUpgradeCompletionTime = -1L
    }

    companion object {
        private val durationRegex = "§8Duration: (\\d{1,3})d".toRegex()
        private val startedRegex = "§eYou started the §r§a(.+) §r§eupgrade!".toRegex()
        private val claimedRegex = "§eYou claimed the §r§a.+ §r§eupgrade!".toRegex()
        private const val MILLIS_IN_DAY = 1000 * 60 * 60 * 24

        private fun isEnabled() = SkyHanniMod.feature.misc.accountUpgradeReminder

        fun disable() {
            SkyHanniMod.feature.misc.accountUpgradeReminder = false
        }
    }
}