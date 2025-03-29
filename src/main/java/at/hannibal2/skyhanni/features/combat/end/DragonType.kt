package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase

enum class DragonType(
    val color: LorenzColor,
    val selectable: Boolean = true,
) {
    PROTECTOR(
        LorenzColor.GRAY,
    ),
    OLD(
        LorenzColor.YELLOW,
    ),
    UNSTABLE(
        LorenzColor.DARK_PURPLE,
    ),
    YOUNG(
        LorenzColor.WHITE,
    ),
    STRONG(
        LorenzColor.RED,
    ),
    WISE(
        LorenzColor.AQUA,
    ),
    SUPERIOR(
        LorenzColor.GOLD,
    ),
    UNKNOWN(
        LorenzColor.WHITE,
        false,
    );

    val displayName: String = "${name.firstLetterUppercase()} Dragon"

    val fragment by lazy { "${name}_FRAGMENT".toInternalName() }

    override fun toString(): String = displayName

    companion object {
        fun getByName(name: String): DragonType {
            return entries.firstOrNull { it.name == name } ?: UNKNOWN
        }
    }
}
