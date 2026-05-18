package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.event.spook.TheGreatSpook.isGreatSpookActive
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object StatOverlay {
    private val config get() = SkyHanniMod.feature.gui.displayStats

    @HandleEvent
    fun onGuiRender() {
        config.forEach { statToDisplay ->
            if (statToDisplay.extraCondition) {
                statToDisplay.stat.displayValue?.let {
                    statToDisplay.position.renderRenderable(Renderable.text(it), posLabel = "${statToDisplay.posLabel} Stat Display")
                }
            }
        }
    }

    @Suppress("Unused")
    enum class SkyblockStatUI(
        val stat: SkyblockStat,
        val position: Position,
        val posLabel: String,
        val extraCondition: Boolean = true,
    ) {
        FEROCITY(SkyblockStat.FEROCITY, Position(10, 80), "Ferocity"),
        FEAR(SkyblockStat.FEAR, Position(30, 30), "Fear", isGreatSpookActive),
        OVERBLOOM(SkyblockStat.OVERBLOOM, Position(5, -105), "Overbloom")
    }


    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {

        val configList: List<SkyblockStatUI> = buildList {
            event.transform(133, "combat.ferocityDisplay.enabled") { element ->
                if (element.asBoolean) add(SkyblockStatUI.FEROCITY)
                element
            }
            event.transform(133, "event.spook.fearStatDisplay") {element ->
                if (element.asBoolean) add(SkyblockStatUI.FEAR)
                element
            }
        }
        val blar = configList
    }
}

