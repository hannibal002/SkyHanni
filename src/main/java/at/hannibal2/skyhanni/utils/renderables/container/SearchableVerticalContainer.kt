package at.hannibal2.hanni.utils.renderables.container

import at.hannibal2.hanni.data.model.TextInput
import at.hannibal2.hanni.utils.RenderUtils
import at.hannibal2.hanni.utils.renderables.Renderable

// TODO
class SearchableVerticalContainer(
    private val content: Map<Renderable, String?>,
    spacing: Int = 0,
    private val textInput: TextInput,
    key: Int,
    horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
    verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.TOP,
) : VerticalContainerRenderable(
    content.map { it.key },
    spacing,
    horizontalAlign,
    verticalAlign,
) {
    init {
        textInput.registerToEvent(key) {
            // null = ignored, never filtered
            renderables = Renderable.filterList(content, textInput.textBox)
        }
    }
}
