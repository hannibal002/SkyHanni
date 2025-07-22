import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import liveplugin.registerInspection
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtVisitorVoid

// depends-on-plugin org.jetbrains.kotlin
// depends-on-plugin com.intellij.java

val skyhanniEventFQN = "at.hannibal2.skyhanni.api.event.SkyHanniEvent"
val primaryAnnotationName = "PrimaryFunction"

registerInspection(EventLinkingInspection())

class EventLinkingInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                val functionName = function.name ?: return

                // Build a map of primary event names to the fully-qualified event class names
                val primaryNameMap = buildPrimaryNameMap(function.project)
                val isPrimaryName = primaryNameMap.containsKey(functionName)

                if (isPrimaryName) {
                    // If the function name matches, register an informational problem with a quick fix.
                    val eventClassFQN = primaryNameMap[functionName]
                    holder.registerProblem(
                        function.nameIdentifier ?: function,
                        "Handler '$functionName' references event <$functionName>.",
                        ProblemHighlightType.INFORMATION,
                        NavigateToEventQuickFix(eventClassFQN)
                    )
                }
            }
        }

    override fun getDisplayName() = "Link event handler to event declaration"
    override fun getShortName() = "EventLinkingInspection"
    override fun getGroupDisplayName() = "SkyHanni"
    override fun isEnabledByDefault() = true
}

/**
 * Quick fix that navigates to the event class declaration.
 */
class NavigateToEventQuickFix(private val eventClassFQN: String?) : LocalQuickFix {
    override fun getName() = "Go to event declaration"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        if (eventClassFQN == null) return
        val psiClass: PsiClass? = JavaPsiFacade.getInstance(project).findClass(
            eventClassFQN,
            GlobalSearchScope.allScope(project)
        )
        psiClass?.navigate(true)
    }
}

/**
 * Builds a PSI-based map of primary function names (keys) to the fully-qualified names
 * of event classes (values). It searches for all classes that inherit from SkyHanniEvent
 * and then looks for a @PrimaryFunction annotation (or falls back to constructor/property values).
 */
fun buildPrimaryNameMap(project: Project): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val facade = JavaPsiFacade.getInstance(project)
    val skyhanniEventPsiClass: PsiClass = facade.findClass(
        skyhanniEventFQN,
        GlobalSearchScope.allScope(project)
    ) ?: return emptyMap()

    val inheritors = ClassInheritorsSearch.search(
        skyhanniEventPsiClass,
        GlobalSearchScope.allScope(project),
        true,
        true,
        false
    )

    for (psiInheritor in inheritors) {
        val ktDeclaration = psiInheritor.navigationElement.takeIf { it is KtClassOrObject } as? KtClassOrObject ?: continue

        val primaryAnnotation = ktDeclaration.annotationEntries.firstOrNull {
            it.shortName?.asString() == primaryAnnotationName
        }
        if (primaryAnnotation != null) {
            val valueArg = primaryAnnotation.valueArguments.firstOrNull()?.getArgumentExpression()?.text ?: continue
            val primaryName = valueArg.removeSurrounding("\"")
            if (primaryName.isNotBlank()) {
                val fqName = ktDeclaration.fqName?.asString() ?: continue
                result[primaryName] = fqName
                continue
            }
        }
    }
    return result
}

EditorFactory.getInstance().eventMulticaster.addEditorMouseListener(
    object : EditorMouseListener {
        override fun mouseClicked(e: EditorMouseEvent) {
            if (e.area != EditorMouseEventArea.EDITING_AREA) return
            val me = e.mouseEvent.takeIf { it.isControlDown && it.isShiftDown } ?: return
            val editor = e.editor
            val project = editor.project ?: return

            val vp = editor.xyToVisualPosition(me.point)
            val offset = editor.visualPositionToOffset(vp)

            val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? KtFile ?: return
            val func = psi.findElementAt(offset)?.parent as? KtNamedFunction ?: return

            val targetFqn = buildPrimaryNameMap(project)[func.name] ?: return
            val clazz = JavaPsiFacade.getInstance(project).findClass(targetFqn, GlobalSearchScope.allScope(project)) ?: return
            clazz.navigate(true)
        }
    },
    pluginDisposable
)
