package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DarkMonolithFeatures {

    private val config get() = SkyHanniMod.feature.mining.darkMonolith
    private var knownEggs: Set<LorenzVec> = setOf()
    private var foundEggVec: LorenzVec? = null
    private var lastFoundEggVec: LorenzVec? = null
    private var renderBox: AxisAlignedBB? = null

    @HandleEvent
    fun onWorldChange() {
        knownEggs = setOf()
        foundEggVec = null
        renderBox = null
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onSecondPassed() {
        if (!isEnabled()) return
        knownEggs = BlockUtils.nearbyBlocks(
            LocationUtils.playerLocation(),
            distance = 40,
            filter = Blocks.dragon_egg,
        ).keys
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onTick() {
        if (!isEnabled()) return
        foundEggVec = knownEggs.firstOrNull { it.canBeSeen() }
        checkTitle()
        lastFoundEggVec = foundEggVec
        val knownEggVec = foundEggVec ?: return
        renderBox = knownEggVec.boundingToOffset(1.0, 1.0, 1.0).expandBlock()
    }

    private fun checkTitle() {
        if (lastFoundEggVec != null || foundEggVec == null) return
        val titleText = config.title.takeIf { it.isNotEmpty() } ?: return
        TitleManager.sendTitle(titleText, duration = 3.seconds)
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        val knownEggVec = foundEggVec ?: return
        if (knownEggVec.distanceToPlayer() >= 100) return
        val axis = renderBox ?: return
        event.drawFilledBoundingBox(axis, config.highlightColor.toColor())
    }


    private fun isEnabled() = config.highlight || config.title.isNotEmpty()
}
