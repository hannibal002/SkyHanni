package formatting

import SkyHanniRule
import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import potentialbugs.StorageNeedsExpose.Companion.CONFIG_PACKAGE
import utils.DetektUtils.doWeNeedToCheckConfigProp

class StorageVarOrVal(config: Config) : RequiresAnalysisApi, SkyHanniRule(
    config,
    "Storage and config variables should be declared as `var` for primitives and `val` for objects.",
) {
    private enum class StorageType { VAR, VAL }

    companion object {
        private val CHROMA_COLOUR_CLASS_ID = ClassId(
            FqName("io.github.notenoughupdates.moulconfig"),
            FqName("ChromaColour"),
            false,
        )
        private val PRIMITIVE_CLASS_IDS = setOf(
            StandardClassIds.Boolean,
            StandardClassIds.Byte,
            StandardClassIds.Char,
            StandardClassIds.Double,
            StandardClassIds.Float,
            StandardClassIds.Int,
            StandardClassIds.Long,
            StandardClassIds.Short,
        )
    }

    override fun visitKtFile(file: KtFile) {
        val packageName = file.packageDirective?.fqName?.asString() ?: ""
        if (!packageName.startsWith(CONFIG_PACKAGE)) return
        super.visitKtFile(file)
    }

    override fun visitProperty(property: KtProperty) {
        if (!property.doWeNeedToCheckConfigProp()) return

        val expected = if (shouldBeVar(property)) StorageType.VAR else StorageType.VAL
        val actual = if (property.isVar) StorageType.VAR else StorageType.VAL

        if (actual != expected) return property.reportIssue(
            "${property.typeReference?.text} `${property.name}` should be a ${expected.name.lowercase()}",
        )

        super.visitProperty(property)
    }

    private fun shouldBeVar(property: KtProperty): Boolean {
        val annotations = property.annotationEntries.mapNotNull { it.shortName?.asString() }
        val isInfoText = "ConfigEditorInfoText" in annotations
        val isKeyMapping = "ConfigEditorKeyMapping" in annotations

        return analyze(property) {
            val type = property.returnType
            val classId = (type as? KaClassType)?.classId

            // Property<T> should always be val.
            if (classId?.shortClassName?.asString() == "Property") {
                return@analyze false
            }

            isKeyMapping || (
                !isInfoText && (
                    classId in PRIMITIVE_CLASS_IDS ||
                        classId == StandardClassIds.String ||
                        type.expandedSymbol?.classKind == KaClassKind.ENUM_CLASS ||
                        classId == CHROMA_COLOUR_CLASS_ID
                    )
                )
        }
    }
}
