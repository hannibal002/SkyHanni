package at.hannibal2.skyhanni.features.misc.items.enchants

import at.hannibal2.skyhanni.features.misc.items.enchants.EnchantParser.enchantmentPattern
import com.google.gson.annotations.Expose

class EnchantsJson {
    @Expose
    var NORMAL: HashMap<String, Enchant.Normal> = hashMapOf()

    @Expose
    var ULTIMATE: HashMap<String, Enchant.Ultimate> = hashMapOf()

    @Expose
    var STACKING: HashMap<String, Enchant.Stacking> = hashMapOf()

    fun getFromLore(passedLoreName: String): Enchant {
        val loreName = passedLoreName.lowercase()
        var enchant: Enchant? = NORMAL[loreName]
        if (enchant == null) enchant = ULTIMATE[loreName]
        if (enchant == null) enchant = STACKING[loreName]
        if (enchant == null) enchant = Enchant.Dummy(passedLoreName)
        return enchant
    }

    fun containsEnchantment(enchants: Map<String, Int>, line: String): Boolean {
        val matcher = enchantmentPattern.matcher(line)
        while (matcher.find()) {
            val enchant = this.getFromLore(matcher.group("enchant"))
            if (enchants.isNotEmpty()) {
                if (enchants.containsKey(enchant.nbtName)) return true
            } else {
                if (NORMAL.containsKey(enchant.loreName.lowercase())) return true
                if (ULTIMATE.containsKey(enchant.loreName.lowercase())) return true
                if (STACKING.containsKey(enchant.loreName.lowercase())) return true
            }
        }
        return false
    }

    fun hasEnchantData() = NORMAL.isNotEmpty() && ULTIMATE.isNotEmpty() && STACKING.isNotEmpty()
}
