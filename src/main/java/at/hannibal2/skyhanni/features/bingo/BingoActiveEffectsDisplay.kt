package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.effect.EffectApi
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions

/**
 * While on a Bingo profile, shows a small top-right readout of the total active effects and when
 * the soonest one expires, so the player knows when to get their next god splash. The effect data
 * (count and live remaining time) is parsed centrally by [EffectApi]; this only renders it.
 */
@SkyHanniModule
object BingoActiveEffectsDisplay {

    private val config get() = SkyHanniMod.feature.event.bingo

    // A vanilla potion tinted red (an empty potion renders as a colorless water bottle); the
    // Healing potion's contents give it the classic red potion color.
    private val potionIcon by lazy {
        Renderable.item(
            SafeItemStack(Items.POTION) {
                set(DataComponents.POTION_CONTENTS, PotionContents(Potions.HEALING))
            },
        )
    }

    private var display: Renderable? = null

    @HandleEvent
    private fun onProfileJoin() {
        display = null
    }

    @HandleEvent
    private fun onSecondPassed() {
        if (!isEnabled()) return
        display = buildDisplay()
    }

    private fun buildDisplay(): Renderable {
        val lines = if (EffectApi.totalActiveEffects <= 0) {
            listOf("§cNo potion effects active")
        } else buildList {
            add("§aTotal Effects: §e${EffectApi.totalActiveEffects}")
            EffectApi.soonestActiveEffectRemaining?.let {
                add("§aEffect Duration: §e${it.format(TimeUnit.HOUR)}")
            }
        }
        return Renderable.horizontal(
            potionIcon,
            Renderable.vertical(lines.map { Renderable.text(it) }),
            spacing = 3,
            verticalAlign = VerticalAlignment.CENTER,
        )
    }

    @HandleEvent
    private fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val current = display ?: return
        config.activeEffectsPosition.renderRenderable(current, posLabel = "Bingo Active Effects")
    }

    private fun isEnabled() = SkyBlockUtils.isBingoProfile && config.activeEffectsDisplay
}
