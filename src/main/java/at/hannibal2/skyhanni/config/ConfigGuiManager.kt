package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConfigUtils
import com.mojang.brigadier.suggestion.SuggestionProvider
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.ConfigStructureReader
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import java.lang.reflect.Field
import java.util.IdentityHashMap

@SkyHanniModule
object ConfigGuiManager {

    private const val MAX_CONFIG_TAB_COMPLETE_SUGGESTIONS = 200

    private val widenConfig get() = SkyHanniMod.feature.gui.widenConfig

    /** The names of all top-level config categories, extracted from the [Category] annotations of [SkyHanniConfig]. */
    val categoryNames: List<String> by lazy {
        SkyHanniConfig::class.java.declaredFields
            .mapNotNull { it.getAnnotation(Category::class.java)?.name }
            .toList()
    }

    /** All config option paths mapped to their fields, extracted by processing the config with a [ConfigStructureReader]. */
    val optionPathToField: Map<String, Field> by lazy {
        collectOptionPaths(SkyHanniMod.feature)
    }

    /** All config option paths mapped to their processed options, resolved through the config editor. */
    val optionPathToOption: Map<String, ProcessedOption> by lazy {
        val editor = getEditorInstance()
        optionPathToField.mapNotNull { (path, field) ->
            editor.getOptionFromField(field)?.let { path to it }
        }.toMap()
    }

    /**
     * Checks whether a config option matches a /sh search, reusing MoulConfig's own matching
     * ([GuiOptionEditor.fulfillsSearch]) in addition to matching against the option's path.
     * Mirrors the config editor's search semantics: trimmed and case-insensitive, with
     * `+`-separated terms that all have to match.
     */
    internal fun configOptionMatchesSearch(input: String, path: String, option: ProcessedOption): Boolean =
        searchTerms(input).all { term ->
            path.contains(term) || option.getEditor().fulfillsSearch(term)
        }

    /** Checks whether a config category name matches a /sh search with the same semantics as [configOptionMatchesSearch]. */
    internal fun categoryMatchesSearch(categoryName: String, input: String): Boolean =
        searchTerms(input).all { term -> categoryName.lowercase().contains(term) }

    private fun searchTerms(input: String): List<String> {
        val search = input.trim().lowercase()
        return if (search.isEmpty()) emptyList() else search.split("+")
    }

    /** Suggestions for the /sh search: category names and option paths matching MoulConfig's search. */
    val configTabCompleteSuggestionProvider: SuggestionProvider<FabricClientCommandSource> by lazy {
        SuggestionProvider { _, builder ->
            val input = builder.remainingLowerCase
            var count = 0
            for (category in categoryNames) {
                if (count >= MAX_CONFIG_TAB_COMPLETE_SUGGESTIONS) break
                if (categoryMatchesSearch(category, input)) {
                    builder.suggest(category)
                    count++
                }
            }
            for ((path, option) in optionPathToOption) {
                if (count >= MAX_CONFIG_TAB_COMPLETE_SUGGESTIONS) break
                if (configOptionMatchesSearch(input, path, option)) {
                    builder.suggest(path)
                    count++
                }
            }
            builder.buildFuture()
        }
    }

    internal fun collectOptionPaths(config: SkyHanniConfig): Map<String, Field> {
        val reader = OptionPathReader()
        val driver = ConfigProcessorDriver(reader)
        driver.warnForPrivateFields = false
        driver.processConfig(config)
        return reader.options
    }

    fun openConfigOption(field: Field) {
        val editor = getEditorInstance()
        val option = editor.getOptionFromField(field)
        if (option == null || !editor.goToOption(option)) {
            editor.search("")
        }
        ConfigUtils.openEditor(editor)
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        getEditorInstance().wide = widenConfig.get()
        ConditionalUtils.onToggle(widenConfig) {
            getEditorInstance().wide = widenConfig.get()
        }
    }

    var editor: MoulConfigEditor<SkyHanniConfig>? = null

    fun getEditorInstance() = editor ?: MoulConfigEditor(SkyHanniMod.configManager.processor).also { editor = it }

    fun openConfigGui(search: String? = null) {
        val editor = getEditorInstance()

        if (search != null) {
            editor.search(search)
        }
        ConfigUtils.openEditor(editor)
    }

    private class OptionPathReader : ConfigStructureReader {
        val options = mutableMapOf<String, Field>()
        private val instancePath = IdentityHashMap<Any, String>()

        override fun beginConfig(configClass: Class<out Config>, driver: ConfigProcessorDriver, config: Config) {
            instancePath[config] = ""
        }

        override fun beginCategory(baseObject: Any?, field: Field?, name: String, description: String) {
            if (field != null) mapChildInstance(baseObject, field)
        }

        override fun endCategory() = Unit

        override fun beginAccordion(baseObject: Any?, field: Field?, option: ConfigOption?, id: Int) {
            if (field != null) mapChildInstance(baseObject, field)
        }

        override fun endAccordion() = Unit

        private fun mapChildInstance(baseObject: Any?, field: Field) {
            val parent = baseObject ?: return
            val parentPath = instancePath[parent] ?: return
            val child = field.get(parent) ?: return
            instancePath[child] = if (parentPath.isEmpty()) field.name else "$parentPath.${field.name}"
        }

        override fun emitOption(baseObject: Any, field: Field, option: ConfigOption) {
            val path = instancePath[baseObject]
            options[if (path.isNullOrEmpty()) field.name else "$path.${field.name}"] = field
        }
    }
}
