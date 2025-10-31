package at.hannibal2.hanni.utils

class DisplayTableEntry(
    val left: String,
    val right: String,
    val sort: Number,
    val item: NeuInternalName,
    val hover: List<String> = emptyList(),
    val highlightsOnHoverSlots: List<Int> = emptyList(),
)
