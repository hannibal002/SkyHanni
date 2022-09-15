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


    // TODO replace old logic

    HEAL_1("Small Heal", 7, "Wand of Healing"),
    HEAL_2("Medium Heal", 7, "Wand of Mending"),
    HEAL_3("Big Heal", 7, "Wand of Restoration"),
    HEAL_4("Huge Heal", 7, "Wand of Atonement"),

    STAR_FALL("Starfall", 2, "Starlight Wand"),
    VODOO_DOLL("Acupuncture", 5, "Voodoo Doll"),
    INK_WAND("Ink Bomb", 30, "Ink Wand"),
    GOLEM_SWORD("Iron Punch", 3, "Golem Sword"),
    EMBER_ROD("Fire Blast", 30, "Ember Rod"),
    ENDER_BOW("Ender Warp", 30, "Ender Bow"),

    LIVID_DAGGER("Throw", 5, "Livid Dagger"),
    WEIRD_TUBA("Howl", 20, "Weird Tuba"),

    ENDSTONE_SWORD("Extreme Focus", 5, "End Stone Sword"),
    PIGMAN_SWORD("Burning Souls", 5, "Pigman Sword"),

    SOULWARD("Soulward", 20, "Soul Esoward"),
    ECHO("Echo", 3, "Ancestral Spade"),

    FIRE_VEIL("Fire Veil", 5, "Fire Veil Wand"),
    //TODO add new crimson isle weapons

    ;

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
            LorenzUtils.formatInteger(duration.toInt())
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