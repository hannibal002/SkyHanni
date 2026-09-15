package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.config.MoulConfigEditorComponent
import at.hannibal2.skyhanni.config.core.dependency.UsedByResolver
import at.hannibal2.skyhanni.features.pets.PetDisplayConfigGuiManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.lang.reflect.Field
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.javaField

object ConfigUtils {

    private const val UNKNOWN_EDITOR_INDEX = -1

    private val editorProviders = listOf<() -> MoulConfigEditor<*>>(
        ConfigGuiManager::getEditorInstance,
        PetDisplayConfigGuiManager::getEditorInstance,
    )
    private val editorIndexCache = mutableMapOf<Field, Int>()

    // moulconfig only consults the SearchFunction when the search text is non-blank, so the
    // filtered config view keeps a sentinel in the search box; typing clears it and falls back
    // to normal text search, clearing the box shows everything again.
    private const val FILTER_SENTINEL = "###"
    private var activeSearchFilter: Set<String>? = null

    /**
     * Open the config GUI filtered to only show the given config option keys
     * (see [UsedByResolver.transitiveUsedBy]).
     */
    fun openFilteredConfig(keys: Set<String>) {
        val editor = ConfigGuiManager.getEditorInstance()
        activeSearchFilter = keys.takeIf { it.isNotEmpty() }
        if (activeSearchFilter == null) {
            // moulconfig crashes when the search function is null and a query is entered,
            // so restore the default (plain text search) instead
            editor.setSearchFunction { optionEditor, query -> optionEditor.fulfillsSearch(query) }
            editor.search("")
        } else {
            editor.setSearchFunction { optionEditor, query ->
                if (query != FILTER_SENTINEL) {
                    optionEditor.fulfillsSearch(query)
                } else {
                    (optionEditor.getOption() as? ProcessedOption.HasField)?.field
                        ?.let { "${it.declaringClass.name}#${it.name}" in keys }
                        ?: false
                }
            }
            editor.search(FILTER_SENTINEL)
        }
        openEditor(editor, reuseOpenScreen = true)
    }

    /** Removes any active search filter so the full config is visible again. */
    fun clearSearchFilter() {
        activeSearchFilter = null
        ConfigGuiManager.getEditorInstance().apply {
            setSearchFunction { optionEditor, query -> optionEditor.fulfillsSearch(query) }
            search("")
        }
    }

    private fun MoulConfigEditor<*>.searchForJump(field: Field?) {
        val filter = activeSearchFilter
        if (filter == null) {
            search("")
            return
        }
        // jumps to an option inside the filter keep the filter, jumps outside exit it
        if (field != null && "${field.declaringClass.name}#${field.name}" in filter) {
            search(FILTER_SENTINEL)
        } else {
            clearSearchFilter()
        }
    }

    /**
     * Migrates a Boolean to an Enum Constant.
     *
     * @param element The JsonElement to migrate
     * @param trueValue The enum value it should map to if the value is true
     * @param falseValue The enum value it should map to if the value is false
     * @return The migrated JsonElement
     */
    fun <T : Enum<T>> migrateBooleanToEnum(element: JsonElement, trueValue: T, falseValue: T): JsonElement {
        require(element is JsonPrimitive) { "Expected a JsonPrimitive but got ${element.javaClass.simpleName}" }
        return JsonPrimitive(if (element.asBoolean) trueValue.name else falseValue.name)
    }

    private fun KProperty0<*>.tryFindEditor(editor: MoulConfigEditor<*>): ProcessedOption? {
        // Java reflection is used because MoulConfig is relocated at build time, causing Kotlin reflection
        // (this.javaField) to fail to resolve property descriptors in the production build.
        val receiver = (this as? CallableReference)?.boundReceiver
            ?.takeIf { it !== CallableReference.NO_RECEIVER }
            ?: return null
        val field = generateSequence(receiver.javaClass as Class<*>?) { it.superclass }
            .firstNotNullOfOrNull { clazz ->
                runCatching { clazz.getDeclaredField(name) }.getOrNull()
            } ?: return null
        return editor.getOptionFromField(field)
    }

    fun KProperty0<*>.jumpToEditor() {
        if (tryJumpToEditor(ConfigGuiManager.getEditorInstance())) return

        ErrorManager.crashInDevEnv("Can not open config $name")
        ErrorManager.logErrorStateWithData(
            "Can not open the config",
            "error while trying to jump to an editor element",
            "this.name" to this.name,
            "this.toString()" to this.toString(),
            "this" to this,
        )
    }

    private fun KProperty0<*>.tryJumpToEditor(editor: MoulConfigEditor<*>): Boolean {
        val option = tryFindEditor(editor) ?: return false
        return editor.jumpToOption(option)
    }

    fun canJumpToEditor(field: Field): Boolean =
        field.findEditorIndex() != UNKNOWN_EDITOR_INDEX

