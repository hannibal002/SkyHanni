package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.event.spook.TheGreatSpook.isGreatSpookActive
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils.getAsPosition
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object StatOverlay {
    private val config get() = SkyHanniMod.feature.gui.statDisplayer

    @HandleEvent
    fun onGuiRender() {
        config.displayStats.forEach { statToDisplay ->
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
        val posLabel: String,
        val extraCondition: Boolean = true,
    ) {
        FEROCITY(SkyblockStat.FEROCITY, "Ferocity"),
        FEAR(SkyblockStat.FEAR, "Fear", isGreatSpookActive),
        OVERBLOOM(SkyblockStat.OVERBLOOM, "Overbloom");

        val position get() = config.displayPositions[ordinal]
    }


    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(134, "combat.ferocityDisplay.enabled") { element ->
            if (element.asBoolean) {
                config.displayStats.add(SkyblockStatUI.FEROCITY)
                config.enabled = true
            }
            element
        }
        event.transform(134, "combat.ferocityDisplay.position") { element ->
            config.displayPositions[SkyblockStatUI.FEROCITY.ordinal] = element.getAsPosition()
            element
        }
        event.transform(134, "event.spook.fearStatDisplay") { element ->
            if (element.asBoolean) {
                config.displayStats.add(SkyblockStatUI.FEAR)
                config.enabled = true
            }
            element
        }
        event.transform(134, "event.spook.positionFear") { element ->
            config.displayPositions[SkyblockStatUI.FEAR.ordinal] = element.getAsPosition()
            element
        }
    }
}

