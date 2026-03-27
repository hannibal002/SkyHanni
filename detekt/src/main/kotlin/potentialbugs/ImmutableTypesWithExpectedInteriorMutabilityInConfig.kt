package potentialbugs

import SkyHanniRule
import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty

class ImmutableTypesWithExpectedInteriorMutabilityInConfig(config: Config) : RequiresAnalysisApi, SkyHanniRule(
    config,
    "Disallow using types which may disallow MoulConfig from mutating them, due to the lack of interior mutability",
) {
    companion object {
        val configPackage = FqName("at.hannibal2.skyhanni.config")

        private val propertyClassId = ClassId(
            FqName("io.github.notenoughupdates.moulconfig.observer"),
            FqName("Property"),
            false,
        )
        private val immutableCollectionClassIds = setOf(
            ClassId(FqName("kotlin.collections"), FqName("Collection"), false),
            ClassId(FqName("kotlin.collections"), FqName("Map"), false),
            ClassId(FqName("java.util"), FqName("Collection"), false),
            ClassId(FqName("java.util"), FqName("Map"), false),
        )
        private val mutableCollectionClassIds = setOf(
            ClassId(FqName("kotlin.collections"), FqName("MutableCollection"), false),
            ClassId(FqName("kotlin.collections"), FqName("MutableMap"), false),
            ClassId(FqName("java.util"), FqName("EnumMap"), false),
            ClassId(FqName("java.util"), FqName("EnumSet"), false),
        )
    }

    override fun visit(root: KtFile) {
        if (!root.packageFqName.startsWith(configPackage)) return
        super.visit(root)
    }

    private fun KtAnnotated.hasConfigOptionAnnotation(): Boolean {
        return annotationEntries.any { it.shortName?.asString() == "ConfigOption" }
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (!property.hasConfigOptionAnnotation()) return

        analyze(property) {
            val allClassIds = resolveAllClassIds(property.returnType)

            val immutableParentTypes = allClassIds.intersect(immutableCollectionClassIds)
            if (immutableParentTypes.isEmpty()) return@analyze

            val mutableParentTypes = allClassIds.intersect(mutableCollectionClassIds)
            if (mutableParentTypes.isNotEmpty()) return@analyze

            property.reportIssue("A @ConfigOption field must use types with interior mutability: $immutableParentTypes")
        }
    }

    private fun KaSession.resolveAllClassIds(type: KaType, visited: MutableSet<KaType> = mutableSetOf()): Set<ClassId> {
        if (!visited.add(type)) return emptySet()
        val classType = type as? KaClassType ?: return emptySet()
        val result = mutableSetOf(classType.classId)

        // Unwrap MoulConfig's Property<T> wrapper to check the inner type
        if (classType.classId == propertyClassId) {
            val innerType = (classType.typeArguments.firstOrNull() as? KaTypeArgumentWithVariance)?.type
            if (innerType != null) {
                result.addAll(resolveAllClassIds(innerType, visited))
            }
        }

        for (supertype in type.directSupertypes) {
            result.addAll(resolveAllClassIds(supertype, visited))
        }
        return result
    }
}
