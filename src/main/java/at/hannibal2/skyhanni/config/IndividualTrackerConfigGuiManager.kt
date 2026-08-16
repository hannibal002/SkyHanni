package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.GenericIndividualTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.IndividualTrackerSettingsConfig
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConfigUtils
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorAccordion
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor

@SkyHanniModule
object IndividualTrackerConfigGuiManager {
    private const val FALLBACK_TITLE = "Individual Tracker Settings"

    private val widenConfig get() = SkyHanniMod.feature.gui.widenConfig

    private val editors =
        mutableMapOf<GenericIndividualTrackerConfig<*>, MoulConfigEditor<IndividualTrackerSettingsConfig>>()

    fun open(tracker: GenericIndividualTrackerConfig<*>) {
        ConfigUtils.openEditor(getEditorInstance(tracker))
    }

    private fun getEditorInstance(tracker: GenericIndividualTrackerConfig<*>) = editors.getOrPut(tracker) {
        val config = IndividualTrackerSettingsConfig(findTitle(tracker), tracker.trackerConfig)
        val processor = MoulConfigProcessor(config)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(config)
        MoulConfigEditor(processor).apply {
            wide = widenConfig.get()
        }
    }

    // All trackers share the same settings classes, so the only thing naming the tracker being edited
    // is the accordion the settings sit in, for example "Rare Crop Tracker" for the rare crop tracker.
    private fun findTitle(tracker: GenericIndividualTrackerConfig<*>): String {
        return findEnclosingName(tracker) ?: FALLBACK_TITLE
    }

    private fun findEnclosingName(tracker: GenericIndividualTrackerConfig<*>): String? {
        val editor = ConfigGuiManager.getEditorInstance()
        val option = editor.allOptions.firstOrNull { it.get() === tracker } ?: return null
        // Trackers that are not nested in an accordion name themselves, for example "Stray Tracker Settings"
        val enclosing = option.category.options.firstOrNull {
            (it.editor as? GuiOptionEditorAccordion)?.accordionId == option.accordionId
        }
        return (enclosing ?: option).name.text
    }

    fun invalidate() {
        editors.clear()
    }

    @HandleEvent
    private fun onConfigLoad() {
        invalidate()
        ConditionalUtils.onToggle(widenConfig) {
            editors.values.forEach { it.wide = widenConfig.get() }
        }
    }
}
