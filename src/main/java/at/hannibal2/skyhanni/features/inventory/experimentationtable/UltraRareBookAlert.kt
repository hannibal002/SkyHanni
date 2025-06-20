package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.bookPattern
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.ultraRarePattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils.createSound
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object UltraRareBookAlert {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable
    private val dragonSound by lazy { createSound("mob.enderdragon.growl", 1f) }

    private var enchantsFound = false

    private var lastNotificationTime = SimpleTimeMark.farPast()

    private fun notification(enchantsName: String) {
        lastNotificationTime = SimpleTimeMark.now()
        dragonSound.playSound()
        ChatUtils.chat("You have uncovered a §d§kXX§5 ULTRA-RARE BOOK! §d§kXX§e! You found: §9$enchantsName")
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (lastNotificationTime.passedSince() > 5.seconds) return

        TitleManager.sendTitle(
            titleText = "§d§kXX§5 ULTRA-RARE BOOK! §d§kXX",
            duration = 2.seconds,
            location = TitleManager.TitleLocation.INVENTORY,
        )
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        if (enchantsFound) return

        for (lore in event.inventoryItems.map { it.value.getLore() }) {
            val firstLine = lore.firstOrNull() ?: continue
            if (!ultraRarePattern.matches(firstLine)) continue
            val bookNameLine = lore.getOrNull(2) ?: continue
            bookPattern.matchMatcher(bookNameLine) {
                val enchantsName = group("enchant")
                notification(enchantsName)
                enchantsFound = true
            }
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        enchantsFound = false
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(59, "inventory.helper.enchanting.ultraRareBookAlert", "inventory.experimentationTable.ultraRareBookAlert")
    }

    private fun isEnabled() =
        SkyBlockUtils.inSkyBlock && config.ultraRareBookAlert && ExperimentationTableApi.currentExperiment != null
}
