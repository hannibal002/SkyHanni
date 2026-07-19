package imports

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList

/**
 * This rule enforces correct import ordering, while taking preprocessed imports into account.
 */
class CustomImportOrdering(config: Config) :
    SkyHanniRule(config, "Enforces correct import ordering, taking into account preprocessed imports.") {

    private data class ImportLine(
        val lineIndex: Int,
        val text: String,
    )

    private data class ImportBlock(
        val inPreprocessingBlock: Boolean,
        val imports: List<ImportLine>,
    )

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

        for ((index, line) in rawLines.withIndex()) {
            val trimmed = line.trim()

            when {
                PreprocessingPattern.IF.matches(trimmed) -> {
                    flush()
                    inPreprocessingBlock = true
                }

                PreprocessingPattern.ELSEIF.matches(trimmed) ||
                    PreprocessingPattern.ELSE.matches(trimmed) -> {
                    flush()
                    inPreprocessingBlock = true
                }

                PreprocessingPattern.ENDIF.matches(trimmed) -> {
                    flush()
                    inPreprocessingBlock = false
                }

                trimmed.startsWith("import ") -> {
                    currentImports += ImportLine(index, trimmed)
                }
            }
        }

        flush()

        return blocks
    }

    private fun isValidSpacingBetweenBlocks(
        rawLines: List<String>,
        blocks: List<ImportBlock>,
    ): Boolean {
        return blocks.zipWithNext().none { (current, next) ->
            current.inPreprocessingBlock != next.inPreprocessingBlock &&
                rawLines
                    .subList(
                        current.imports.last().lineIndex + 1,
                        next.imports.first().lineIndex,
                    )
                    .none(String::isBlank)
        }
    }

    private fun isValidSpacingInsideBlocks(
        rawLines: List<String>,
        blocks: List<ImportBlock>,
    ): Boolean {
        return blocks.all { block ->
            block.imports.zipWithNext().none { (current, next) ->
                rawLines
                    .subList(current.lineIndex + 1, next.lineIndex)
                    .any(String::isBlank)
            }
        }
    }

    private fun isBlockOrdered(
        block: ImportBlock,
        imports: Iterator<KtImportDirective>,
    ): Boolean {
        val expected = buildList {
            repeat(block.imports.size) {
                if (!imports.hasNext()) {
                    return false
                }
                add(imports.next())
            }
        }
            .sortedWith(ImportOrdering.getOrdering())
            .map { "import ${it.importPath}" }

        return block.imports.map { it.text } == expected
    }

    private fun areImportsOrdered(
        imports: List<KtImportDirective>,
        blocks: List<ImportBlock>,
    ): Boolean {
        val iterator = imports.iterator()

        return blocks.all { block ->
            isBlockOrdered(block, iterator)
        } && !iterator.hasNext()
    }

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

    override fun visitImportList(importList: KtImportList) {
        val rawText = importList.text.lines()
        if (rawText.all(String::isBlank)) {
            return
        }

        val blocks = createImportBlocks(rawText)

        // Should never happen unless import processing is broken.
        require(blocks.isNotEmpty()) { "No import blocks found in the import list." }

        if (!isPreprocessingBlocksLast(blocks)) {
            importList.reportIssue(
                "Preprocessed import blocks must be at the end of the import list.",
            )
        }

        if (!isValidSpacingInsideBlocks(rawText, blocks)) {
            importList.reportIssue(
                "import blocks must not contain empty lines between imports.",
            )
        }

        if (!isValidSpacingBetweenBlocks(rawText, blocks)) {
            importList.reportIssue(
                "Preprocessed and non-preprocessed import blocks must be separated by an empty line."
            )
        }

        if (!areImportsOrdered(importList.imports, blocks)) {
            importList.reportIssue(
                "Imports must be ordered in lexicographic order " +
                    "with \"java\", \"javax\", \"kotlin\", \"kotlinx\" and aliases in the end."
            )
        }

        super.visitImportList(importList)
    }
}
