package at.hannibal2.hanni.features.inventory.experimentationtable

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.ExperimentationTableApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.experiments.TableRareUncoverEvent
import at.hannibal2.hanni.events.experiments.TableTaskCompletedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.RenderUtils.renderStrings
import at.hannibal2.hanni.utils.StringUtils.pluralize
import at.hannibal2.hanni.utils.collection.CollectionUtils.takeIfNotEmpty

@HanniModule
object ExperimentsDryStreakDisplay {

    private val config get() = HanniMod.feature.inventory.experimentationTable.dryStreak
    private val storage get() = ProfileStorageData.profileSpecific?.experimentation?.dryStreak
    private var display = emptyList<String>()
    private var ignoreNextFinish = false

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled() || !ExperimentationTableApi.inTable) return

        display = display.takeIfNotEmpty() ?: drawDisplay()
        config.position.renderStrings(
            display,
            posLabel = "Experimentation Table Dry Streak",
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onTableRareUncover(event: TableRareUncoverEvent) {
        if (!isEnabled()) return
        val storage = storage ?: return
        val attemptsFormat = "attempt".pluralize(storage.attemptsSince)
        val finallyFormat = if (storage.attemptsSince >= 10) "§o(finally)§r§e " else ""
        ChatUtils.chat(
            "§a§lDRY-STREAK ENDED! §eYou have $finallyFormat" +
                "found an §5ULTRA-RARE §eafter §3${storage.xpSince.shortFormat()} Enchanting Exp " +
                "§eand §2${storage.attemptsSince} $attemptsFormat§e!",
        )
        storage.attemptsSince = 0
        storage.xpSince = 0
        display = drawDisplay()
        ignoreNextFinish = true
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onTableTaskCompleted(event: TableTaskCompletedEvent) {
        if (ignoreNextFinish) {
            ignoreNextFinish = false
            return
        }
        val storage = storage ?: return
        storage.xpSince += (event.enchantingXpGained ?: 0L)
        if (event.type == ExperimentationTableApi.ExperimentationTaskType.SUPERPAIRS) {
            storage.attemptsSince++
        }
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        if (!isEnabled()) return@buildList
        val storage = storage ?: return@buildList

        add("§cDry-Streak since last §5ULTRA-RARE")

        val attemptsSince = storage.attemptsSince
        val xpSince = storage.xpSince.shortFormat()
        val attemptFormat = "Attempt".pluralize(attemptsSince)

        if (config.attemptsSince && config.xpSince) {
            add("§e ├ $attemptsSince $attemptFormat")
            add("§e └ $xpSince XP")
        } else if (config.attemptsSince) {
            add("§e └ $attemptsSince $attemptFormat")
        } else {
            add("§e └ $xpSince XP")
        }
    }

    private fun isEnabled() = config.enabled && (config.xpSince || config.attemptsSince)
}
