package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.experiments.TableRareUncoverEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskCompletedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting

@SkyHanniModule
object ExperimentsDryStreakDisplay {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.dryStreak
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
            componentBuilder {
                append("DRY-STREAK ENDED! ") {
                    withColor(ChatFormatting.GREEN)
                    bold = true
                }
                append("You have $finallyFormat")
                append("found an ")
                appendWithColor("ULTRA-RARE ", ChatFormatting.DARK_PURPLE)
                append("after ")
                appendWithColor("${storage.xpSince.shortFormat()} Enchanting Exp ", ChatFormatting.DARK_AQUA)
                append("and ")
                appendWithColor("${storage.attemptsSince} $attemptsFormat", ChatFormatting.DARK_GREEN)
                append("!")
            }
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
