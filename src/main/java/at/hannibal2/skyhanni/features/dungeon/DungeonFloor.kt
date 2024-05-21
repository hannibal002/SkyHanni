package at.hannibal2.skyhanni.features.dungeon

enum class DungeonFloor(private val bossName: String, val isMaster: Boolean = false) {
    E("The Watcher"),
    F1("Bonzo"),
    F2("Scarf"),
    F3("The Professor"),
    F4("Thorn"),
    F5("Livid"),
    F6("Sadan"),
    F7("Necron"),
    M1("Bonzo", true),
    M2("Scarf", true),
    M3("The Professor", true),
    M4("Thorn", true),
    M5("Livid", true),
    M6("Sadan", true),
    M7("Necron", true);

    companion object {
        fun byBossName(bossName: String) = DungeonFloor.entries.firstOrNull { it.bossName == bossName }
        fun getNormalName(input: DungeonFloor) = input.name.replace("M", "F")
    }
}
