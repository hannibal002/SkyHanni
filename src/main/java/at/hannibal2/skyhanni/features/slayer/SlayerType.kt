package at.hannibal2.skyhanni.features.slayer

import net.minecraft.entity.mob.BlazeEntity
import net.minecraft.entity.mob.EndermanEntity
import net.minecraft.entity.mob.SpiderEntity
import net.minecraft.entity.mob.ZombieEntity
import net.minecraft.entity.passive.WolfEntity

enum class SlayerType(
    val displayName: String,
    val rngName: String,
    val clazz: Class<*>,
    val miniBossType: SlayerMiniBossType? = null,
    val otherNames: List<String> = listOf(),
) {
    REVENANT(
        "Revenant Horror",
        "revenant",
        ZombieEntity::class.java,
        SlayerMiniBossType.REVENANT,
        listOf("Atoned Horror"),
    ),
    TARANTULA(
        "Tarantula Broodfather",
        "tarantula",
        SpiderEntity::class.java,
        SlayerMiniBossType.TARANTULA,
        listOf("Conjoined Brood"),
    ),
    SVEN(
        "Sven Packmaster",
        "sven",
        WolfEntity::class.java,
        SlayerMiniBossType.SVEN,
    ),
    VOID(
        "Voidgloom Seraph",
        "voidgloom",
        EndermanEntity::class.java,
        SlayerMiniBossType.VOIDLING,
    ),
    INFERNO(
        "Inferno Demonlord",
        "inferno",
        BlazeEntity::class.java,
        SlayerMiniBossType.INFERNAL,
    ),
    VAMPIRE(
        "Bloodfiend",
        "vampire",
        ZombieEntity::class.java,
    ) // previously called "Riftstalker Bloodfiend"
    ;

    companion object {
        fun getByName(name: String): SlayerType? = entries.firstOrNull { slayer ->
            name.contains(slayer.displayName) || slayer.otherNames.any { name.contains(it) }
        }

        fun getByClassName(name: String): SlayerType? = entries.firstOrNull {
            it.clazz.simpleName.removePrefix("Entity").equals(name, ignoreCase = true)
        }
    }
}

enum class SlayerMiniBossType(vararg names: String) {
    REVENANT("Revenant Sycophant", "Revenant Champion", "Deformed Revenant", "Atoned Champion", "Atoned Revenant"),
    TARANTULA("Tarantula Vermin", "Tarantula Beast", "Mutant Tarantula", "Primordial Jockey", "Primordial Viscount"),
    SVEN("Pack Enforcer", "Sven Follower", "Sven Alpha"),
    VOIDLING("Voidling Devotee", "Voidling Radical", "Voidcrazed Maniac"),
    INFERNAL("Flare Demon", "Kindleheart Demon", "Burningsoul Demon"),
    ;

    val names = names.toSet()

    companion object {
        private val allNames = entries.flatMap { it.names }.toSet()

        fun isMiniboss(name: String) = name in allNames
    }
}
