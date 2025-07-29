package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.blockhighlight.SkyHanniBlockHighlighter
import at.hannibal2.skyhanni.utils.blockhighlight.TimedHighlightBlock
import net.minecraft.init.Blocks
import net.minecraft.util.EnumParticleTypes
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GlowingMushroomHighlighter {

    private val config get() = SkyHanniMod.feature.misc.glowingMushroomHighlighter

    private val blockHighlighter = SkyHanniBlockHighlighter<TimedHighlightBlock>(
        highlightCondition = { isEnabled() },
        blockCondition = { it.block == Blocks.red_mushroom || it.block == Blocks.brown_mushroom },
        colorProvider = { config.mushroomColor },
    )

    private fun isEnabled(): Boolean {
        return IslandType.THE_FARMING_ISLANDS.isCurrent() && config.enabled
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (event.type != EnumParticleTypes.SPELL_MOB) return

        val offsetX = (event.location.x % 1).roundTo(1).absoluteValue
        val offsetY = (event.location.y % 1).roundTo(1).absoluteValue
        val offsetZ = (event.location.z % 1).roundTo(1).absoluteValue

        if (offsetX != 0.5 || offsetY != 0.1 || offsetZ != 0.5) return
        blockHighlighter.addBlock(TimedHighlightBlock(event.location, 1.seconds))
    }
}
