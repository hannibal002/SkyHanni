// plugin.kts – LivePlugin script to customize Optimize Imports for Kotlin
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import kotlin.text.trim
import kotlin.math.min
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.psi.PsiFile
import liveplugin.show
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.ex.ScrollingModelEx

// Utility: find conditional blocks and their import line indices
data class ConditionalBlock(
    val condition: String,
    val ifImportLines: Set<String>,
    val elseImportLines: Set<String>?,
    val ifStartLine: Int,
    val elseStartLine: Int?,
    val negatives: Set<String>
) {

    val ifOrder = mutableListOf<String>()
    val elseOrder = mutableListOf<String>()

    fun correctedImport(line: String): String {
        if (negatives.contains(line)) {
            return "//\$\$ $line"
        } else {
            return line
        }
    }
}

fun Document.deleteLine(line: Int) {
    this.deleteString(this.getLineStartOffset(line), min(this.getLineEndOffset(line) + 1, this.textLength))
}

fun Document.insertLine(line: Int, text: String) {
    if(line>16){show(this.text.replace("\n","\\n"))}
    this.insertString(this.getLineStartOffset(line), text + "\n")
}

fun Document.replaceLine(line: Int, text: String) {
    this.replaceString(
        this.getLineStartOffset(line),
        min(this.getLineEndOffset(line) + 1, this.textLength),
        text + "\n"
    )
}

fun findConditionalBlocks(text: String, doc: Document): Pair<List<ConditionalBlock>, List<Int>> {
    val lines = text.lines()
    val blocks = mutableListOf<ConditionalBlock>()
    val globalImports = mutableSetOf<String>()
    val toDelete = mutableListOf<Int>()
    var i = 0
    while (i < lines.size) {
        var trim = lines[i].trim()
        if (trim.startsWith("//#if") && (lines[i + 1].trim().startsWith("import ") || lines[i + 1].trim().startsWith("//\$\$ import "))) {
            val cond = trim
            toDelete.add(i)
            val ifImports = mutableSetOf<String>()
            var elseImports: MutableSet<String>? = null
            var importList = ifImports
            val negatives = mutableSetOf<String>()
            var elseStart: Int? = null
            val ifStart = i + 1
            i++
            trim = lines[i].trim()
            while (i < lines.size && !trim.startsWith("//#endif")) {
                if (trim.startsWith("//#else")) {
                    elseImports = mutableSetOf()
                    importList = elseImports
                    elseStart = i + 1
                    toDelete.add(i)
                    i++
                    trim = lines[i].trim()
                    continue
                }
                if (trim.startsWith("import ")) {
                    importList.add(trim)
                    toDelete.add(i)
                }
                if (trim.startsWith("//\$\$ import ")) {
                    val edited = trim.removePrefix("//\$\$ ")
                    importList.add(edited)
                    doc.replaceLine(i, edited)
                    negatives.add(edited)
                    toDelete.add(i)
                }
                i++
                trim = lines[i].trim()
            }
            toDelete.add(i)
            blocks.add(ConditionalBlock(cond, ifImports, elseImports, ifStart, elseStart, negatives))
        } else {
            if (trim.startsWith("import ")) {
                globalImports.add(trim)
                toDelete.add(i)
            }
        }
        i++
    }
    blocks.add(ConditionalBlock("", globalImports, null, 2, null, emptySet()))
    return blocks to toDelete
}

