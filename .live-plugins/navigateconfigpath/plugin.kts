import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import liveplugin.registerIntention
import liveplugin.PluginUtil.show
import org.jetbrains.kotlin.idea.codeinsight.api.classic.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.psi.*

// depends-on-plugin org.jetbrains.kotlin
// depends-on-plugin com.intellij.java

registerIntention(NavigateToConfigIntention())

val basePkg = "at.hannibal2.skyhanni.config"
val baseClass = "at.hannibal2.skyhanni.config.Features"

class NavigateToConfigIntention :
    SelfTargetingOffsetIndependentIntention<KtStringTemplateExpression>(
        KtStringTemplateExpression::class.java,
        { "Go to config" }
    ) {
    override fun isApplicableTo(element: KtStringTemplateExpression): Boolean {
        val literal = element.text.removeSurrounding("\"")
        if (literal.startsWith("#") || !literal.contains('.')) {
            return false
        }

        val call = PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java) ?: return false
        val dot = call.parent as? KtDotQualifiedExpression ?: return false
        if (dot.receiverExpression.text != "event" || call.calleeExpression?.text != "move") {
            return false
        }
        return true
    }

    override fun applyTo(element: KtStringTemplateExpression, editor: Editor?) {
        val path = element.text.removeSurrounding("\"")
        val segments = path.split('.')
        val project = element.project

        var current = JavaPsiFacade.getInstance(project).findClass(
            baseClass,
            GlobalSearchScope.projectScope(project)
        )?.navigationElement as? KtClassOrObject ?: run {
            show("⚠️ Could not find root Features class for path '$path'")
            return
        }

        for ((i, name) in segments.withIndex()) {
            val prop = current?.declarations.orEmpty().filterIsInstance<KtProperty>().firstOrNull { it.name == name } ?: run {
                show("⚠️ Property '$name' not found in ${current.name} for path '$path'")
                return
            }

            if (i == segments.lastIndex) {
                show("Opening ${current.name} for path '$path'")
                (prop.navigationElement as? NavigatablePsiElement)?.navigate(true)
                return
            }

            val rawType = prop.typeReference?.text?.substringBefore('<')?.substringBefore('?') ?: run {
                show("⚠️ Could not parse type of '${prop.name}' in ${current.name} for path '$path'")
                return
            }

            val scope = GlobalSearchScope.projectScope(project)
            val candidates = PsiShortNamesCache
                .getInstance(project)
                .getClassesByName(rawType, scope)
                .filter { it.qualifiedName?.startsWith(basePkg) == true }
            val psiClass = candidates.firstOrNull() ?: run {
                show("⚠️ Config class '$rawType' not found under '$basePkg' for path '$path'")
                return
            }

            current = psiClass.navigationElement as? KtClassOrObject ?: run {
                show("⚠️ Navigation target for class '$rawType' is not a KtClassOrObject")
                return
            }
        }
    }
}
