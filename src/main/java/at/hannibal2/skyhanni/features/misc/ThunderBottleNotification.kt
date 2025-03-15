package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.fishing.IsFishingDetection.isFishing
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.createCommaSeparatedList
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ThunderBottleNotification {

    private val config get() = SkyHanniMod.feature.misc

    private val THUNDER_IN_A_BOTTLE = "THUNDER_IN_A_BOTTLE".toInternalName()
    private val STORM_IN_A_BOTTLE = "STORM_IN_A_BOTTLE".toInternalName()
    private val HURRICANE_IN_A_BOTTLE = "HURRICANE_IN_A_BOTTLE".toInternalName()

    private val bottles = listOf(THUNDER_IN_A_BOTTLE, STORM_IN_A_BOTTLE, HURRICANE_IN_A_BOTTLE)

    private var lastChecked = SimpleTimeMark.farPast()

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (lastChecked.passedSince() < 10.seconds) return
        if (isFishing) {
            lastChecked = SimpleTimeMark.now()
            val bottlesInInventory = bottles.filter { InventoryUtils.isItemInInventory(it) }
                .map { it.itemNameWithoutColor }
            if (bottlesInInventory.isNotEmpty()) {
                val length = bottlesInInventory.size
                ChatUtils.clickableChat(
                    "You are currently fishing, but " +
                        "${bottlesInInventory.createCommaSeparatedList()} ${StringUtils.pluralize(length, "is", "are")} full. " +
                        "Click here to disable this notification.",
                    { config::thunderBottleNotification.jumpToEditor() },
                    replaceSameMessage = true,
                )
            }
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.thunderBottleNotification
}
