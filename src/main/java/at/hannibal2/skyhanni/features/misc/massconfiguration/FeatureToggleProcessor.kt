package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.config.FeatureToggle
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean
import io.github.moulberry.moulconfig.annotations.ConfigOption
import io.github.moulberry.moulconfig.processor.ConfigStructureReader
import java.lang.reflect.Field
import java.util.*

class FeatureToggleProcessor : ConfigStructureReader {

    var latestCategory: Category? = null
    val pathStack = Stack<String>()

    val allOptions = mutableListOf<FeatureToggleableOption>()
    val orderedOptions by lazy {
        allOptions.groupBy { it.category }
    }

    override fun beginCategory(baseObject: Any?, field: Field?, name: String, description: String) {
        latestCategory = Category(name, description)
    }

    override fun endCategory() {
    }

    override fun beginAccordion(baseObject: Any?, field: Field?, option: ConfigOption?, id: Int) {
    }

    override fun endAccordion() {
    }

    override fun pushPath(fieldPath: String) {
        pathStack.push(fieldPath)
    }

    override fun popPath() {
        pathStack.pop()
    }

    override fun emitOption(baseObject: Any, field: Field, option: ConfigOption) {
        val featureToggle = field.getAnnotation(FeatureToggle::class.java) ?: return
        field.getAnnotation(ConfigEditorBoolean::class.java)
            ?: error("Feature toggle found without ConfigEditorBoolean: $field")
        allOptions.add(
            FeatureToggleableOption(
                option.name,
                option.desc,
                field.getBoolean(baseObject),
                featureToggle.trueIsEnabled,
                latestCategory!!,
                { field.setBoolean(baseObject, it) },
                pathStack.joinToString(".") + "." + field.name
            )
        )
    }

    override fun emitGuiOverlay(baseObject: Any?, field: Field?, option: ConfigOption?) {
    }
}