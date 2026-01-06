package at.hannibal2.skyhanni.features.misc.customtodos

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.jsonobjects.repo.CommunityTodo
import at.hannibal2.skyhanni.data.jsonobjects.repo.CommunityTodosJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils

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
                        SkyHanniMod.customTodos.customTodos.add(template.also { it.downloaded = true })
                        CustomTodos.save()
                        ChatUtils.chat("Todo downloaded successfully. Use /shtodos to edit it")
                        return@argCallback
                    }
                }
                ChatUtils.userError("Todo not found")
            }
            simpleCallback {
                ChatUtils.userError("Do /shdownloadtodo <id>")
            }
        }
        event.registerBrigadier("shexportcommunitytodo") {
            category = CommandCategory.DEVELOPER_TEST
            description = "Export todos for the community repo"
            simpleCallback {
                convertTodoData()
            }
        }
    }

    private fun convertTodoData() {
        SkyHanniMod.launchIOCoroutine("export custom todo for repo") {
            val clipboard = ClipboardUtils.readFromClipboard()?.trim() ?: return@launchIOCoroutine
            val customTodo = CustomTodo.fromTemplate(clipboard) ?: return@launchIOCoroutine
            val output = """
                {
                    "name": "${customTodo.label}",
                    "id": "id",
                    "author": "author",
                    "icon": "${customTodo.icon}",
                    "discord_thread": "",
                    "todo_data": "$clipboard"
                }
            """.trimIndent()
            ClipboardUtils.copyToClipboard(output)
            ChatUtils.chat("Copied data to clipboard")
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
