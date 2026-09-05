package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.monster.Blaze
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.spider.Spider
import net.minecraft.world.entity.monster.zombie.Zombie

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
        Zombie::class.java,
        SlayerMiniBossType.REVENANT,
        listOf("Atoned Horror"),
    ),
    TARANTULA(
        "Tarantula Broodfather",
        "tarantula",
        Spider::class.java,
        SlayerMiniBossType.TARANTULA,
        listOf("Conjoined Brood"),
    ),
    SVEN(
        "Sven Packmaster",
        "sven",
        Wolf::class.java,
        SlayerMiniBossType.SVEN,
    ),
    VOID(
        "Voidgloom Seraph",
        "voidgloom",
        EnderMan::class.java,
        SlayerMiniBossType.VOIDLING,
    ),
    INFERNO(
        "Inferno Demonlord",
        "inferno",
        Blaze::class.java,
        SlayerMiniBossType.INFERNAL,
    ),
    VAMPIRE(
        "Bloodfiend",
        "vampire",
        Zombie::class.java,
    ) // previously called "Riftstalker Bloodfiend"
    ;

    // The cost reduction gained by contributing to the Bartender's Brewery project (5%)
    // overrides the discount gained by having all slayers at level 7 (4%).
    fun calculateSpawnCost(tier: Int, includeReduction: Boolean = true): Double? {
        val base = SlayerApi.jsonData?.spawnCosts?.get(this)?.get(tier) ?: return null
        val bonusLevel = SlayerApi.bonusRewardsLevel

        val reduction = when {
            SlayerApi.breweryContribution ->
                SlayerApi.BREWERY_CONTRIBUTION_REDUCTION

            bonusLevel >= SlayerApi.COST_REDUCTION_LEVEL -> {
                if (bonusLevel > SlayerApi.COST_REDUCTION_LEVEL) {
                    ErrorManager.skyHanniError(
                        "Slayer Bonus Rewards Level is above max level ($bonusLevel)",
                        "Bonus Rewards Level" to bonusLevel,
                    )
                }
                SlayerApi.COST_REDUCTION
            }

            else -> 1.0
        }

        var cost = if (includeReduction) base * reduction else base.toDouble()
        if (Perk.SLASHED_PRICING.isActive) cost *= 0.5
        return cost
    }

    fun calculateXPGain(tier: Int, @Suppress("unused") includeAatrox: Boolean = true): Double? {
        val xpBuff = Perk.SLAYER_XP_BUFF.isActive
        val baseGained = SlayerApi.jsonData?.xpGains?.get(this)?.get(tier) ?: return null

        return baseGained * (if (xpBuff) 1.25 else 1.0)
    }

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
