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

    private fun parseImportSection(file: KtFile): List<Pair<Int, String>> {
        val imports = mutableListOf<Pair<Int, String>>()
        var inImports = false

        fun isImportSectionLine(line: String): Boolean =
            line.startsWith("import ") ||
                line.startsWith("/*import ") ||
                PreprocessingPattern.IF.matches(line) ||
                PreprocessingPattern.ELSEIF.matches(line) ||
                PreprocessingPattern.ELSE.matches(line) ||
                PreprocessingPattern.ENDIF.matches(line) ||
                line.isBlank()

        for ((index, rawLine) in file.text.lineSequence().withIndex()) {
            val line = rawLine.trim()

            when {
                line.startsWith("package ") -> continue
                !inImports && line.isBlank() -> continue

                isImportSectionLine(line) -> {
                    inImports = true
                    imports += index to rawLine
                }

                inImports -> break
            }
        }

        return imports
    }

    private fun createImportBlocks(rawLines: List<Pair<Int, String>>): List<ImportBlock> {
        val blocks = mutableListOf<ImportBlock>()
        var currentImports = mutableListOf<ImportLine>()
        var inPreprocessingBlock = false

        fun flush() {
            if (currentImports.isNotEmpty()) {
                blocks += ImportBlock(inPreprocessingBlock, currentImports)
                currentImports = mutableListOf()
            }
        }

        for ((index, rawLine) in rawLines) {
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
                    currentImports += ImportLine(
                        line.removePrefix("/*"),
                        index,
                    )
                }

                line.startsWith("import ") -> {
                    currentImports += ImportLine(
                        line,
                        index,
                    )
                }
            }
        }

        flush()

        return blocks
    }

    // Preprocessed blocks must be at the end of the import list.
    private fun findPreprocessingBlockOrderViolation(
        blocks: List<ImportBlock>,
    ): ImportLine? {
        var preprocessingStarted = false

        for (block in blocks) {
            if (block.inPreprocessingBlock) {
                preprocessingStarted = true
            } else if (preprocessingStarted) {
                return block.importLines.first()
            }
        }

        return null
    }

    // Must have empty lines between preprocessed and non-preprocessed blocks.
    private fun findSpacingBetweenBlocksViolation(
        fileLines: List<String>,
        blocks: List<ImportBlock>,
    ): ImportLine? =
        blocks.zipWithNext().firstNotNullOfOrNull { (current, next) ->
            if (
                current.inPreprocessingBlock != next.inPreprocessingBlock &&
                fileLines
                    .subList(
                        current.importLines.last().lineIndex + 1,
                        next.importLines.first().lineIndex,
                    )
                    .none(String::isBlank)
            ) {
                next.importLines.first()
            } else {
                null
            }
        }

    // Must not have empty lines inside a block.
    private fun findSpacingInsideBlockViolation(
        fileLines: List<String>,
        blocks: List<ImportBlock>,
    ): ImportLine? =
        blocks.firstNotNullOfOrNull { block ->
            block.importLines.zipWithNext().firstNotNullOfOrNull { (current, next) ->
                if (
                    fileLines
                        .subList(
                            current.lineIndex + 1,
                            next.lineIndex,
                        )
                        .any(String::isBlank)
                ) {
                    next
                } else {
                    null
                }
            }
        }

    // Each block must be ordered according to the ordering defined in ImportOrdering.
    private fun findOrderingViolation(
        blocks: List<ImportBlock>,
    ): ImportLine? =
        blocks.firstNotNullOfOrNull { block ->
            val imports = block.importLines.map { it.text }
            val sortedImports = imports.sortedWith(ImportOrdering.getOrdering())

            block.importLines.zip(sortedImports).firstOrNull { (actual, expected) ->
                actual.text != expected
            }?.first
        }

    override fun visitKtFile(file: KtFile) {
        val rawText = parseImportSection(file)

        if (rawText.all { (_, line) -> line.isBlank() }) {
            return
        }

        val blocks = createImportBlocks(rawText)

        require(blocks.isNotEmpty()) { "No import blocks found in the import list." }

        val fileLines = file.text.lines()

        findPreprocessingBlockOrderViolation(blocks)?.let {
            reportIssue(
                "Preprocessed import blocks must be at the end of the import list.",
                it.lineIndex,
                fileLines[it.lineIndex],
                file,
            )
        }

        findSpacingInsideBlockViolation(fileLines, blocks)?.let {
            reportIssue(
                "Import blocks must not contain empty lines between imports.",
                it.lineIndex,
                fileLines[it.lineIndex],
                file,
            )
        }

        findSpacingBetweenBlocksViolation(fileLines, blocks)?.let {
            reportIssue(
                "Preprocessed and non-preprocessed import blocks must be separated by an empty line.",
                it.lineIndex,
                fileLines[it.lineIndex],
                file,
            )
        }

        findOrderingViolation(blocks)?.let {
            reportIssue(
                "Imports must be ordered in lexicographic order with \"java\", \"javax\", \"kotlin\", \"kotlinx\" and aliases in the end.",
                it.lineIndex,
                fileLines[it.lineIndex],
                file,
            )
        }

        super.visitKtFile(file)
    }
}
