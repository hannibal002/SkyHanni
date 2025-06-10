package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NeuItems.isGenerator
import at.hannibal2.skyhanni.utils.RegexUtils.matches

object CommandUtils {

    private enum class NameSource {
        INTERNAL_NAME,
        ITEM_NAME
    }

    private val namePattern = "^(?i)(name:)(.*)".toRegex()
    private val internalPattern = "^(?i)(internal:)(.*)".toRegex()

    private val removeApostrophe = "[\"']".toRegex()

    private val itemNamePattern = "[a-zA-Z:_\"';]+([:\\-;]\\d+)?".toPattern()

    fun itemCheck(
        args: Iterable<String>,
        context: CommandContextAwareObject,
        validItems: (NeuInternalName) -> Boolean = { true },
        aliases: Map<String, NeuInternalName> = NeuItems.commonItemAliases.global,
        notFoundResponse: (String) -> String = { "Could not find a valid item for: '$it'" },
    ): Pair<Int, Any?> {
        // This replacement does not work for iterable interface. Therefore, the suppression.
        @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
        if (args.count() == 0) {
            context.errorMessage = "No item specified"
            return 0 to null
        }
        val first = args.first()

        val expected = when {
            namePattern.matches(first) -> NameSource.ITEM_NAME
            internalPattern.matches(first) -> NameSource.INTERNAL_NAME
            else -> null
        }

        val grabbed = args.takeWhile { itemNamePattern.matches(it) }

        val collected = grabbed.joinToString(" ").replace(removeApostrophe, "").lowercase()

        val item: NeuInternalName? = when (expected) {
            NameSource.INTERNAL_NAME -> collected.replace(internalPattern, "$2").replace(" ", "_").toInternalName()
            NameSource.ITEM_NAME -> NeuInternalName.fromItemNameOrNull(collected.replace(namePattern, "$2").replace("_", " "))
            null -> {
                val alias = aliases[collected.replace("_", " ")]?.takeIf { validItems(it) }
                if (alias != null) {
                    alias
                } else {
                    val fromItemName = NeuInternalName.fromItemNameOrNull(collected.replace("_", " "))?.takeIf { validItems(it) }
                    if (fromItemName?.getItemStackOrNull() != null) {
                        fromItemName
                    } else {
                        val internalName = collected.replace(" ", "_").toInternalName().takeIf { validItems(it) }
                        if (internalName?.getItemStackOrNull() != null) {
                            internalName
                        } else {
                            null
                        }
                    }
                }
            }
        }

        if (item?.getItemStackOrNull() == null) {
            context.errorMessage = notFoundResponse(collected)
        }

        return grabbed.size to item
    }

    fun itemTabComplete(
        start: String,
        validItems: (NeuInternalName) -> Boolean = { !it.isGenerator() },
        aliases: Map<String, NeuInternalName> = NeuItems.commonItemAliases.global,
        suggestAtEmpty: Boolean = false,
    ): List<String> = buildList {
        if (!suggestAtEmpty && start.isEmpty()) return@buildList
        val expected = when {
            namePattern.matches(start) -> NameSource.ITEM_NAME
            internalPattern.matches(start) -> NameSource.INTERNAL_NAME
            else -> null
        }

        val uppercaseStart = start.uppercase().replace(" ", "_")
        val lowercaseStart = start.lowercase().replace("_", " ")

        fun MutableList<String>.resultAdd(
            pattern: Regex,
            start: String,
            transformedStart: String,
            similar: (String) -> Collection<String>,
        ) {
            val prefix = start.replace(pattern, "$1")
            val withoutPrefix = transformedStart.replace(pattern, "$2")
            if (withoutPrefix.isEmpty()) return
            val lastSpaceIndex = start.replace(pattern, "$2").indexOfLast { it == ' ' } + 1
            this@resultAdd.addAll(
                similar(withoutPrefix).map { result ->
                    if (lastSpaceIndex == 0) {
                        prefix + result
                    } else {
                        result.substring(lastSpaceIndex)
                    }
                },
            )
        }

        when (expected) {
            NameSource.INTERNAL_NAME -> resultAdd(
                internalPattern,
                start,
                uppercaseStart,
                { NeuItems.findInternalNameStartingWithWithoutNPCs(it, validItems) },
            )

            NameSource.ITEM_NAME -> resultAdd(
                namePattern,
                start,
                lowercaseStart,
                { NeuItems.findItemNameStartingWithWithoutNPCs(it, validItems) },
            )

            null -> {
                val lastSpaceIndex = start.indexOfLast { it == ' ' } + 1
                addAll(aliases.filter { it.key.startsWith(lowercaseStart) }.filter { validItems(it.value) }.keys)
                addAll(NeuItems.findInternalNameStartingWithWithoutNPCs(uppercaseStart, validItems).map { it.substring(lastSpaceIndex) })
                // 200 is here to limit the max amount of results since more than that can introduce performance issues for the client
                if (size < 200) {
                    addAll(
                        NeuItems.findItemNameStartingWithWithoutNPCs(lowercaseStart, validItems).map { result ->
                            result.substring(lastSpaceIndex).replace(" ", "_")
                        },
                    )
                }
            }
        }
    }