class CustomOptimizeImports(val oldAction: AnAction) : AnAction(
    oldAction.templatePresentation.text,
    oldAction.templatePresentation.description,
    oldAction.templatePresentation.icon
) {

    override fun update(e: AnActionEvent) {
        oldAction.update(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR) ?: return
        var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? KtFile ?: return
        // Capture View
        val scrollingModel = editor.scrollingModel
        val initialScroll = scrollingModel.verticalScrollOffset
        val initialCaret  = editor.caretModel.logicalPosition

        editor.settings.isAnimatedScrolling = false

        // Record imports in conditional blocks *before* optimization
        val originalText = psiFile.text
        var originalBlocks: List<ConditionalBlock> = emptyList()
        var linesToDelete: List<Int> = emptyList()

        WriteCommandAction.runWriteCommandAction(project) {
            val pair = findConditionalBlocks(originalText, editor.document)
            originalBlocks = pair.first
            linesToDelete = pair.second
            val blocks = originalBlocks.sortedBy { it.ifStartLine }

            val postActions = object : Runnable {
                override fun run() {
                    if (blocks.size == 1) return
                    show("Custom Optimize Imports run!")
                    // Read the order of imports after sort
                    psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? KtFile ?: return
                    for (line in psiFile.text.lines()) {
                        val trim = line.trim()
                        if (!trim.startsWith("import ")) continue
                        for (block in blocks) {
                            if (block.ifImportLines.contains(trim)) {
                                block.ifOrder.add(trim)
                                break
                            } else if (block?.elseImportLines?.contains(trim) == true) {
                                block.elseOrder.add(trim)
                                break
                            }
                        }
                    }
                    // Fix removed Imports (does not happen for globale to remove unused imports)
                    for (block in blocks.drop(1)) {
                        if (block.ifOrder.size != block.ifImportLines.size) {
                            for (import in block.ifImportLines) {
                                if (block.ifOrder.contains(import)) continue
                                block.ifOrder.add(import)
                            }
                        }
                        if (block.elseImportLines == null) continue
                        if (block.elseOrder.size != block.elseImportLines.size) {
                            for (import in block.elseImportLines) {
                                if (block.elseOrder.contains(import)) continue
                                block.elseOrder.add(import)
                            }
                        }
                    }

                    PsiDocumentManager.getInstance(project)
                        .doPostponedOperationsAndUnblockDocument(editor.document)

                    // Reset File, remove all import lines and then insert them back in order
                    WriteCommandAction.runWriteCommandAction(project) {
                        val doc = editor.document
                        doc.replaceString(0, doc.textLength, originalText)

                        for (line in linesToDelete.reversed()) {
                            doc.deleteLine(line)
                        }
                        val lines = doc.text.lines()
                        var i = lines.indexOfFirst { it.startsWith("package") } + 1
                        for (line in lines.drop(i)) {
                            if (line.isNotBlank()) break
                            doc.deleteLine(i)
                        }

                        for (block in blocks) {
                            doc.insertLine(i++, block.condition)
                            for (importLine in block.ifOrder) {
                                doc.insertLine(i++, block.correctedImport(importLine))
                            }
                            if (block.elseImportLines != null) {
                                doc.insertLine(i++, "//#else")
                                for (importLine in block.elseOrder) {
                                    doc.insertLine(i++, block.correctedImport(importLine))
                                }
                            }
                            if (block.condition.isNotEmpty()) doc.insertLine(i++, "//#endif")
                        }
                        doc.insertLine(i++, "") // Empty Line at the End of Imports
                    }
                }
            }
            // Run the original optimize imports action
            PsiDocumentManager.getInstance(project).commitDocument(editor.document)
            val directPsiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? PsiFile
                ?: return@runWriteCommandAction
            OptimizeImportsProcessor(project, arrayOf(directPsiFile), postActions).run()
            PsiDocumentManager.getInstance(project).commitDocument(editor.document)

            // Restore View
            scrollingModel.scrollVertically(initialScroll)
            editor.caretModel.moveToLogicalPosition(initialCaret)
            editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
        }
        editor.settings.isAnimatedScrolling = true
    }
}

// Replace the Optimize Imports action
val actionManager = ActionManager.getInstance()
var origOptimizeAction = actionManager.getAction("OptimizeImports")
if (origOptimizeAction != null) {
    show("Old: ${origOptimizeAction::class}")
    // Check if already replaced
    if (origOptimizeAction::class.toString().contains("CustomOptimizeImports")) {
        fun getPropertyValue(obj: Any, propName: String): Any? {
// find the property by name
            val prop = obj::class
                .memberProperties
                .firstOrNull { it.name == propName }
                as? KProperty1<Any, *>

// read its value from the instance
            return prop?.get(obj)
        }

        origOptimizeAction = getPropertyValue(origOptimizeAction, "oldAction") as? AnAction
        show("Replaced: ${origOptimizeAction::class}")
    }
    val newOptimize = CustomOptimizeImports(origOptimizeAction)
    actionManager.replaceAction("OptimizeImports", newOptimize)
}
