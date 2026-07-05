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

    private fun String.isLineBlockSeparator(): Boolean {
        if (this.isEmpty()) return true

        return this.containsPreprocessingPattern() ||
            this.startsWith("//?") ||
            this.startsWith("//~") ||
            this.startsWith("//#")
    }

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
                // Start of //? if block
                line.contains(PreprocessingPattern.IF.asComment) -> {
                    flushCurrentBlock()
                    inIfBlock = true
                }

                // End of //? if block
                line.contains(PreprocessingPattern.ENDIF.asComment) -> {
                    flushCurrentBlock()
                    inIfBlock = false
                }

                // Single-import directives.
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

                line.isBlank() -> {
                    flushCurrentBlock()
                }
            }
        }

        flushCurrentBlock()

        return blocks
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

    private fun checkEmptyLines(importList: KtImportList): KtImportDirective? {
        val rawLines = importList.text.lines()
        val imports = importList.imports.iterator()

        var inBlock = false
        var seenEmptyLine = false

        for (rawLine in rawLines) {
            val line = rawLine.trim()
            if (line.startsWith("import ")) {
                if (!imports.hasNext()) break
                val current = imports.next()

                // If we hit an import, and we've already seen an empty line
                // while in a block, this is an illegal break.
                if (inBlock && seenEmptyLine) {
                    return current
                }

                inBlock = true
                seenEmptyLine = false
            } else if (line.isEmpty()) {
                if (inBlock) seenEmptyLine = true
            } else if (line.isLineBlockSeparator()) {
                // Preprocessor separators reset the block, so empty lines before them
                // were valid boundaries.
                inBlock = false
                seenEmptyLine = false
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

        val emptyLineViolation = checkEmptyLines(importList)
        if (emptyLineViolation != null) {
            emptyLineViolation.reportIssue("Illegal empty line between standard imports.")
            return
        }

        super.visitImportList(importList)
    }
}
