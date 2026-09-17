package potentialbugs

import SkyHanniRule
import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import potentialbugs.StorageNeedsExpose.Companion.CONFIG_PACKAGE
import potentialbugs.StorageNeedsExpose.Companion.STORAGE_PACKAGE
import utils.DetektUtils.hasAnnotation

class NonStorageDoesntNeedExpose(config: Config) : RequiresAnalysisApi, SkyHanniRule(
    config,
    "Config/storage properties that are not intended to store data should not be annotated with @Expose.",
) {
    override fun visitKtFile(file: KtFile) {
        val packageName = file.packageDirective?.fqName?.asString() ?: ""
        if (!packageName.startsWith(CONFIG_PACKAGE) && !packageName.startsWith(STORAGE_PACKAGE)) return
        super.visitKtFile(file)
    }

    override fun visitProperty(property: KtProperty) {
        if (property.hasAnnotation("Expose")) {
            // Reasons to NOT have Expose annotation:
            //  - The property is local
            //  - The property is private
            //  - The property has a getter
            //  - The property is annotated with ConfigEditorInfoText
            //  - The property is annotated with ConfigEditorButton and is a plain Runnable
            //    (a button can also be bound to a stored config object, which does need @Expose)
            //  - The property is annotated with Transient
            val hasExplicitGetter = property.getter?.hasBody() ?: false
            val isPlainButton = property.hasAnnotation("ConfigEditorButton") && analyze(property) {
                property.returnType.isClassType(ClassId.topLevel(FqName("java.lang.Runnable")))
            }
            val doWeCare = property.isLocal || property.isPrivate() || hasExplicitGetter ||
                property.hasAnnotation("ConfigEditorInfoText") ||
                isPlainButton ||
                property.hasAnnotation("Transient")

            if (doWeCare) {
                property.reportIssue("@Expose annotation is not needed on property ${property.name}")
            }
        }

        return super.visitProperty(property)
    }
}
