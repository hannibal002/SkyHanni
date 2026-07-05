package imports

import PreprocessingPattern.Companion.containsPreprocessingPattern
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList

/**
 * This rule enforces correct import ordering, while ignoring preprocessed comments and imports that are in a preprocessed block.
 */
class CustomImportOrdering(config: Config) : SkyHanniRule(config, "Enforces correct import ordering, taking into account preprocessed imports.") {

    private fun getBlocks(importList: KtImportList): List<List<KtImportDirective>> {
        val blocks = mutableListOf<List<KtImportDirective>>()
        val imports = ArrayDeque(importList.imports)
        val currentBlock = mutableListOf<KtImportDirective>()

        var inIfBlock = false
        var nextImportIsStandalone = false

        fun flushCurrentBlock() {
            if (currentBlock.isNotEmpty()) {
                blocks += currentBlock.toList()
                currentBlock.clear()
            }
        }

        for (rawLine in importList.text.lines()) {
            val line = rawLine.trim()
            when {
                line.contains(PreprocessingPattern.IF.asComment) -> {
                    flushCurrentBlock()
                    inIfBlock = true
                }
                line.contains(PreprocessingPattern.ENDIF.asComment) -> {
                    flushCurrentBlock()
                    inIfBlock = false
                }
                line.startsWith("//~") || line.startsWith("//#") -> {
                    flushCurrentBlock()
                    nextImportIsStandalone = true
                }
                line.startsWith("import ") -> {
                    val import = imports.removeFirst()

                    when {
                        inIfBlock -> {
                            currentBlock += import
                        }
                        nextImportIsStandalone -> {
                            blocks += listOf(import)
                            nextImportIsStandalone = false
                        }
                        else -> {
                            currentBlock += import
                        }
                    }
                }

                line.isBlank() -> flushCurrentBlock()
            }
        }
        flushCurrentBlock()
        return blocks
    }

    private fun checkEmptyLines(importList: KtImportList): KtImportDirective? {
        val rawLines = importList.text.lines()
        val imports = importList.imports.iterator()

        var inStandardBlock = false
        var seenEmptyLine = false

        for (line in rawLines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("import ") -> {
                    val current = imports.next()
                    if (inStandardBlock && seenEmptyLine) return current
                    inStandardBlock = true
                    seenEmptyLine = false
                }
                trimmed.isBlank() -> if (inStandardBlock) seenEmptyLine = true
                // Any other line (preprocessor/comment) resets the block state
                else -> {
                    inStandardBlock = false
                    seenEmptyLine = false
                }
            }
        }
        return null
    }

    private fun checkSorting(blocks: List<List<KtImportDirective>>): KtImportDirective? {
        for (block in blocks) {
            val sortedBlock = block.sortedWith(ImportOrdering.getOrdering())
            for (i in block.indices) {
                if (block[i].importPath != sortedBlock[i].importPath) {
                    return block[i]
                }
            }
        }
        return null
    }

    override fun visitImportList(importList: KtImportList) {
        val blocks = getBlocks(importList)

        val sortingViolation = checkSorting(blocks)
        if (sortingViolation != null) {
            sortingViolation.reportIssue("Import is not in lexicographical order.")
            return
        }

        if (checkEmptyLines(importList) != null) {
            importList.imports.first().reportIssue("There should be no empty lines between imports.")
            return
        }

        super.visitImportList(importList)
    }
}
