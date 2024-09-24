package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.commands.CommandArgument
import at.hannibal2.skyhanni.config.commands.CommandContextAwareObject
import at.hannibal2.skyhanni.events.TabCompletionEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.TreeMap

object CommandUtils {

    class ItemGroup(val name: String, vararg items: Pair<String, Int>, val collection: String = "") {

        val icon = items.first().first.asInternalName()

        val items = items.associate { it.first.asInternalName() to it.second }

        init {
            entries[name] = this
        }

        companion object {

            private val entries = TreeMap<String, ItemGroup>()

            fun groupStartingWith(start: String): Collection<ItemGroup> = StringUtils.subMapOfStringsStartingWith(start, entries).values

            fun groupNameStartingWith(start: String): List<String> = groupStartingWith(start).map { it.name }

            fun findGroup(string: String): ItemGroup? {
                val search = string.replace(" ", "_").uppercase()
                return entries[search]
            }
        }
    }

    private enum class NameSource {
        INTERNAL_NAME,
        ITEM_NAME,
        GROUP;
    }

    private val namePattern = "^(?i)(name:)(.*)".toRegex()
    private val internalPattern = "^(?i)(internal:)(.*)".toRegex()
    private val groupPattern = "(?i)^(group:|collection:)(.*)".toRegex()

