import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import liveplugin.PluginUtil.show
import liveplugin.registerInspection
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVisitorVoid
import java.awt.datatransfer.StringSelection

// depends-on-plugin org.jetbrains.kotlin
// depends-on-plugin com.intellij.java

registerInspection(CopyConfigPathIntention())

class CopyConfigPathIntention : AbstractKotlinInspection() {
    override fun getDisplayName() = "Copy config path for a ConfigOption property"
    override fun getShortName() = "CopyConfigPath"
    override fun getGroupDisplayName() = "SkyHanni"
    override fun isEnabledByDefault() = true

    override fun buildVisitor(
        holder: com.intellij.codeInspection.ProblemsHolder,
        isOnTheFly: Boolean
    ) = object : KtVisitorVoid() {
        override fun visitProperty(property: KtProperty) {
            if (property.annotationEntries.any { it.shortName?.asString() == "ConfigOption" }) {
                holder.registerProblem(
                    property.nameIdentifier ?: property,
                    "Copy config path",
                    ProblemHighlightType.INFORMATION,
                    CopyPathQuickFix()
                )
            }
        }
    }
}

class CopyPathQuickFix : LocalQuickFix {
    override fun getName() = "Copy config path"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val prop = PsiTreeUtil.getParentOfType(descriptor.psiElement, KtProperty::class.java) ?: return
        val path = computePath(prop, project)
        CopyPasteManager.getInstance().setContents(StringSelection(path))
        show("Copied: $path")
    }

    private fun computePath(prop: KtProperty, project: Project): String {
        val segments = mutableListOf<String>()
        segments.add(prop.name ?: return "")
        var currentClass = PsiTreeUtil.getParentOfType(prop, KtClassOrObject::class.java)
        while (currentClass != null) {
            val containing = findContaining(currentClass, project) ?: break
            segments.add(containing.first)
            currentClass = containing.second
        }
        segments.reverse()
        return segments.joinToString(".").removePrefix("modules.editor.")
    }

    /**
     * For a given config class KtClassOrObject, finds the single property
     * (in another class) whose type refers to this class.
     * Returns Pair(propertyName, its containing KtClassOrObject).
     */
    private fun findContaining(
        kClass: KtClassOrObject,
        project: Project
    ): Pair<String, KtClassOrObject>? {
        val fqName = kClass.fqName?.asString() ?: return null
        val psiClass = JavaPsiFacade
            .getInstance(project)
            .findClass(fqName, GlobalSearchScope.allScope(project))
            ?: return null

        for (ref in ReferencesSearch.search(psiClass, GlobalSearchScope.projectScope(project))) {
            val elem = ref.element
            val prop = PsiTreeUtil.getParentOfType(elem, KtProperty::class.java) ?: continue
            val parentClass = PsiTreeUtil.getParentOfType(prop, KtClassOrObject::class.java) ?: continue
            if (parentClass == kClass) continue
            return Pair(prop.name!!, parentClass)
        }
        return null
    }
}
