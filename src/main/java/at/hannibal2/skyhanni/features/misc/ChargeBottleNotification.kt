package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.features.fishing.IsFishingDetection.isFishing
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.StringUtils.createCommaSeparatedList
import kotlin.time.Duration.Companion.seconds

@HanniModule
object ChargeBottleNotification {

    private val config get() = HanniMod.feature.misc

    private val emptyBottles = setOf(
        "THUNDER_IN_A_BOTTLE_EMPTY",
        "STORM_IN_A_BOTTLE_EMPTY",
        "HURRICANE_IN_A_BOTTLE_EMPTY",
    ).toInternalNames()

    private val bottles = setOf(
        "THUNDER_IN_A_BOTTLE",
        "STORM_IN_A_BOTTLE",
        "HURRICANE_IN_A_BOTTLE",
    ).toInternalNames()

    private var lastChecked = SimpleTimeMark.farPast()

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (lastChecked.passedSince() < 10.seconds) return

        lastChecked = SimpleTimeMark.now()
        if (!isFishing) return
        if (emptyBottles.any { InventoryUtils.isItemInInventory(it) }) return
        val bottlesInInventory = bottles.filter { InventoryUtils.isItemInInventory(it) }
            .map { it.itemNameWithoutColor }
        if (bottlesInInventory.isEmpty()) return
        val size = bottlesInInventory.size

        ChatUtils.clickableChat(
            "You are currently fishing, but " +
                "${bottlesInInventory.createCommaSeparatedList()} ${StringUtils.pluralize(size, "is", "are")} full. " +
                "Click here to disable this notification.",
            { config::chargeBottleNotification.jumpToEditor() },
            replaceSameMessage = true,
        )
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(76, "misc.thunderBottleNotification", "misc.chargeBottleNotification")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.chargeBottleNotification
}
