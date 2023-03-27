package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.utils.LorenzUtils

enum class ItemAbility(
    val abilityName: String,
    val cooldownInSeconds: Long,
    vararg val itemNames: String,
    var lastClick: Long = 0L,
    var lastNewClick: Long = 0L,
    val actionBarDetection: Boolean = true,
) {
    //TODO add into repo

    HYPERION(5, "SCYLLA", "VALKYRIE", "ASTREA"),
    GYROKINETIC_WAND(30),
    GIANTS_SWORD(30),
    ICE_SPRAY_WAND(5),
    ATOMSPLIT_KATANA(4, "VORPAL_KATANA", "VOIDEDGE_KATANA"),
    RAGNAROCK_AXE(20),
    WAND_OF_ATONEMENT(7, "WAND_OF_HEALING", "WAND_OF_MENDING", "WAND_OF_RESTORATION"),

    GOLEM_SWORD(3),
    END_STONE_SWORD(5),
    SOUL_ESOWARD(20),
    PIGMAN_SWORD(5),
    EMBER_ROD(30),
    STAFF_OF_THE_VOLCANO(30),
    STARLIGHT_WAND(2),
    VOODOO_DOLL(5),
    WEIRD_TUBA(20),
    FIRE_FREEZE_STAFF(10),
    SWORD_OF_BAD_HEALTH(5),

    // doesn't have a sound
    ENDER_BOW("Ender Warp", 30, "Ender Bow"),
    LIVID_DAGGER("Throw", 5, "Livid Dagger"),
    FIRE_VEIL("Fire Veil", 5, "Fire Veil Wand"),
    INK_WAND("Ink Bomb", 30, "Ink Wand"),

    // doesn't have a consistent sound
    ECHO("Echo", 3, "Ancestral Spade");

    var newVariant = false
    var internalNames = mutableListOf<String>()

    constructor(cooldownInSeconds: Int, vararg alternateInternalNames: String) : this("no name", cooldownInSeconds.toLong(), actionBarDetection = false) {
        newVariant = true
        internalNames.addAll(alternateInternalNames)
        internalNames.add(name)
    }

    fun oldClick() {
        lastClick = System.currentTimeMillis()
    }

    fun isOnCooldown(): Boolean = lastClick + getCooldown() > System.currentTimeMillis()

    fun getCooldown(): Long = cooldownInSeconds * 1000

    fun getDurationText(): String {
        var duration: Long = lastClick + getCooldown() - System.currentTimeMillis()
        return if (duration < 1600) {
            duration /= 100
            var d = duration.toDouble()
            d /= 10.0
            LorenzUtils.formatDouble(d)
        } else {
            duration /= 1000
            duration++
            LorenzUtils.formatInteger(duration)
        }
    }

    fun newClick() {
//        println("newClick $this")
        lastNewClick = System.currentTimeMillis()
    }

    companion object {
        fun getByInternalName(internalName: String): ItemAbility? {
            return values().firstOrNull { it.newVariant && internalName in it.internalNames }
        }
    }

}