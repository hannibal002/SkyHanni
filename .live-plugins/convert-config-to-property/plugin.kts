// depends-on-plugin org.jetbrains.kotlin
// depends-on-plugin com.intellij.java

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import liveplugin.PluginUtil.show
import liveplugin.registerIntention
import org.jetbrains.kotlin.idea.codeinsight.api.classic.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.ImportPath

registerIntention(ConvertConfigOptionToPropertyIntention())

class ConvertConfigOptionToPropertyIntention :
    SelfTargetingOffsetIndependentIntention<KtProperty>(
        KtProperty::class.java,
        { "Convert ConfigOption to Property" }
    ) {

    override fun isApplicableTo(element: KtProperty): Boolean =
        element.isVar &&
            element.annotationEntries.any { it.shortName?.asString() == "ConfigOption" } &&
            element.typeReference != null &&
            element.initializer != null

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val project = element.project
        val factory = KtPsiFactory(project)
        element.valOrVarKeyword.replace(factory.createValKeyword())

        val oldTypeRef = element.typeReference!!
        val newTypeRef = factory.createType("Property<${oldTypeRef.text}>")
        oldTypeRef.replace(newTypeRef)

        val oldInit = element.initializer!!
        val newInit = factory.createExpression("Property.of(${oldInit.text})")
        element.setInitializer(newInit)

        val file = element.containingKtFile
        val importFq = "io.github.notenoughupdates.moulconfig.observer.Property"
        if (file.importDirectives.none { it.importPath?.pathStr == importFq }) {
            val importDirective = factory.createImportDirective(ImportPath.fromString(importFq))
            (file.importList ?: file).add(importDirective)
        }

        show("Converted ${element.name} to Property")
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        val element = file.findElementAt(editor.caretModel.offset)
            ?.getStrictParentOfType<KtProperty>()
            ?: return IntentionPreviewInfo.EMPTY

        val modifiers = element.modifierList?.text?.let { "$it " } ?: ""
        val varOrVal = element.valOrVarKeyword.text
        val name = element.name!!
        val typeText = element.typeReference!!.text
        val initText = element.initializer!!.text

        val beforeLine = "$modifiers$varOrVal $name: $typeText = $initText"
        val afterLine = "${modifiers}val $name: Property<$typeText> = Property.of($initText)"

        return IntentionPreviewInfo.CustomDiff(file.fileType, beforeLine, afterLine)
    }
}
