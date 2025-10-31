package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.roundTo
import at.hannibal2.hanni.utils.blockhighlight.HanniBlockHighlighter
import at.hannibal2.hanni.utils.blockhighlight.TimedHighlightBlock
import net.minecraft.init.Blocks
import net.minecraft.util.EnumParticleTypes
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

@HanniModule
object GlowingMushroomHighlighter {

    private val config get() = HanniMod.feature.misc.glowingMushroomHighlighter

    private val blockHighlighter = HanniBlockHighlighter<TimedHighlightBlock>(
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
