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

val baseConfigPkg = "at.hannibal2.skyhanni.config"
val baseConfigClass = "at.hannibal2.skyhanni.config.Features"

val baseStoragePkg = "$baseConfigPkg.storage"
val profileStorageClass = "$baseStoragePkg.ProfileSpecificStorage"
val playerStorageClass = "$baseStoragePkg.PlayerSpecificStorage"

class NavigateToConfigIntention :
    SelfTargetingOffsetIndependentIntention<KtStringTemplateExpression>(
        KtStringTemplateExpression::class.java,
        { "Go to definition" }
    ) {
    override fun isApplicableTo(element: KtStringTemplateExpression): Boolean {
        val literal = element.text.removeSurrounding("\"")
        if (!literal.contains('.')) {
            return false
        }

        val call = PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java) ?: return false
        val dot = call.parent as? KtDotQualifiedExpression ?: return false
        return !(dot.receiverExpression.text != "event" || call.calleeExpression?.text != "move")
    }

    private fun searchProjectFor(
        element: KtStringTemplateExpression,
        className: String,
    ) : KtClassOrObject? = JavaPsiFacade.getInstance(element.project).findClass(
        className,
        GlobalSearchScope.projectScope(element.project)
    )?.navigationElement as? KtClassOrObject

    override fun applyTo(element: KtStringTemplateExpression, editor: Editor?) {
        val path = element.text.removeSurrounding("\"")
        val segments = path.split('.').takeIf { it.isNotEmpty() }?.toMutableList() ?: return
        val project = element.project

        val basePath = when (segments.first()) {
            "#profile" -> {
                segments.removeFirst()
                profileStorageClass
            }
            "#player" -> {
                segments.removeFirst()
                playerStorageClass
            }
            else -> baseConfigClass
        }

        var current = searchProjectFor(element, basePath) ?: run {
            show("⚠️ Could not find root Features class for path '$path'")
            return
        }

        for ((i, name) in segments.withIndex()) {
            val prop = current.declarations.filterIsInstance<KtProperty>().firstOrNull { it.name == name } ?: run {
                show("⚠️ Property '$name' not found in ${current.name} for path '$path'")
                return
            }

            if (i == segments.lastIndex) {
                (prop.navigationElement as? NavigatablePsiElement)?.navigate(true)
                return
            }

            val isPropMap = prop.typeReference?.text?.startsWith("MutableMap") == true ||
                prop.typeReference?.text?.startsWith("Map") == true
            if (i == segments.lastIndex - 1 && isPropMap) {
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
                .filter { it.qualifiedName?.startsWith(baseConfigPkg) == true }
            val psiClass = candidates.firstOrNull() ?: run {
                show("⚠️ Config class '$rawType' not found under '$baseConfigPkg' for path '$path'")
                return
            }

            current = psiClass.navigationElement as? KtClassOrObject ?: run {
                show("⚠️ Navigation target for class '$rawType' is not a KtClassOrObject")
                return
            }
        }
    }
}
