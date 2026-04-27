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
    }
}

enum class DungeonBoss(private val bossName: String) {
    E("The Watcher"),
    F1("Bonzo"),
    F2("Scarf"),
    F3("The Professor"),
    F4("Thorn"),
    F5("Livid"),
    F6("Sadan"),
    F7("Necron");

    companion object {

        fun byBossName(bossName: String): DungeonBoss? = entries.firstOrNull { it.bossName == bossName }
    }
}
