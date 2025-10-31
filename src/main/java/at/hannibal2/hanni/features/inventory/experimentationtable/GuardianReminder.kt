package at.hannibal2.hanni.features.inventory.experimentationtable

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.ExperimentationTableApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SoundUtils
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@HanniModule
object GuardianReminder {

    private val config get() = HanniMod.feature.inventory.experimentationTable
    private var lastInventoryOpen = SimpleTimeMark.farPast()
    private var lastErrorSound = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.guardianReminder) return
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

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (!config.guardianReminder) return
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
}
