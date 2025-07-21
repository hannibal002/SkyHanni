package at.hannibal2.skyhanni.features.slayer

import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntitySpider
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityWolf

enum class SlayerType(
    val displayName: String,
    val rngName: String,
    val clazz: Class<*>,
    val miniBossType: SlayerMiniBossType? = null,
) {
    REVENANT(
        "Revenant Horror",
        "revenant",
        EntityZombie::class.java,
        SlayerMiniBossType.REVENANT,
    ),
    TARANTULA(
        "Tarantula Broodfather",
        "tarantula",
        EntitySpider::class.java,
        SlayerMiniBossType.TARANTULA,
    ),
    SVEN(
        "Sven Packmaster",
        "sven",
        EntityWolf::class.java,
        SlayerMiniBossType.SVEN,
    ),
    VOID(
        "Voidgloom Seraph",
        "voidgloom",
        EntityEnderman::class.java,
        SlayerMiniBossType.VOIDLING,
    ),
    INFERNO(
        "Inferno Demonlord",
        "inferno",
        EntityBlaze::class.java,
        SlayerMiniBossType.INFERNAL,
    ),
    VAMPIRE(
        "Bloodfiend",
        "vampire",
        EntityZombie::class.java,
    ) // previously called "Riftstalker Bloodfiend"
    ;

    companion object {
        fun getByName(name: String): SlayerType? = entries.firstOrNull { name.contains(it.displayName) }
        fun getByClazzName(name: String): SlayerType? = entries.firstOrNull {
            it.clazz.simpleName.removePrefix("Entity").equals(name, ignoreCase = true)
        }
    }
}

enum class SlayerMiniBossType(vararg names: String) {
    REVENANT("Revenant Sycophant", "Revenant Champion", "Deformed Revenant", "Atoned Champion", "Atoned Revenant"),
    TARANTULA("Tarantula Vermin", "Tarantula Beast", "Mutant Tarantula"),
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
