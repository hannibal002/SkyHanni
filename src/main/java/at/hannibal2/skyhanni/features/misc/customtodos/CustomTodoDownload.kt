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
import at.hannibal2.skyhanni.utils.XmlUtils
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.observer.ObservableList

@SkyHanniModule
object CustomTodoDownload {

    var todos: List<CommunityTodo> = listOf()
        private set

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdownloadtodo") {
            category = CommandCategory.USERS_ACTIVE
            description = "Download community /shtodos"

            argCallback("name", BrigadierArguments.greedyString(), BrigadierUtils.dynamicSuggestionProvider { getTodoIds() }) { id ->
                if (todos.isEmpty()) {
                    ChatUtils.userError("Invalid repo data")
                    return@argCallback
                }
                for (todo in todos) {
                    if (todo.id.equals(id, ignoreCase = true)) {
                        val template = CustomTodo.fromTemplateOrNull(todo.todoData) ?: run {
                            ChatUtils.userError("Todo is invalid, please report this on discord")
                            return@argCallback
                        }
                        SkyHanniMod.customTodos.customTodos.add(
                            template.also {
                                it.downloaded = true
                                it.downloadedId = todo.id
                            }
                        )
                        CustomTodos.save()
                        ChatUtils.chat("Todo downloaded successfully. Use /shtodos to edit it")
                        return@argCallback
                    }
                }
                ChatUtils.userError("Todo not found")
            }
            simpleCallback {
                val todosList = ObservableList<CustomTodoEditor>(mutableListOf())
                SkyHanniMod.customTodos.customTodos.forEach { todosList.add(CustomTodoEditor(it, todosList)) }
                XmlUtils.openXmlScreen(
                    CommunityTodoViewer(todos, todosList),
                    MyResourceLocation("skyhanni", "gui/customtodos/communitytodos.xml"),
                )
            }
        }
    }

    private fun getTodoIds(): List<String> {
        return todos.map { it.id }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val constant = event.getConstant<CommunityTodosJson>("community/CommunityTodos")
        constant.communityTodos.forEach { CustomTodo.fromTemplate(it.todoData) }
        todos = constant.communityTodos
    }
}