    fun jumpToEditor(field: Field): Boolean {
        val editor = field.findEditor() ?: return false
        val option = editor.getOptionFromField(field) ?: return false
        return editor.jumpToOption(option)
    }

    fun clearEditorCache() {
        editorIndexCache.clear()
    }

    private fun Field.findEditor(): MoulConfigEditor<*>? =
        editorProviders.getOrNull(findEditorIndex())?.invoke()

    private fun Field.findEditorIndex(): Int = editorIndexCache.getOrPut(this) {
        editorProviders.indexOfFirst { editorProvider ->
            editorProvider().getOptionFromField(this) != null
        }
    }

    private fun MoulConfigEditor<*>.jumpToOption(option: ProcessedOption): Boolean {
        searchForJump((option as? ProcessedOption.HasField)?.field)
        if (!goToOptionPreservingScroll(option)) return false
        openEditor(this)
        return true
    }

    /**
     * Like MoulConfigEditor.goToOption, but without resetting the options scroll to the top when the
     * option is in the already-selected category. MoulConfigEditor.goToOption always calls
     * setSelectedCategory, which resets the scroll to 0, so the jump animation always starts at the
     * top and scrolls down. Preserving the current scroll lets the animation start from the current
     * position and scroll up when the target is above the current viewport.
     */
    private fun MoulConfigEditor<*>.goToOptionPreservingScroll(option: ProcessedOption): Boolean {
        val category = option.getCategory()
        if (getSelectedCategory() != category.getIdentifier()) {
            if (!setSelectedCategory(category)) {
                search("")
                if (!setSelectedCategory(category)) return false
            }
        }
        if (!scrollOptionIntoView(option, 200)) {
            search("")
            if (!scrollOptionIntoView(option, 200)) return false
        }
        return true
    }

    fun openEditor(editor: MoulConfigEditor<*>, reuseOpenScreen: Boolean = false) {
        // When the config screen is already open it wraps the same cached editor instance,
        // so the live screen scrolls to the target itself - recreating it would restart the
        // scroll animation from the top and always scroll down.
        if (reuseOpenScreen && MinecraftCompat.screen is MoulConfigScreenComponent) return
        SkyHanniMod.screenToOpen = createConfigScreen(editor)
    }

    internal fun createConfigScreen(editor: MoulConfigEditor<*>, previousScreen: Screen? = null) =
        MoulConfigScreenComponent(Component.empty(), GuiContext(MoulConfigEditorComponent(editor)), previousScreen)

    /**
     * Open the config editor and navigate to the given java field declared on [owner].
     * This is useful when a KMutableProperty1 belongs to a non-singleton class and we can't safely
     * set the value by creating a new instance.
     */
    fun openEditorForField(owner: Class<*>, fieldName: String) {
        val editor = ConfigGuiManager.getEditorInstance()
        val field = runCatching { owner.getDeclaredField(fieldName) }.getOrNull() ?: return
        field.isAccessible = true
        val option = editor.getOptionFromField(field) ?: return
        ConfigJumpHighlight.highlight(field)
        editor.searchForJump(field)
        if (!editor.goToOptionPreservingScroll(option)) return
        openEditor(editor, reuseOpenScreen = true)
    }

    /**
     * Bind a mutable Kotlin property to a receiver instance and open its editor option if available.
     * Useful for object singletons where we can obtain a bound property reference.
     */
    fun <T> KMutableProperty1<T, *>.jumpToEditor(receiver: T) {
        val editor = ConfigGuiManager.getEditorInstance()
        val field = this.javaField ?: return
        val option = editor.getOptionFromField(field) ?: return
        ConfigJumpHighlight.highlight(field)
        editor.searchForJump(field)
        if (!editor.goToOptionPreservingScroll(option)) return
        openEditor(editor, reuseOpenScreen = true)
    }

    val configScreenCurrentlyOpen: Boolean
        get() = MinecraftCompat.screen is MoulConfigScreenComponent

    fun String.asStructuredText() = StructuredText.of(this)
}

/**
 * Tracks a config option that the user just jumped to from elsewhere in the config,
 * so the target option row can blink yellow briefly after the jump.
 */
object ConfigJumpHighlight {
    private var target: String? = null
    private var until = 0L
    const val DURATION_MS = 3000L

    fun highlight(field: Field) {
        target = "${field.declaringClass.name}#${field.name}"
        until = System.currentTimeMillis() + DURATION_MS
    }

    fun isActive(ownerName: String, fieldName: String): Boolean = remainingMs(ownerName, fieldName) > 0

    /** Milliseconds left of the highlight, or 0 when inactive/expired. */
    fun remainingMs(ownerName: String, fieldName: String): Long {
        if (target != "$ownerName#$fieldName") return 0
        val remaining = until - System.currentTimeMillis()
        if (remaining <= 0) {
            target = null
            return 0
        }
        return remaining
    }
}
