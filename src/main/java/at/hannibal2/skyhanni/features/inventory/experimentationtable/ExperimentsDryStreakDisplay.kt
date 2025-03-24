package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.experiments.TableRareUncoverEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskCompletedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings

@SkyHanniModule
object ExperimentsDryStreakDisplay {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.dryStreak
    private val storage get() = ProfileStorageData.profileSpecific?.experimentation?.dryStreak

    private var display = emptyList<String>()

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!ExperimentationTableApi.inventoriesPattern.matches(InventoryUtils.openInventoryName())) return

        display = drawDisplay()
        config.position.renderStrings(
            display,
            posLabel = "Experimentation Table Dry Streak",
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onTableRareUncover(event: TableRareUncoverEvent) {
        if (!isEnabled()) return
        val storage = storage ?: return
        ChatUtils.chat(
            "§a§lDRY-STREAK ENDED! §eYou have (finally) " +
                "found a §5ULTRA-RARE §eafter §3${storage.xpSince.shortFormat()} Enchanting Exp " +
                "§e and §2${storage.attemptsSince} attempts§e!",
        )
        storage.attemptsSince = 0
        storage.xpSince = 0
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onTableTaskCompleted(event: TableTaskCompletedEvent) {
        val storage = storage ?: return
        storage.xpSince += (event.enchantingXpGained ?: 0L)
        if (event.type == ExperimentationTableApi.ExperimentationTaskType.SUPERPAIRS) {
            storage.attemptsSince++
        }
    }

    private fun drawDisplay() = buildList {
        val storage = storage ?: return@buildList

        add("§cDry-Streak since last §5ULTRA-RARE")

        val colorPrefix = "§e"
        val attemptsSince = storage.attemptsSince
        val xpSince = storage.xpSince.shortFormat()
        val attemptsSuffix = if (attemptsSince == 1) "" else "s"

        if (config.attemptsSince && config.xpSince) {
            add("$colorPrefix ├ $attemptsSince Attempt$attemptsSuffix")
            add("$colorPrefix └ $xpSince XP")
        } else if (config.attemptsSince) {
            add("$colorPrefix └ $attemptsSince Attempt$attemptsSuffix")
        } else {
            add("$colorPrefix └ $xpSince XP")
        }
    }

    private fun isEnabled() = config.enabled && (config.xpSince || config.attemptsSince)
}
