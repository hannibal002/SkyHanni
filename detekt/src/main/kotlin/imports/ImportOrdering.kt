package imports

object ImportOrdering {
    private val importOrder = Comparator(::compareImports)

    private val packageImportOrdering = listOf("java.", "javax.", "kotlin.", "kotlinx.")

    fun getOrdering(): Comparator<String> = importOrder
    
    private fun compareImports(import1: String, import2: String): Int {
        val path1 = import1.removePrefix("import ").substringBefore(" as ")
        val path2 = import2.removePrefix("import ").substringBefore(" as ")

        val alias1 = import1.contains(" as ")
        val alias2 = import2.contains(" as ")

        val index1 = packageImportOrdering.indexOfFirst(path1::startsWith)
        val index2 = packageImportOrdering.indexOfFirst(path2::startsWith)

        return when {
            alias1 && alias2 -> path1.compareTo(path2)
            alias1 -> 1
            alias2 -> -1
            index1 == -1 && index2 == -1 -> path1.compareTo(path2)
            index1 == -1 -> -1
            index2 == -1 -> 1
            index1 != index2 -> index1.compareTo(index2)
            else -> path1.compareTo(path2)
        }
    }
}
