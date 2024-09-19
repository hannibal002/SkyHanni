package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.commands.CommandContextAwareObject
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matches

object CommandUtils {

    class ItemGroup(val name: String, vararg items: Pair<String, Int>) {

        val icon = items.first().first.asInternalName()

        val items = items.associate { it.first.asInternalName() to it.second }

        init {
            entries[items.first().first.uppercase().replace(" ", "_")] = this
        }

        companion object {

            private val entries = mutableMapOf<String, ItemGroup>()

            fun findGroup(string: String): ItemGroup? {
                val search = string.replace(" ", "_").uppercase()
                return entries[search]
            }
        }
    }

    private enum class NameSource {
        INTERNAL_NAME,
        ITEM_NAME,
        GROUP
        ;
    }

    fun itemCheck(args: Iterable<String>, context: CommandContextAwareObject): Pair<Int, Any?> {
        @Suppress("ReplaceSizeZeroCheckWithIsEmpty") // A bug since the replacement does not work for iterable interface.
        if (args.count() == 0) {
            context.errorMessage = "No item specified"
            return 0 to null
        }
        val first = args.first()

        val namePattern = "^name:".toRegex()
        val internalPattern = "^internal:".toRegex()
        val groupPattern = "^(?:group|collection):".toRegex()

        val expected = when {
            namePattern.matches(first) -> NameSource.ITEM_NAME
            internalPattern.matches(first) -> NameSource.INTERNAL_NAME
            groupPattern.matches(first) -> NameSource.GROUP
            else -> null
        }

        val grabbed = args.takeWhile { "[a-zA-Z:_\"';]+([:-;]\\d+)?".toPattern().matches(it) }

        val collected = grabbed.joinToString(" ").replace("[\"']".toRegex(), "")

        val item: Any? = when (expected) {
            NameSource.INTERNAL_NAME -> collected.replace(internalPattern, "").replace(" ", "_").asInternalName()
            NameSource.ITEM_NAME -> NEUInternalName.fromItemNameOrNull(collected.replace(namePattern, "").replace("_", " "))
            NameSource.GROUP -> ItemGroup.findGroup(collected)
            null -> {
                val fromItemName = NEUInternalName.fromItemNameOrNull(collected.replace("_", " "))
                if (fromItemName?.getItemStackOrNull() != null) {
                    fromItemName
                } else {
                    val internalName = collected.replace(" ", "_").asInternalName()
                    if (internalName.getItemStackOrNull() != null) {
                        internalName
                    } else {
                        ItemGroup.findGroup(collected)
                    }
                }
            }
        }

        if ((item as? NEUInternalName)?.getItemStackOrNull() == null && (item as? ItemGroup) == null) {
            context.errorMessage = "Could not find a valid item for: '$collected'"
        }

        return grabbed.size to item
    }

    fun <T : CommandContextAwareObject> numberCalculate(args: Iterable<String>, context: T, use: (T, Long) -> Unit): Int {
        NEUCalculator.calculateOrNull(args.firstOrNull())?.toLong()?.let { use(context, it) } ?: {
            context.errorMessage = "Unkown number/calculation: '${args.firstOrNull()}'"
        }
        return args.firstOrNull()?.let { 1 } ?: 0
    }

}

