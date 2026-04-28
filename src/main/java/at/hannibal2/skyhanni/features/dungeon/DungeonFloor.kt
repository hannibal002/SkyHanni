package at.hannibal2.skyhanni.features.dungeon

enum class DungeonFloor(val boss: DungeonBoss, val masterMode: Boolean) {
    E(DungeonBoss.E, false),

    F1(DungeonBoss.F1, false),
    F2(DungeonBoss.F2, false),
    F3(DungeonBoss.F3, false),
    F4(DungeonBoss.F4, false),
    F5(DungeonBoss.F5, false),
    F6(DungeonBoss.F6, false),
    F7(DungeonBoss.F7, false),

    M1(DungeonBoss.F1, true),
    M2(DungeonBoss.F2, true),
    M3(DungeonBoss.F3, true),
    M4(DungeonBoss.F4, true),
    M5(DungeonBoss.F5, true),
    M6(DungeonBoss.F6, true),
    M7(DungeonBoss.F7, true),
    ;

    fun asString() = name

    companion object {
        fun getByName(name: String): DungeonFloor? = entries.find { it.name == name }
        fun getByBoss(boss: DungeonBoss, masterMode: Boolean): DungeonFloor? =
            entries.find { it.boss == boss && it.masterMode == masterMode }
    }
}

enum class DungeonBoss(private val bossName: String, private val floor: Int) {
    E("The Watcher", 0),
    F1("Bonzo", 1),
    F2("Scarf", 2),
    F3("The Professor", 3),
    F4("Thorn", 4),
    F5("Livid", 5),
    F6("Sadan", 6),
    F7("Necron", 7);

    companion object {

        fun byBossName(bossName: String): DungeonBoss? = entries.firstOrNull { it.bossName == bossName }
        fun byFloorNumber(floor: Int): DungeonBoss = entries.firstOrNull { it.floor == floor } ?: error("unknown floor number: $floor")
    }
}
