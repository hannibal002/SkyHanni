package at.hannibal2.hanni.utils.blockhighlight

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.hanni.utils.expand
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.block.state.IBlockState

/**
 * A [HanniBlockHighlighter] is used to highlight blocks based on a certain condition
 *
 * @property highlightCondition A condition for when the highlighter should be running its code such as an isEnabled function.
 * @property blockCondition A condition that the blockstate at a location must fulfill to be highlighted.
 * @property colorProvider Provides the color that the highlighter will use when rendering the highlighted block.
 */
class HanniBlockHighlighter<T : AbstractHighlightedBlock>(
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

    private fun drawHighlight(event: HanniRenderWorldEvent) {
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
        blockHighlighters.add(this as HanniBlockHighlighter<AbstractHighlightedBlock>)
    }

    @HanniModule
    companion object {

        private val blockHighlighters = mutableListOf<HanniBlockHighlighter<AbstractHighlightedBlock>>()

        @HandleEvent(priority = HandleEvent.HIGHEST)
        fun onTick() {
            blockHighlighters.forEach { it.checkAllBlocks() }
        }

        @HandleEvent
        fun onRenderWorld(event: HanniRenderWorldEvent) {
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
