package at.hannibal2.skyhanni.features.event.diana

enum class RareDianaMob(val mobName: String) {
    SPHINX("Sphinx"),
    MINOS_INQUISITOR("Minos Inquisitor"),
    MANTICORE("Manticore"),
    KING_MINOS("King Minos"),
    ;

    companion object {
        fun fromName(name: String): RareDianaMob? =
            entries.firstOrNull { name.contains(it.mobName) }
    }
}
