package imports

import PreprocessingPattern
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtFile

/**
 * This rule enforces correct import ordering, while taking preprocessed imports into account.
 */
class CustomImportOrdering(config: Config) :
    SkyHanniRule(config, "Enforces correct import ordering, taking into account preprocessed imports.") {

    private data class ImportLine(
        val text: String,
        val lineIndex: Int,
    )

    private data class ImportBlock(
        val inPreprocessingBlock: Boolean,
        val importLines: List<ImportLine>,
    )

    private fun parseImportSection(file: KtFile): List<String> {
        val imports = mutableListOf<String>()
        var inImports = false

        fun isImportSectionLine(line: String): Boolean =
            line.startsWith("import ") ||
                line.startsWith("/*import ") ||
                PreprocessingPattern.IF.matches(line) ||
                PreprocessingPattern.ELSEIF.matches(line) ||
                PreprocessingPattern.ELSE.matches(line) ||
                PreprocessingPattern.ENDIF.matches(line) ||
                line.isBlank()

        for (rawLine in file.text.lineSequence()) {
            val line = rawLine.trim()

            when {
                line.startsWith("package ") -> continue
                !inImports && line.isBlank() -> continue
                isImportSectionLine(line) -> {
                    inImports = true
                    imports += rawLine
                }
                inImports -> break
            }
        }

        return imports
    }

    private fun createImportBlocks(rawLines: List<String>): List<ImportBlock> {
        val blocks = mutableListOf<ImportBlock>()
        var currentImports = mutableListOf<ImportLine>()
        var inPreprocessingBlock = false

        fun flush() {
            if (currentImports.isNotEmpty()) {
                blocks += ImportBlock(inPreprocessingBlock, currentImports)
                currentImports = mutableListOf()
            }
        }

        for ((index, rawLine) in rawLines.withIndex()) {
            val line = rawLine.trim()

            when {
                PreprocessingPattern.IF.matches(line) -> {
                    flush()
                    inPreprocessingBlock = true
                }

                PreprocessingPattern.ELSEIF.matches(line) ||
                    PreprocessingPattern.ELSE.matches(line) -> {
                    flush()
                    inPreprocessingBlock = true
                }

                PreprocessingPattern.ENDIF.matches(line) -> {
                    flush()
                    inPreprocessingBlock = false
                }

                line.startsWith("/*import ") -> {
                    currentImports += ImportLine(line.removePrefix("/*"), index)
                }

                line.startsWith("import ") -> {
                    currentImports += ImportLine(line, index)
                }
            }
        }

        flush()

        return blocks
    }

    // Preprocessed blocks must be at the end of the import list.
    private fun isPreprocessingBlocksLast(blocks: List<ImportBlock>): Boolean {
        var preprocessingStart = false
        for (block in blocks) {
            if (block.inPreprocessingBlock) {
                preprocessingStart = true
            } else if (preprocessingStart) {
                return false
            }
        }
        return true
    }

    // Must have empty lines between preprocessed and non-preprocessed blocks.
    private fun isValidSpacingBetweenBlocks(
        rawLines: List<String>,
        blocks: List<ImportBlock>,
    ): Boolean {
        return blocks.zipWithNext().none { (current, next) ->
            current.inPreprocessingBlock != next.inPreprocessingBlock &&
                rawLines
                    .subList(
                        current.importLines.last().lineIndex + 1,
                        next.importLines.first().lineIndex,
                    )
                    .none(String::isBlank)
        }
    }

    // Must not have empty lines inside a block.
    private fun isValidSpacingInsideBlocks(
        rawLines: List<String>,
        blocks: List<ImportBlock>,
    ): Boolean {
        return blocks.all { block ->
            block.importLines.zipWithNext().none { (current, next) ->
                rawLines
                    .subList(current.lineIndex + 1, next.lineIndex)
                    .any(String::isBlank)
            }
        }
    }

    // Each block must be ordered according to the ordering defined in ImportOrdering.
    private fun areImportsOrdered(blocks: List<ImportBlock>): Boolean =
        blocks.all { block ->
            val imports = block.importLines.map { it.text }
            imports == imports.sortedWith(ImportOrdering.getOrdering())
        }

    // Cannot use visitImportList(importList: KtImportList) since it does not count the last commented out preprocessed block as part of the import list.
    override fun visitKtFile(file: KtFile) {
        val rawText = parseImportSection(file)
        if (rawText.isEmpty()) {
            return
        }

        val blocks = createImportBlocks(rawText)
        require(blocks.isNotEmpty()) { "No import blocks found in the import list." }

        if (!isPreprocessingBlocksLast(blocks)) {
            file.reportIssue(
                "Preprocessed import blocks must be at the end of the import list.",
            )
        }

        if (!isValidSpacingInsideBlocks(rawText, blocks)) {
            file.reportIssue(
                "Import blocks must not contain empty lines between imports.",
            )
        }

        if (!isValidSpacingBetweenBlocks(rawText, blocks)) {
            file.reportIssue(
                "Preprocessed and non-preprocessed import blocks must be separated by an empty line.",
            )
        }

        if (!areImportsOrdered(blocks)) {
            file.reportIssue(
                "Imports must be ordered in lexicographic order " +
                    "with \"java\", \"javax\", \"kotlin\", \"kotlinx\" and aliases in the end.",
            )
        }

        super.visitKtFile(file)
    }
}
