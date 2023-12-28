package at.hannibal2.skyhanni.features.misc.items.enchants

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.misc.items.enchants.EnchantParser.ENCHANTMENT_PATTERN
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import java.util.TreeSet
import kotlin.collections.HashMap

class Enchants {
    var NORMAL: HashMap<String, Enchant.Normal> = hashMapOf()
    var ULTIMATE: HashMap<String, Enchant.Ultimate> = hashMapOf()
    var STACKING: HashMap<String, Enchant.Stacking> = hashMapOf()

    fun getFromLore(passedLoreName: String) : Enchant {
        val loreName = passedLoreName.lowercase()
        var enchant: Enchant? = NORMAL[loreName]
        if (enchant == null) enchant = ULTIMATE[loreName]
        if (enchant == null) enchant = STACKING[loreName]
        if (enchant == null) enchant = Enchant.Dummy(passedLoreName)
        return enchant
    }

    fun containsEnchantment(enchants: Map<String, Int>, line: String) : Boolean {
        val matcher = ENCHANTMENT_PATTERN.matcher(line)
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

open class Enchant : Comparable<Enchant> {
    var nbtName = ""
    var loreName = ""
    private var goodLevel = 0
    private var maxLevel = 0

    private fun isNormal() = this is Normal
    private fun isUltimate() = this is Ultimate
    private fun isStacking() = this is Stacking

    open fun getFormattedName(level: Int) = getFormat(level) + loreName

    open fun getFormat(level: Int) : String {
        val config = SkyHanniMod.feature.enchantParsing.colorEnchants

        if (level >= maxLevel) return config.perfectEnchantColor.getChatColor()
        if (level > goodLevel) return config.greatEnchantColor.getChatColor()
        if (level == goodLevel) return config.goodEnchantColor.getChatColor()
        return config.poorEnchantColor.getChatColor()
    }

    override fun toString() = "$nbtName $goodLevel $maxLevel\n"

    override fun compareTo(other: Enchant): Int {
        if (this.isUltimate() == other.isUltimate()) {
            if (this.isStacking() == other.isStacking()) {
                return this.loreName.compareTo(other.loreName)
            }
            return if (this.isStacking()) -1 else 1
        }
        return if (this.isUltimate()) -1 else 1
    }

    class Normal : Enchant() {
    }

    class Ultimate : Enchant() {
        override fun getFormat(level: Int) = "§d§l"
    }

    class Stacking : Enchant() {
        private var nbtNum: String? = null
        private var statLabel: String? = null
        private var stackLevel: TreeSet<Int>? = null

        override fun toString() = "$nbtNum ${stackLevel.toString()} ${super.toString()}"
    }

    class Dummy(name: String) : Enchant() {
        init {
            loreName = name
            nbtName = name
        }

        // Ensures enchants not yet in repo stay as vanilla formatting
        // (instead of that stupid dark red lowercase formatting *cough* sba *cough*)
        override fun getFormattedName(level: Int) = "§9$loreName"
    }
}

class FormattedEnchant(
    private val enchant: Enchant,
    private val level: Int,
    stacking: String
) : Comparable<FormattedEnchant> {
    private val stacking: String = stacking
        get() = "§8$field"
    private val loreDescription: MutableList<String> = mutableListOf()

    fun addLore(lineOfLore: String) = loreDescription.add(lineOfLore)

    fun getLore() = loreDescription

    override fun compareTo(other: FormattedEnchant) = this.enchant.compareTo(other.enchant)

    fun getFormattedString() : String {
        val builder = StringBuilder()
        builder.append(enchant.getFormattedName(level)).append(" ").append(level.toRoman())

        return if (!stacking.contains("empty")) builder.append(stacking).toString() else builder.toString()
    }
}
