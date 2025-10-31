package at.hannibal2.hanni.features.inventory.experimentationtable.superpairs

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.ExperimentationTableApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.experiments.TableRareUncoverEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SoundUtils.createSound
import at.hannibal2.hanni.utils.SoundUtils.playSound
import kotlin.time.Duration.Companion.seconds

@HanniModule
object UltraRareBookAlert {

    private val config get() = HanniMod.feature.inventory.experimentationTable
    private val dragonSound by lazy { createSound("mob.enderdragon.growl", 1f) }

    private var enchantsFound = false

    private var lastNotificationTime = SimpleTimeMark.farPast()

    private fun notification(enchantsName: String) {
        lastNotificationTime = SimpleTimeMark.now()
        dragonSound.playSound()
        ChatUtils.chat("You have uncovered a §d§kXX§5 ULTRA-RARE BOOK! §d§kXX§e! You found: §9$enchantsName")
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (lastNotificationTime.passedSince() > 5.seconds) return

        TitleManager.sendTitle(
            titleText = "§d§kXX§5 ULTRA-RARE BOOK! §d§kXX",
            duration = 2.seconds,
            location = TitleManager.TitleLocation.INVENTORY,
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onTableRareUncover(event: TableRareUncoverEvent) {
        if (enchantsFound || !isEnabled()) return
        notification(event.dropName)
        enchantsFound = true
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryClose(event: InventoryCloseEvent) {
        enchantsFound = false
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(59, "inventory.helper.enchanting.ultraRareBookAlert", "inventory.experimentationTable.ultraRareBookAlert")

        val pathBase = "inventory.experimentationTable"
        event.move(93, "$pathBase.ultraRareBookAlert", "$pathBase.superpairs.ultraRareBookAlert")
    }

    private fun isEnabled() = config.superpairs.ultraRareBookAlert && ExperimentationTableApi.inSuperpairs
}
