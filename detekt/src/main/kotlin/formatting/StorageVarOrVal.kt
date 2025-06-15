package at.hannibal2.skyhanni.detektrules.formatting

import at.hannibal2.skyhanni.detektrules.SkyHanniRule
import at.hannibal2.skyhanni.detektrules.potentialbugs.StorageNeedsExpose.Companion.CONFIG_PACKAGE
import at.hannibal2.skyhanni.detektrules.potentialbugs.StorageNeedsExpose.Companion.STORAGE_PACKAGE
import at.hannibal2.skyhanni.detektrules.utils.DetektUtils.doWeNeedToCheckConfigProp
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType

@RequiresTypeResolution
class StorageVarOrVal(config: Config) : SkyHanniRule(config) {
    override val issue = Issue(
        "StorageVarOrVal",
        Severity.CodeSmell,
        "Storage and config variables should be declared as `var` for primitives and `val` for objects.",
        Debt.TEN_MINS,
    )

    private enum class StorageType { VAR, VAL }

    override fun visitKtFile(file: KtFile) {
        val packageName = file.packageDirective?.fqName?.asString() ?: ""
        if (!packageName.startsWith(CONFIG_PACKAGE)) return
        super.visitKtFile(file)
    }

    override fun visitProperty(property: KtProperty) {
        if (!property.doWeNeedToCheckConfigProp()) return

        val typeRef = property.typeReference ?: return
        val ktType = bindingContext[BindingContext.TYPE, typeRef] ?: return

        val shouldBeVar =  KotlinBuiltIns.isPrimitiveType(ktType) ||
            ktType.isStringType() ||
            ktType.isEnum() ||
            isChromaColour(ktType)

        val expected = if (shouldBeVar) StorageType.VAR else StorageType.VAL
        val actual = if (property.isVar) StorageType.VAR else StorageType.VAL

        if (actual != expected) return property.reportIssue(
            "${typeRef.text} `${property.name}` should be a ${expected.name.lowercase()}",
        )

        super.visitProperty(property)
    }

    private fun KotlinType.isStringType(): Boolean {
        val desc = (constructor.declarationDescriptor as? ClassDescriptor) ?: return false
        return desc.fqNameSafe.asString() == "kotlin.String"
    }

    private fun isChromaColour(type: KotlinType): Boolean {
        val desc = (type.constructor.declarationDescriptor as? ClassDescriptor) ?: return false
        return desc.fqNameSafe == FqName("io.github.notenoughupdates.moulconfig.ChromaColour")
    }
}
