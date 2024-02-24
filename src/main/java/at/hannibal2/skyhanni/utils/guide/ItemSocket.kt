package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.guide.ItemSocket.Companion.keyShape
import net.minecraft.item.ItemStack
import java.util.regex.Pattern

/**
 * Represents an item socket with a unique key, optional category, pattern for internal name,
 * and an evaluation function to determine if an item should be socketed into it.
 *
 * @property key The unique key identifying this item socket.
 *                     When changing the key to something else don't forget to make a config move.
 * @property category The category of items that can be socketed into this socket. Defaults to any.
 * @property internalNamePattern The pattern used to match internal names of items that can be socketed. Defaults to any.
 * @property evaluate The function that evaluates whether an item can be socketed into this socket.
 *                     It takes two parameters: the current item socketed into the socket and the new item to evaluate.
 *                     If the function returns true, the new item will replace the current one; if false, the current item will remain.
 *                     Defaults to always replace the item.
 * @property currentItem The currently socketed item in this socket.
 * @throws IllegalArgumentException if the provided key fails shape requirements (see [keyShape]) or if the key is already defined somewhere else.
 */
class ItemSocket(
    val key: String,
    val category: ItemCategory? = null,
    val internalNamePattern: Pattern? = null,
    val evaluate: (ItemStack, ItemStack) -> Boolean = defaultEvaluate,
) {

    var currentItem: ItemStack?
        get() = ProfileStorageData.profileSpecific?.itemSockets?.get(key)
        set(value) {
            ProfileStorageData.profileSpecific?.itemSockets?.put(key, value)
        }

    init {
        require(validateKey(key)) { "itemSocket key: \"$key\" failed shape requirements" }
        require(!map.contains(key)) { "itemSocket key: \"$key\" already defined somewhere else" }
        map[key] = this
        checkMap.getOrPut(this.category, { mutableListOf() }).add(this)
        currentItem = null
    }

    fun deleteCurrent() {
        currentItem = null
    }

    override fun toString() = buildString {
        append("ItemSocket(key='")
        append(key)
        append("'")
        if (category != null) {
            append(",category=")
            append(category.name)
        }
        if (internalNamePattern != null) {
            append(",internalNamePattern=")
            append(internalNamePattern.toString())
        }
        append(",evaluate=")
        if (evaluate == defaultEvaluate) {
            append("default")
        } else {
            append(evaluate.toString()) // Not very descriptive
        }
        append(")")
    }

    private fun checkSocket(item: ItemStack, internalName: String) {
        if (internalNamePattern != null && !internalNamePattern.matches(internalName)) return
        if (currentItem?.let { this.evaluate(it, item) } == false) return
        currentItem = item
    }

    companion object {

        private val defaultEvaluate: (ItemStack, ItemStack) -> Boolean = { _, _ -> true }

        private val keyShape = Pattern.compile("^[A-Za-z0-9._]+$")
        private fun validateKey(string: String) = keyShape.matches(string)

        private val map = mutableMapOf<String, ItemSocket>()
        private val checkMap = mutableMapOf<ItemCategory?, MutableList<ItemSocket>>()

        fun getSocketFromKey(key: String) = map[key]
        fun updateSockets(item: ItemStack) {
            val itemCategory = item.getItemCategoryOrNull()
            val internalName = item.getInternalName().asString()
            checkMap[itemCategory]?.forEach {
                it.checkSocket(item, internalName)
            }
            checkMap[null]?.forEach {
                it.checkSocket(item, internalName)
            }
        }
    }
}
