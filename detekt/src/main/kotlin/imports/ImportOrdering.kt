package at.hannibal2.skyhanni.detektrules.imports

import org.jetbrains.kotlin.psi.KtImportDirective

object ImportOrdering {
    private val importOrder = ImportSorter()

    fun getOrdering(): Comparator<KtImportDirective> {
        return importOrder
    }

    private val packageImportOrdering = listOf("java.", "javax.", "kotlin.")

    private class ImportSorter : Comparator<KtImportDirective> {
        override fun compare(
            import1: KtImportDirective,
            import2: KtImportDirective,
        ): Int {
            val importPath1 = import1.importPath!!.pathStr
            val importPath2 = import2.importPath!!.pathStr

            val isTypeAlias1 = import1.aliasName != null
            val isTypeAlias2 = import2.aliasName != null

            val index1 = packageImportOrdering.indexOfFirst { importPath1.startsWith(it) }
            val index2 = packageImportOrdering.indexOfFirst { importPath2.startsWith(it) }

            return when {
                isTypeAlias1 && isTypeAlias2 -> importPath1.compareTo(importPath2)
                isTypeAlias1 && !isTypeAlias2 -> 1
                !isTypeAlias1 && isTypeAlias2 -> -1
                index1 == -1 && index2 == -1 -> importPath1.compareTo(importPath2)
                index1 == -1 -> -1
                index2 == -1 -> 1
                else -> index1.compareTo(index2)
            }
        }
    }
}