    fun itemCheck(args: Iterable<String>, context: CommandContextAwareObject): Pair<Int, Any?> {
        @Suppress("ReplaceSizeZeroCheckWithIsEmpty") // A bug since the replacement does not work for iterable interface.
        if (args.count() == 0) {
            context.errorMessage = "No item specified"
            return 0 to null
        }
        val first = args.first()

        val expected = when {
            namePattern.matches(first) -> NameSource.ITEM_NAME
            internalPattern.matches(first) -> NameSource.INTERNAL_NAME
            groupPattern.matches(first) -> NameSource.GROUP
            else -> null
        }

        val grabbed = args.takeWhile { "[a-zA-Z:_\"';]+([:-;]\\d+)?".toPattern().matches(it) }

        val collected = grabbed.joinToString(" ").replace("[\"']".toRegex(), "")

        val item: Any? = when (expected) {
            NameSource.INTERNAL_NAME -> collected.replace(internalPattern, "$2").replace(" ", "_").asInternalName()
            NameSource.ITEM_NAME -> NEUInternalName.fromItemNameOrNull(collected.replace(namePattern, "$2").replace("_", " "))
            NameSource.GROUP -> ItemGroup.findGroup(collected.replace(groupPattern, "$2"))
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

    fun itemTabComplete(start: String): List<String> = buildList {
        if (start.isEmpty()) return@buildList
        val expected = when {
            namePattern.matches(start) -> NameSource.ITEM_NAME
            internalPattern.matches(start) -> NameSource.INTERNAL_NAME
            groupPattern.matches(start) -> NameSource.GROUP
            else -> null
        }

        val uppercaseStart = start.uppercase().replace(" ", "_")
        val lowercaseStart = start.lowercase().replace("_", " ")

        fun MutableList<String>.resultAdd(pattern: Regex, start: String, transformedStart: String, f: (String) -> Collection<String>) {
            val prefix = start.replace(pattern, "$1")
            val withoutPrefix = transformedStart.replace(pattern, "$2")
            if (withoutPrefix.isEmpty()) return
            val lastSpaceIndex = start.replace(pattern, "$2").indexOfLast { it == ' ' } + 1
            this@resultAdd.addAll(
                f(withoutPrefix).map { r ->
                    if (lastSpaceIndex == 0) {
                        prefix + r
                    } else {
                        r.substring(lastSpaceIndex)
                    }
                },
            )
        }

        when (expected) {
            NameSource.INTERNAL_NAME -> resultAdd(internalPattern, start, uppercaseStart, NEUItems::findInternalNameStartingWithWithoutNPCs)
            NameSource.ITEM_NAME -> resultAdd(namePattern, start, lowercaseStart, NEUItems::findItemNameStartingWithWithoutNPCs)
            NameSource.GROUP -> resultAdd(groupPattern, start, uppercaseStart, ItemGroup::groupNameStartingWith)
            null -> {
                val lastSpaceIndex = start.indexOfLast { it == ' ' } + 1
                addAll(ItemGroup.groupStartingWith(uppercaseStart).map { it.name.substring(lastSpaceIndex) })
                addAll(NEUItems.findInternalNameStartingWithWithoutNPCs(uppercaseStart).map { it.substring(lastSpaceIndex) })
                if (size < 200) {
                    addAll(
                        NEUItems.findItemNameStartingWithWithoutNPCs(lowercaseStart).map { r ->
                            r.substring(lastSpaceIndex).replace(" ", "_")
                        },
                    )
                }
            }
        }
    }

    fun <T : CommandContextAwareObject> numberCalculate(args: Iterable<String>, context: T, use: (T, Long) -> Unit): Int {
        NEUCalculator.calculateOrNull(args.firstOrNull())?.toLong()?.let { use(context, it) } ?: {
            context.errorMessage = "Unkown number/calculation: '${args.firstOrNull()}'"
        }
        return args.firstOrNull()?.let { 1 } ?: 0
    }

}

data class ComplexCommand<O : CommandContextAwareObject>(
    val name: String,
    val specifiers: Collection<CommandArgument<O>>,
    val context: () -> O,
) {

    init {
        entries[name] = this
    }

    private fun tabParse(args: List<String>, partial: String?): List<String> {
        val context = context()

        var index = 0
        var amountNoPrefixArguments = 0

        while (args.size > index) {
            val loopStartAmountNoPrefix = amountNoPrefixArguments
            val current = args[index]
            val (spec, lookup) = specifiers.firstOrNull { it.prefix == current && it.validity(context) }?.let { it to 0 }
                ?: specifiers.firstOrNull { it.defaultPosition == amountNoPrefixArguments && it.validity(context) }?.let {
                    amountNoPrefixArguments++
                    it to -1
                } ?: specifiers.firstOrNull { it.defaultPosition == -2 && it.validity(context) }?.let {
                    amountNoPrefixArguments++
                    it to -1
                } ?: (null to 0)
            val result = spec?.handler?.let { it(args.slice((lookup + index + 1)..<args.size), context) }
            if (result == null) {
                context.errorMessage = "Unknown argument: '$current'"
            }
            context.errorMessage?.let {
                if (loopStartAmountNoPrefix != amountNoPrefixArguments) {
                    amountNoPrefixArguments = loopStartAmountNoPrefix
                }
                break
            }
            index += (1 + lookup) + (result ?: 0)
        }

        val result = mutableListOf<String>()

        val validSpecifier = specifiers.filter { it.validity(context) }

        val rest = (args.slice(index..<args.size).joinToString(" ") + (partial?.let { " $it" } ?: "")).trimStart()

        if (rest.isEmpty()) {
            result.addAll(validSpecifier.mapNotNull { it.prefix.takeIf { it.isNotEmpty() } })
            result.addAll(validSpecifier.filter { it.defaultPosition == amountNoPrefixArguments }.map { it.tabComplete("") }.flatten())
        } else {
            result.addAll(
                validSpecifier.filter { it.prefix.startsWith(rest) }.mapNotNull { it.prefix.takeIf { it.isNotEmpty() } },
            )
            result.addAll(validSpecifier.filter { it.defaultPosition == amountNoPrefixArguments }.map { it.tabComplete(rest) }.flatten())
        }

        return result
    }

    @SkyHanniModule
    companion object {
        val entries = mutableMapOf<String, ComplexCommand<*>>()

        // TODO clear on chat close
        private var tabCachedCommand: ComplexCommand<*>? = null
        private var tabCached = emptyList<String>()
        private var tabBeginString = ""

        @SubscribeEvent
        fun onTabCompletion(event: TabCompletionEvent) {
            val command = entries[event.command] ?: return
            if (tabCachedCommand == command && event.leftOfCursor == tabBeginString) {
                event.addSuggestions(tabCached)
                return
            }
            tabCachedCommand = command
            val rawArgs = event.leftOfCursor.split(" ").drop(1)
            val isPartial = rawArgs.last().isNotEmpty()
            val args = if (isPartial) rawArgs.dropLast(1) else rawArgs

            val partial = if (isPartial) rawArgs.last() else null

            tabBeginString = event.leftOfCursor
            tabCached = command.tabParse(args, partial)
            event.addSuggestions(tabCached)
        }
    }
}
