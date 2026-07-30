package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.getTooltip
import at.hannibal2.skyhanni.utils.compat.getTooltipCompat
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import java.awt.Color

object CustomRenderUtils {

    fun createLabeledButton(
        text: String,
        hoveredColor: Color = Color(130, 130, 130, 200),
        unhoveredColor: Color = hoveredColor.darker(0.57),
        onClick: () -> Unit,
        width: Int,
        height: Int,
        topBorderColor: ChromaColour,
        bottomBorderColor: ChromaColour,
        scale: Int,
    ): Renderable {
        val buttonWidth = (width * (scale / 100.0)).toInt()
        val buttonHeight = (height * (scale / 100.0)).toInt()
        val textScale = (scale / 100.0)

        val renderable = Renderable.hoverable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.doubleLayered(
                    Renderable.clickable(
                        Renderable.placeholder(buttonWidth, buttonHeight),
                        onClick,
                    ),
                    centerString(text, scale = textScale),
                    false,
                ),
                hoveredColor,
                padding = 0,
                topOutlineColor = topBorderColor.toColor().rgb,
                bottomOutlineColor = bottomBorderColor.toColor().rgb,
                borderOutlineThickness = 2,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            ),
            Renderable.drawInsideRoundedRect(
                Renderable.doubleLayered(
                    Renderable.placeholder(buttonWidth, buttonHeight),
                    centerString(text, scale = textScale),
                ),
                unhoveredColor.darker(0.57),
                padding = 0,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            ),
        )

        return renderable
    }

    fun createHoverableRenderable(
        hoveredRenderable: Renderable,
        unhoveredRenderable: Renderable = Renderable.placeholder(hoveredRenderable.width, hoveredRenderable.height),
        topLayerRenderable: Renderable = Renderable.placeholder(0, 0),
        padding: Int = 0,
        horizontalAlignment: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.CENTER,
        verticalAlignment: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
        hoveredColor: Color,
        unHoveredColor: Color = hoveredColor,
        borderOutlineThickness: Int,
        borderOutlineBlur: Float = 0.5f,
        onClick: () -> Unit,
        onHover: () -> Unit = {},
        topOutlineColor: Color,
        bottomOutlineColor: Color,
    ): Renderable =
        Renderable.hoverable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.doubleLayered(
                    Renderable.clickable(
                        hoveredRenderable,
                        onClick,
                    ),
                    topLayerRenderable,
                ),
                hoveredColor,
                padding = padding,
                topOutlineColor = topOutlineColor.rgb,
                bottomOutlineColor = bottomOutlineColor.rgb,
                borderOutlineThickness = borderOutlineThickness,
                blur = borderOutlineBlur,
                horizontalAlign = horizontalAlignment,
                verticalAlign = verticalAlignment,
            ),
            Renderable.drawInsideRoundedRect(
                unhoveredRenderable,
                unHoveredColor,
                padding = padding,
                horizontalAlign = horizontalAlignment,
                verticalAlign = verticalAlignment,
            ),
            onHover = { onHover() },
        )

    fun createFakePlayerRenderable(
        armor: List<SafeItemStack?>,
        inPage: Boolean,
        playerWidth: Double,
        containerHeight: Int,
        containerWidth: Int,
        eyesFollowMouse: Boolean = true,
    ): Renderable {
        val fakePlayer = FakePlayer.fromLocalPlayerOrThrow()
        var scale = playerWidth

        for (equipment in Inventory.EQUIPMENT_SLOT_MAPPING.values) {
            val armorOrdinal = equipment.ordinal - 2
            if (armorOrdinal !in 0..3) continue
            var stack = armor.reversed()[armorOrdinal]?.copy()?.removeEnchants()
            if (stack == null) stack = SafeItemStack.EMPTY
            fakePlayer.equipment.set(equipment, stack)
        }

        val playerColor = if (!inPage) {
            scale *= 0.9
            Color.GRAY.addAlpha(100)
        } else null

        return Renderable.fakePlayer(
            fakePlayer,
            followMouse = eyesFollowMouse,
            width = containerWidth,
            height = containerHeight,
            entityScale = scale.toInt(),
            padding = 0,
            color = playerColor,
        )
    }

    // TODO don't initialize all 18 slots at once, load them lazily when first time hovering over the item.
    fun createArmorTooltipRenderable(
        armor: List<SafeItemStack?>,
        containerHeight: Int,
        containerWidth: Int,
        tooltipKeybind: Int,
    ): Renderable {
        val loreList = mutableListOf<Renderable>()
        val height = containerHeight - 3

        // This is needed to keep the background size the same as the player renderable size
        val hoverableSizes = MutableList(4) { height / 4 }.apply {
            for (k in 0 until height % 4) this[k]++
        }

        for (armorIndex in 0 until 4) {
            val stack = armor.getOrNull(armorIndex)?.copy()
            var renderable = Renderable.placeholder(containerWidth, hoverableSizes[armorIndex])
            if (stack != null) {
                val toolTip = getToolTip(stack)
                if (toolTip != null) {
                    renderable = Renderable.hoverTips(
                        renderable,
                        tips = toolTip,
                        stack = stack,
                        condition = {
                            tooltipKeybind.isKeyHeld()
                        },
                        onHover = {
                            if (EstimatedItemValue.config.enabled) EstimatedItemValue.updateItem(stack)
                        },
                    )
                }
            }
            loreList.add(renderable)
        }
        return Renderable.vertical(loreList, spacing = 1)
    }

    private fun getToolTip(stack: SafeItemStack): List<Component>? {
        try {
            // Get tooltip from minecraft and other mods
            val toolTips = stack.getTooltip(Minecraft.getInstance().options.advancedItemTooltips)

            return toolTips
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Failed to get tooltip for armor piece in CustomWardrobe",
                "Armor" to stack,
                "Lore" to stack.getTooltipCompat(false),
            )
            return null
        }
    }

    fun addGuiBackground(
        renderable: Renderable,
        borderPadding: Int,
        scale: Int,
        backgroundColor: ChromaColour,
        onLeftClick: () -> Unit,
    ) =
        Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                renderable,
                Renderable.clickable(
                    Renderable.text(
                        "§7SkyHanni",
                        horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT,
                        verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
                        scale = 1.0 * (scale / 100.0),
                    ).let { Renderable.hoverable(hovered = Renderable.underlined(it), unHovered = it) },
                    onLeftClick = onLeftClick
                ),
                blockBottomHover = false,
            ),
            backgroundColor.toColor(),
            padding = borderPadding,
        )


    fun centerString(
        text: String,
        scale: Double = 1.0,
        color: Color = Color.WHITE,
    ) = Renderable.text(text, scale, color, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)
}
