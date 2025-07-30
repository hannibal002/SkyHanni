package at.hannibal2.skyhanni.utils.blockhighlight

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.expand
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.block.state.IBlockState

/**
 * A [SkyHanniBlockHighlighter] is used to highlight blocks based on a certain condition
 *
 * @property highlightCondition A condition for when the highlighter should be running its code such as an isEnabled function.
 * @property blockCondition A condition that the blockstate at a location must fulfill to be highlighted.
 * @property colorProvider Provides the color that the highlighter will use when rendering the highlighted block.
 */
class SkyHanniBlockHighlighter<T : AbstractHighlightedBlock>(
    val highlightCondition: () -> Boolean,
    val blockCondition: (IBlockState) -> Boolean,
    val colorProvider: () -> ChromaColour,
) {

    private val blocksToHighlight = mutableListOf<T>()
    private val blocksLock = Any()

    fun addBlock(blockToAdd: T) {
        if (!checkIsValid(blockToAdd)) return

        synchronized(blocksLock) {
            val existingEntryAtLocation = blocksToHighlight.firstOrNull { it.location == blockToAdd.location }

            when (existingEntryAtLocation) {
                null -> blocksToHighlight.add(blockToAdd)
                is TimedHighlightBlock -> existingEntryAtLocation.update()
                else -> Unit
            }
        }
    }

    private fun checkAllBlocks() {
        if (!highlightCondition()) return
        synchronized(blocksLock) {
            blocksToHighlight.removeIf { !checkIsValid(it) || !it.extraCondition() }
        }
    }

    private fun checkIsValid(blockToCheck: T): Boolean {
        return blockCondition(blockToCheck.location.getBlockStateAt())
    }

    private fun drawHighlight(event: SkyHanniRenderWorldEvent) {
        if (!highlightCondition()) return
        synchronized(blocksLock) {
            if (blocksToHighlight.isEmpty()) return
            for (block in blocksToHighlight) {
                val aabb = block.location.boundingToOffset(1.0, 1.0, 1.0).expand(0.001)
                event.drawFilledBoundingBox(aabb, colorProvider(), renderRelativeToCamera = false)
            }
        }
    }

    init {
        @Suppress("UNCHECKED_CAST")
        blockHighlighters.add(this as SkyHanniBlockHighlighter<AbstractHighlightedBlock>)
    }

    @SkyHanniModule
    companion object {

        private val blockHighlighters = mutableListOf<SkyHanniBlockHighlighter<AbstractHighlightedBlock>>()

        @HandleEvent(priority = HandleEvent.HIGHEST)
        fun onTick() {
            blockHighlighters.forEach { it.checkAllBlocks() }
        }

        @HandleEvent
        fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
            blockHighlighters.forEach { it.drawHighlight(event) }
        }

        @HandleEvent
        fun onWorldChange() {
            blockHighlighters.forEach {
                synchronized(it.blocksLock) {
                    it.blocksToHighlight.clear()
                }
            }
        }
    }
}