    fun <T : CommandContextAwareObject> numberCalculate(args: Iterable<String>, context: T, use: (T, Long) -> Unit): Int {
        NeuCalculator.calculateOrNull(args.firstOrNull())?.toLong()?.let { use(context, it) } ?: {
            context.errorMessage = "Invalid number/calculation: '${args.firstOrNull()}'"
        }
        return args.firstOrNull()?.let { 1 } ?: 0
    }
}

/**
 * Interface that needs to be implemented for a context object for a [ComplexCommand].
 * The implementer should store the state during processing the [CommandArgument]'s of the specific [ComplexCommand].
 * @property errorMessage Setting this to none null will print the [errorMessage] as a user error and terminates the command handling.
 * @property post It is executed after all arguments have been handled. Here should all the processing happen / the execution of the command.
 */
interface CommandContextAwareObject {

    var errorMessage: String?

    fun post()
}

/**
 * An Argument that is used by a [ComplexCommand].
 * [T] is the [CommandContextAwareObject] that should be mutated by the [ComplexCommand].
 * @param documentation User facing descriptor of the argument.
 *
 * It should start with a listing of the parameters it takes in followed by a " - " and the description of what it does.
 *
 * A needed parameter should be specified with: "&lt;>", inside the brackets there should be short name of what type it is
 * eg: "&lt;item>", "&lt;number>", "&lt;number/calculation>".
 * If no argument is present prefix it with: "- " and then add the rest.
 * @param prefix A prefix that can be used to directly access that argument. Eg: "-i", "-p", "-cc".
 * @param defaultPosition The position where it is expected to be called if no prefix was specified by the user.
 *
 * 0>= Index where it is expected to be used , -1 = can only be called with a prefix, -2 = expected position as last element.
 * @param validity Check the [CommandContextAwareObject] if this argument can be called with the current state
 * @param tabComplete This is called if the [ComplexCommand.tabParse] thinks this is valid
 * eg: The prefix is present before that. Or the [defaultPosition] matches
 * The input is the partial written input the user gave.
 * The return value should be all possible completions of that.
 * All return values need to start with partial input that start with the last space (exclusive) till the end of the input.
 * @param handler This is called on the execution of the command.
 * (Note: It is also called inside the [ComplexCommand.tabParse] to get the state of the [CommandContextAwareObject]
 * correct for the [tabComplete] of the following arguments)
 *
 * For error handling inside that function set the [CommandContextAwareObject.errorMessage] instead.
 *
 * The first input is the remaining elements that can/should be processed. The elements are simply the input split by spaces.
 * The return value is the amount of elements that got processed (must be >=0).
 * The second input is the [CommandContextAwareObject] at its current stage of execution.
 * This function should mutate the [CommandContextAwareObject] but shouldn't have any other side effects.
 */
data class CommandArgument<T : CommandContextAwareObject>(
    val documentation: String,
    val prefix: String = "",
    val defaultPosition: Int = -1,
    val validity: (T) -> Boolean = { true },
    val tabComplete: (String, T) -> Collection<String> = { _, _ -> emptyList() },
    val handler: (Iterable<String>, T) -> Int,
) {

    override fun toString(): String = documentation

    fun getResult(
        args: Array<String>,
        lookup: Int,
        index: Int,
        context: T,
    ) = this.handler(args.slice((lookup + index + 1)..<args.size), context)

    companion object {
        private fun <A : CommandArgument<O>, O : CommandContextAwareObject> Collection<A>.findSpecifier(
            current: String,
            context: O,
            amountNoPrefixArguments: Int,
            amountNoPrefixArgumentsIncrement: () -> Unit,
        ): Pair<A?, Int> {
            val specifierWithPrefix = firstOrNull { it.prefix == current && it.validity(context) }
            if (specifierWithPrefix != null) {
                return specifierWithPrefix to 0
            }

            val specifierAtDefaultPos = firstOrNull { it.defaultPosition == amountNoPrefixArguments && it.validity(context) }
            if (specifierAtDefaultPos != null) {
                amountNoPrefixArgumentsIncrement()
                return specifierAtDefaultPos to -1
            }

            val specifierAtMinusTwo = firstOrNull { it.defaultPosition == -2 && it.validity(context) }
            if (specifierAtMinusTwo != null) {
                amountNoPrefixArgumentsIncrement()
                return specifierAtMinusTwo to -1
            }

            return null to 0
        }

        fun <A : CommandArgument<O>, O : CommandContextAwareObject> Collection<A>.findSpecifierAndGetResult(
            args: Array<String>,
            index: Int,
            context: O,
            amountNoPrefixArguments: Int,
            amountNoPrefixArgumentsIncrement: () -> Unit,
        ): Int {
            val (spec, lookup) = findSpecifier(args[index], context, amountNoPrefixArguments, amountNoPrefixArgumentsIncrement)
            val result = spec?.getResult(args, lookup, index, context)
            return if (result == null) {
                context.errorMessage = "Unknown argument: '${args[index]}'"
                lookup + 1
            } else {
                lookup + 1 + result
            }
        }
    }
}
