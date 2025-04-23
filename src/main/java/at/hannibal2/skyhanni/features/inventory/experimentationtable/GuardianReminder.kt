package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GuardianReminder {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable
    private var lastInventoryOpen = SimpleTimeMark.farPast()
    private var lastErrorSound = SimpleTimeMark.farPast()

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Experimentation Table") return
        lastInventoryOpen = SimpleTimeMark.now()

        DelayedRun.runDelayed(200.milliseconds, ::warn)
    }

    private fun warn() {
        if (ExperimentationTableApi.hasGuardianPet()) return

        ChatUtils.clickToActionOrDisable(
            "Use a §9§lGuardian Pet §efor more Exp in the Experimentation Table.",
            config::guardianReminder,
            actionName = "open pets menu",
            action = { HypixelCommands.pet() },
        )
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Experimentation Table") return
        if (lastInventoryOpen.passedSince() > 2.seconds) return
        if (ExperimentationTableApi.hasGuardianPet()) return

        TitleManager.sendTitle(
            titleText = "§cWrong Pet equipped!",
            duration = 2.seconds,
            location = TitleManager.TitleLocation.INVENTORY,
        )

        if (lastErrorSound.passedSince() > 200.milliseconds) {
            lastErrorSound = SimpleTimeMark.now()
            SoundUtils.playPlingSound()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(59, "inventory.helper.enchanting.guardianReminder", "inventory.experimentationTable.guardianReminder")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.guardianReminder
}
