package at.hannibal2.skyhanni.features.misc.customtodos

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.jsonobjects.repo.CommunityTodo
import at.hannibal2.skyhanni.data.jsonobjects.repo.CommunityTodosJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils

@SkyHanniModule
object CustomTodoDownload {

    private var todos: List<CommunityTodo>? = null

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdownloadtodo") {
            category = CommandCategory.USERS_ACTIVE
            description = "Download community /shtodos"

            argCallback("name", BrigadierArguments.greedyString(), BrigadierUtils.dynamicSuggestionProvider { getTodoIds() }) { id ->
                val todos = todos ?: run {
                    ChatUtils.userError("Invalid repo data")
                    return@argCallback
                }
                for (todo in todos) {
                    if (todo.id == id) {
                        val template = CustomTodo.fromTemplate(todo.todoData) ?: run {
                            ChatUtils.userError("Todo is invalid, please report this on discord")
                            return@argCallback
                        }
                        SkyHanniMod.customTodos.customTodos += template
                        SkyHanniMod.configManager.saveConfig(ConfigFileType.CUSTOM_TODOS, "Save file")
                        ChatUtils.chat("Todo downloaded successfully")
                        return@argCallback
                    }
                }
                ChatUtils.userError("Todo not found")
            }

            simpleCallback {
                ChatUtils.userError("Do /shdownloadtodo <id>")
            }

        }
    }

    private fun getTodoIds(): List<String> {
        return todos?.map { it.id } ?: listOf()
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val constant = event.getConstant<CommunityTodosJson>("community/CommunityTodos")
        todos = constant.communityTodos

    }
}
