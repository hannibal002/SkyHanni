package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.PositionList
import at.hannibal2.skyhanni.config.core.config.PositionList.Companion.updateConfigPositionList
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.event.spook.TheGreatSpook.isGreatSpookActive
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object StatOverlay {
    private val config get() = SkyHanniMod.feature.gui.statDisplayer
    private val displayPositionsLock = Any()

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay() {
        if (config.displayStats.isEmpty()) return
        config.displayStats.forEach { statToDisplay ->
            if (!statToDisplay.extraCondition()) return@forEach
            val stat = statToDisplay.stat
            val statText = if (config.shortenedStats.contains(statToDisplay))
                stat.icon
            else stat.iconWithName
            val statNum = if (config.integerStats.contains(statToDisplay))
                stat.displayValueInt else stat.displayValueDouble ?: return@forEach
            val displayText = "$statText $statNum"
            synchronized(displayPositionsLock) {
                statToDisplay.position.renderRenderable(Renderable.text(displayText), posLabel = "${statToDisplay.posLabel} Stat Display")
            }
        }
    }

    enum class SkyblockStatUI(
        val stat: SkyblockStat,
        val posLabel: String,
        val extraCondition: () -> Boolean = { true },
    ) {
        FEROCITY(SkyblockStat.FEROCITY, "Ferocity"),
        FEAR(SkyblockStat.FEAR, "Fear", { isGreatSpookActive }),
        OVERBLOOM(SkyblockStat.OVERBLOOM, "Overbloom", { GardenApi.inGarden() }),
        BONUS_PEST_CHANCE(SkyblockStat.BONUS_PEST_CHANCE, "Bonus Pest Chance", { GardenApi.inGarden() }),
        MAGIC_FIND(SkyblockStat.MAGIC_FIND, "Magic Find"),
        ;

        val position get() = config.displayPositions[ordinal]

        override fun toString(): String = this.stat.iconWithName
    }

    @HandleEvent
    fun onProfileJoin() {
        synchronized(displayPositionsLock) {
            with(config.displayPositions) {
                val updatedList = updateConfigPositionList(
                    this,
                    SkyblockStatUI.entries,
                    "gui.statDisplayer.displayPositions",
                )
                clear()
                addAll(updatedList)
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val shortenedStats: MutableList<SkyblockStatUI> = mutableListOf()
        val displayStats = buildList {
            if (event.oldBoolean("combat.ferocityDisplay.enabled")) add(SkyblockStatUI.FEROCITY)
            if (event.oldBoolean("event.spook.fearStatDisplay")) add(SkyblockStatUI.FEAR)
            event.transform(138, "garden.pests.pestChanceDisplay") {
                val oldMode = it.asJsonObject["pestChanceDisplay"].asString
                when (oldMode) {
                    "COMPACT" -> {
                        shortenedStats.add(SkyblockStatUI.BONUS_PEST_CHANCE)
                        add(SkyblockStatUI.BONUS_PEST_CHANCE)
                    }

                    "FULL" -> add(SkyblockStatUI.BONUS_PEST_CHANCE)
                }
                it
            }
        }
        event.add(138, "gui.statDisplayer.displayStats") {
            ConfigManager.gson.toJsonTree(displayStats)
        }
        event.add(138, "gui.statDisplayer.displayPositions") {
            val positions = PositionList(SkyblockStatUI.entries.size)
            event.oldPosition("combat.ferocityDisplay.position")?.let {
                positions[SkyblockStatUI.FEROCITY.ordinal] = it
            }
            event.oldPosition("event.spook.positionFear")?.let {
                positions[SkyblockStatUI.FEAR.ordinal] = it
            }
            event.oldPosition("garden.pests.pestChanceDisplayPosition")?.let {
                positions[SkyblockStatUI.BONUS_PEST_CHANCE.ordinal] = it
            }
            ConfigManager.gson.toJsonTree(positions)
        }

        event.remove(138, "combat.ferocityDisplay.enabled")
        event.remove(138, "combat.ferocityDisplay.position")
        event.remove(138, "event.spook.fearStatDisplay")
        event.remove(138, "event.spook.positionFear")
    }

}
