package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments.getArg
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider

object BrigadierBuilderUtils {

    inline fun <B : ArgumentBuilder<Any?, B>, reified T> BrigadierBuilder<B>.argWithCallback(
        name: String,
        type: ArgumentType<T>,
        suggestions: SuggestionProvider<Any?>? = null,
        crossinline callback: ArgContext.(T) -> Unit,
    ) {
        arg(name, type, suggestions) {
            callback { callback(getArg(name)) }
        }
    }

    inline fun <B : ArgumentBuilder<Any?, B>, reified T> BrigadierBuilder<B>.argWithCallback(
        name: String,
        type: ArgumentType<T>,
        suggestions: Collection<String>,
        crossinline callback: ArgContext.(T) -> Unit,
    ) {
        arg(name, type, suggestions) {
            callback { callback(getArg(name)) }
        }
    }

    fun <B : ArgumentBuilder<Any?, B>> BrigadierBuilder<B>.argStringCallback(
        name: String,
        suggestions: SuggestionProvider<Any?>? = null,
        callback: ArgContext.(String) -> Unit,
    ) = argWithCallback(name, BrigadierArguments.string(), suggestions, callback)

    fun <B : ArgumentBuilder<Any?, B>> BrigadierBuilder<B>.argStringCallback(
        name: String,
        suggestions: Collection<String>,
        callback: ArgContext.(String) -> Unit,
    ) = argWithCallback(name, BrigadierArguments.string(), suggestions, callback)
}
