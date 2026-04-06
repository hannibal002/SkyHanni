package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.compat.SkyHanniScreenTheme
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text

/**
 * A bordered card with a title label above a body Renderable.
 *
 * @param title The card title; may contain Minecraft color codes.
 * @param body The content to display inside the card.
 * @param width The fixed total width of the card.
 */
fun Renderable.Companion.labeledCard(
    title: String,
    body: Renderable,
    width: Int,
): Renderable {
    val titleText = Renderable.fixedSizeLine(Renderable.text(title), width)
    val paddedBody = Renderable.fixedSizeLine(body, width)
    val inner = Renderable.vertical(listOf(titleText, paddedBody), spacing = 4)
    return Renderable.drawInsideRoundedRect(
        inner,
        color = SkyHanniScreenTheme.COLOR_ROW_NORMAL,
        padding = 6,
        radius = 5,
    )
}
