package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import kotlin.math.min

object CustomEquipmentWardrobe : AbstractCustomWardrobe(EquipmentWardrobeApi, "Equipment Sets") {

    private const val ITEM_SIZE = 16.0
    private const val ITEM_FILL = 0.8

    private const val VERTICAL_PADDING = 0.08

    override var onlyFavorites: Boolean
        get() = config.equipmentOnlyFavorites
        set(value) {
            config.equipmentOnlyFavorites = value
        }

    override fun isFeatureEnabled() = config.equipmentEnabled

    override fun jumpToConfig() {
        config::equipmentEnabled.jumpToEditor()
    }

    override fun createSlotContentRenderable(
        slot: WardrobeSlot,
        playerWidth: Double,
        containerHeight: Int,
        containerWidth: Int,
    ): Renderable {
        val padding = (containerHeight * VERTICAL_PADDING).toInt()
        val sectionHeights = itemSectionHeights(containerHeight - 2 * padding)
        val itemScale = min(containerWidth, sectionHeights.min()) / ITEM_SIZE * ITEM_FILL

        val sections = sectionHeights.mapIndexed { index, sectionHeight ->
            val background = Renderable.placeholder(containerWidth, sectionHeight)
            val stack = slot.armor.getOrNull(index) ?: return@mapIndexed background
            val item = Renderable.item(stack) {
                scale = itemScale
                horizontalAlign = HorizontalAlignment.CENTER
                verticalAlign = VerticalAlignment.CENTER
            }
            Renderable.doubleLayered(background, item, blockBottomHover = false, forceBottomRenderFirst = true)
        }
        return Renderable.vertical(spacing = 1) {
            add(Renderable.placeholder(containerWidth, padding))
            addAll(sections)
            add(Renderable.placeholder(containerWidth, padding))
        }
    }
}
