package at.hannibal2.skyhanni.data

object BingoAPI {

    private var ranks = mapOf<String, Int>()

    init {
        ranks = mapOf(
            "§7Ⓑ" to 0,
            "§aⒷ" to 1,
            "§9Ⓑ" to 2,
            "§5Ⓑ" to 3,
            "§6Ⓑ" to 4,
        )
    }

    fun getRank(text: String) = ranks.entries.find { text.contains(it.key) }?.value

    fun getIcon(searchRank: Int) = ranks.entries.find { it.value == searchRank }?.key

}