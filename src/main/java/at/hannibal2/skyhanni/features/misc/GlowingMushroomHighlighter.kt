package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.expand
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import net.minecraft.init.Blocks
import net.minecraft.util.EnumParticleTypes
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GlowingMushroomHighlighter {

    private val config get() = SkyHanniMod.feature.misc.glowingMushroomHighlighter

    private data class GlowingMushroom(val pos: LorenzVec, var lastParticleTime: SimpleTimeMark)

    private val glowingMushrooms = mutableListOf<GlowingMushroom>()

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onTick() {
        if (!config.enabled) return

        glowingMushrooms.removeIf { it.lastParticleTime.passedSince() > 3.seconds || !it.pos.isValidMushroomLocation() }
    }

    @HandleEvent
    fun onWorldChange() {
        glowingMushrooms.clear()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onParticle(event: ReceiveParticleEvent) {
        if (event.type != EnumParticleTypes.SPELL_MOB) return

        val offsetX = (event.location.x % 1).roundTo(1).absoluteValue
        val offsetY = (event.location.y % 1).roundTo(1).absoluteValue
        val offsetZ = (event.location.z % 1).roundTo(1).absoluteValue

        if (offsetX != 0.5 || offsetY != 0.1 || offsetZ != 0.5) return
        registerMushroom(event.location)
    }

    private fun LorenzVec.isValidMushroomLocation(): Boolean {
        val blockAt = this.getBlockAt()
        return blockAt == Blocks.red_mushroom || blockAt == Blocks.brown_mushroom
    }

    private fun registerMushroom(location: LorenzVec) {
        val toBlockPos = location.roundToBlock()
        if (!toBlockPos.isValidMushroomLocation()) return
        val existing = glowingMushrooms.find { it.pos == toBlockPos }
        if (existing != null) {
            existing.lastParticleTime = SimpleTimeMark.now()
        } else {
            glowingMushrooms.add(GlowingMushroom(toBlockPos, SimpleTimeMark.now()))
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return
        if (glowingMushrooms.isEmpty()) return

        for (mushroom in glowingMushrooms) {
            val aabb = mushroom.pos.boundingToOffset(1.0, 1.0, 1.0).expand(0.001)
            event.drawFilledBoundingBox(aabb, config.mushroomColor.getEffectiveColour(), renderRelativeToCamera = false)
        }
    }
}
