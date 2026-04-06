package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.mixins.hooks.VisualWordsHook
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import org.lwjgl.glfw.GLFW

/**
 * Standalone screen for the Visual Words editor.
 *
 * All mutable UI state lives here.
 * All Renderable construction is delegated to [VisualWordGui].
 */
class VisualWordScreen : SkyHanniBaseScreen() {

    val phraseInput = TextInput()
    val replacementInput = TextInput()
    val listScrollValue = ScrollValue()

    private var display: Renderable? = null
    var currentlyEditing = false
    var currentIndex = -1
    var activeInput: TextInput? = null
    var modifiedWords: MutableList<VisualWord> = ModifyVisualWords.userModifiedWords
        .map { it.toVisualWord() }.toMutableList()

    /**
     * Rebuilds the Renderable tree. Call on structural changes such as mode switches,
     * add/delete/reorder, and flag toggles. Do not call while the user is actively typing.
     */
    fun rebuildDisplay() {
        display = VisualWordGui.buildDisplay(this)
    }

    override fun onInitGui() = rebuildDisplay()
    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        val renderable = display ?: return
        val startX = (width - renderable.width) / 2
        val startY = (height - renderable.height) / 2

        VisualWordsHook.withoutWordChanges {
            DrawContextUtils.pushPop {
                DrawContextUtils.translate(startX.toFloat(), startY.toFloat())
                Renderable.withMousePosition(mouseX - startX, mouseY - startY) {
                    renderable.render(0, 0)
                }
            }
        }
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) = keyCode?.let {
        when {
            keyCode == GLFW.GLFW_KEY_ESCAPE || KeyboardManager.checkIsInventoryClosure(keyCode) ->
                if (currentlyEditing) exitEditMode() else onClose()

            keyCode == GLFW.GLFW_KEY_TAB && currentlyEditing ->
                activeInput = if (activeInput === phraseInput) replacementInput else phraseInput

            (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && currentlyEditing ->
                exitEditMode()
        }
    } ?: Unit

    override fun isPauseScreen() = false

    fun enterEditMode(index: Int) {
        currentlyEditing = true
        currentIndex = index
        val word = modifiedWords[index]
        phraseInput.textBox = word.phrase
        replacementInput.textBox = word.replacement

        var lastPhrase = word.phrase
        var lastReplacement = word.replacement

        phraseInput.registerToEvent(PHRASE_KEY) {
            val new = phraseInput.textBox
            if (new == lastPhrase) return@registerToEvent
            lastPhrase = new
            modifiedWords.getOrNull(currentIndex)?.phrase = new
            saveChanges()
        }
        replacementInput.registerToEvent(REPLACEMENT_KEY) {
            val new = replacementInput.textBox
            if (new == lastReplacement) return@registerToEvent
            lastReplacement = new
            modifiedWords.getOrNull(currentIndex)?.replacement = new
            saveChanges()
        }

        activeInput = phraseInput
        rebuildDisplay()
    }

    fun exitEditMode() {
        val word = modifiedWords.getOrNull(currentIndex)
        if (word != null && word.phrase.isEmpty() && word.replacement.isEmpty()) {
            modifiedWords.removeAt(currentIndex)
        }
        currentlyEditing = false
        currentIndex = -1
        activeInput = null
        saveChanges()
        rebuildDisplay()
    }

    fun addNewWord() {
        modifiedWords.add(VisualWord("", "", enabled = true, caseSensitive = false))
        saveChanges()
        enterEditMode(modifiedWords.lastIndex)
    }

    fun deleteWord(index: Int) {
        modifiedWords.removeAt(index)
        currentlyEditing = false
        currentIndex = -1
        activeInput = null
        saveChanges()
        rebuildDisplay()
    }

    fun moveWord(index: Int, up: Boolean) {
        val target = if (up) index - 1 else index + 1
        if (target !in modifiedWords.indices) return
        val temp = modifiedWords[index]
        modifiedWords[index] = modifiedWords[target]
        modifiedWords[target] = temp
        saveChanges()
        rebuildDisplay()
    }

    fun toggleEnabled(index: Int) {
        modifiedWords.getOrNull(index)?.let { it.enabled = !it.enabled }
        saveChanges()
        rebuildDisplay()
    }

    fun toggleCaseSensitive(index: Int) {
        modifiedWords.getOrNull(index)?.let { it.setCaseSensitive(!it.isCaseSensitive()) }
        saveChanges()
        rebuildDisplay()
    }

    fun saveChanges() {
        ModifyVisualWords.userModifiedWords = modifiedWords
            .map { VisualWordText.fromVisualWord(it) }.toMutableList()
        ModifyVisualWords.update()
        SkyHanniMod.configManager.saveConfig(ConfigFileType.VISUAL_WORDS, "Updated visual words")
    }

    companion object {
        private const val PHRASE_KEY = 1
        private const val REPLACEMENT_KEY = 2
    }
}
